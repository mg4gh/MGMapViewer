/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.features.routing;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import mg.mgmap.MGMapApplication;
import mg.mgmap.graph.AStar;
import mg.mgmap.graph.ApproachModel;
import mg.mgmap.graph.GGraphMulti;
import mg.mgmap.graph.GGraphTile;
import mg.mgmap.graph.GGraphTileFactory;
import mg.mgmap.graph.GNeighbour;
import mg.mgmap.graph.GNode;
import mg.mgmap.graph.GNodeRef;
import mg.mgmap.model.BBox;
import mg.mgmap.model.ExtendedPointModelImpl;
import mg.mgmap.model.MultiPointModelImpl;
import mg.mgmap.model.PointModel;
import mg.mgmap.model.PointModelImpl;
import mg.mgmap.model.TrackLog;
import mg.mgmap.model.TrackLogPoint;
import mg.mgmap.model.TrackLogSegment;
import mg.mgmap.model.WriteablePointModel;
import mg.mgmap.model.WriteableTrackLog;
import mg.mgmap.util.AltitudeProvider;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.PointModelUtil;
import mg.mgmap.util.Pref;

public class RoutingEngine {

    private static final int MAX_ROUTE_DISTANCE = 10000; // maximum allowed route length - otherwise AStar is expected be bee too slow

    private final GGraphTileFactory gFactory;
    private final Pref<Boolean> prefSnap2Way;

    HashMap<PointModel, RoutePointModel> routePointMap = new HashMap<>(); // map from mtlp points to corresponding rpms
    HashMap<PointModel, RoutePointModel> routePointMap2 = new HashMap<>(); // map from points of routeTrackLog to corresponding rpms
    private final ArrayList<PointModel> currentRelaxedNodes = new ArrayList<>();

    public RoutingEngine(GGraphTileFactory gFactory, Pref<Boolean> prefSnap2Way){
        this.gFactory = gFactory;
        this.prefSnap2Way = prefSnap2Way;
    }

    RoutePointModel getRoutePointModel(PointModel pm){
        RoutePointModel rpm = routePointMap.get(pm);
        if (rpm == null){
            rpm = new RoutePointModel(pm);
            routePointMap.put(pm, rpm);
        } else {
            ApproachModel am = rpm.getApproach();
            if (am != null){
                if (PointModelUtil.compareTo(am.getPmPos(), pm) != 0){
                    rpm.resetApproaches();
                }
            }
        }
        return rpm;
    }

    RoutePointModel getVerifyRoutePointModel(PointModel pm){
        RoutePointModel rpm = getRoutePointModel(pm);
        calcApproaches(rpm);
        if (prefSnap2Way.getValue()){
            if (rpm.selectedApproach != null){
                if (PointModelUtil.compareTo(rpm.selectedApproach.getApproachNode() , pm) != 0){
                    if (pm instanceof WriteablePointModel) {
                        Log.d(MGMapApplication.LABEL, NameUtil.context());
                        WriteablePointModel wpm = (WriteablePointModel) pm;
                        wpm.setLat(rpm.selectedApproach.getApproachNode().getLat());
                        wpm.setLon(rpm.selectedApproach.getApproachNode().getLon());
                        rpm.resetApproaches();
                        calcApproaches(rpm);
                    }
                }
            }
        }
        return rpm;
    }

    WriteableTrackLog updateRouting2(TrackLog mtl, WriteableTrackLog rotl){
        boolean routeModified = false;

        for (TrackLogSegment segment : mtl.getTrackLogSegments()){
            if (segment.size() < 1) continue;
            Iterator<PointModel> iter = segment.iterator();
            RoutePointModel current = getVerifyRoutePointModel( iter.next() );
            current.newMPM = null;
            while (iter.hasNext()){
                RoutePointModel prev = current;
                current = getVerifyRoutePointModel( iter.next() );

                boolean bRecalcRoute = true;
                try {
                    if ((prev.getApproach() != null) && (current.getApproach() != null)) {
                        PointModel pmFirst = current.currentMPM.get(0);
                        PointModel pmLast = current.currentMPM.get(current.currentMPM.size() - 1);
                        PointModel approachFirst = prev.getApproach().getApproachNode();
                        PointModel approachLast = current.getApproach().getApproachNode();
                        if ((PointModelUtil.compareTo(pmFirst, approachFirst) == 0) &&
                                (PointModelUtil.compareTo(pmLast, approachLast) == 0))
                            bRecalcRoute = false;
                    }
                    if (current.directChanged) bRecalcRoute = true;

                } catch (Exception e){
                    //routeModel can be null, approachSet can be empty, ...
                }

                if (bRecalcRoute){
                    routeModified = true;
                    current.newMPM = calcRouting(prev, current, current.direct, current.routingHints, currentRelaxedNodes);
                }
            }
        }

        if (!routeModified){
            if ((rotl != null) && (mtl.getNumberOfSegments() == rotl.getNumberOfSegments())) {
                for (int i=0; i<mtl.getNumberOfSegments(); i++) {
                    TrackLogSegment mtlSegment = mtl.getTrackLogSegment(i);
                    TrackLogSegment rotlSegment = rotl.getTrackLogSegment(i);
                    if ((mtlSegment.size() > 0) && (rotlSegment.size() > 0)){
                        if (PointModelUtil.compareTo( getRoutePointModel(mtlSegment.get(0)).getApproachNode(), rotlSegment.get(0) ) != 0){
                            routeModified = true;
                            break;
                        }
                        if (PointModelUtil.compareTo( getRoutePointModel(mtlSegment.get(mtlSegment.size()-1)).getApproachNode(), rotlSegment.get(rotlSegment.size()-1) ) != 0){
                            routeModified = true;
                            break;
                        }
                    }
                }
            } else {
                routeModified = true;
            }
        }

        WriteableTrackLog routeTrackLog = rotl;  // default is: route not modified, return the old one.
        if (routeModified){
            // do three things:
            // 1.) move newMPM to currentMPM information
            // 2.) recalc route statistic
            // 3.) setup routingMPMs
            routePointMap2.clear();
            routeTrackLog = new WriteableTrackLog();
            String name = mtl.getName();
            name = name.replaceAll("MarkerTrack$","MarkerRoute");
            routeTrackLog.setName(name);
            routeTrackLog.startTrack(mtl.getTrackStatistic().getTStart());
            routeTrackLog.setModified(true);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " Route modified: "+name);

            for (TrackLogSegment segment : mtl.getTrackLogSegments()){
                routeTrackLog.startSegment(0);
                PointModel lastPM = null;
                for (int idx=1; idx<segment.size(); idx++){ // skip first point of segment, since it doesn't contain route information
                    RoutePointModel rpm = routePointMap.get(segment.get(idx));
                    if (rpm != null){
                        rpm.currentMPM = rpm.newMPM;
                        rpm.directChanged = false;
                        rpm.currentDistance = PointModelUtil.distance(rpm.currentMPM);
                        if (rpm.newMPM != null){
                            for (PointModel pm : rpm.newMPM){
                                if (pm != lastPM){ // don't add, if the same point already exists (connecting point of two routes should belong to the first one)
                                    ExtendedPointModelImpl<RoutingHint> pmr = new ExtendedPointModelImpl<>(pm,rpm.routingHints.get(pm));
                                    routeTrackLog.addPoint(pmr);
                                    routePointMap2.put(pmr,rpm);
                                }
                                lastPM = pm;
                            }
                        }
                    }
                }
                routeTrackLog.stopSegment(0);
            }
            routeTrackLog.stopTrack(0);

        }
        return routeTrackLog;
    }

    MultiPointModelImpl calcRouting(RoutePointModel source, RoutePointModel target) {
        return calcRouting(source,target,false,null,null);
    }

    MultiPointModelImpl calcRouting(RoutePointModel source, RoutePointModel target, boolean direct, Map<PointModel, RoutingHint> hints, ArrayList<PointModel> relaxedNodes){

        MultiPointModelImpl mpm = new MultiPointModelImpl();
        Log.d(MGMapApplication.LABEL, NameUtil.context()+" Start");

        GNode gStart = source.getApproachNode();
        GNode gEnd = target.getApproachNode();
        Log.i(MGMapApplication.LABEL, NameUtil.context()+ " start "+gStart+" end "+gEnd);

        double distLimit = acceptedRouteDistance(source.mtlp, target.mtlp);
        GGraphMulti multi = null;

        try {
            if ((gStart != null) && (gEnd != null) && (distLimit > 0) && !direct){
                BBox bBox = new BBox().extend(source.mtlp).extend(target.mtlp);
                bBox.extend( Math.max(PointModelUtil.getCloseThreshold(), PointModelUtil.distance(source.mtlp,target.mtlp)*0.7 + 2*PointModelUtil.getCloseThreshold() ) );
//                GGraphTileFactory gFactory = getActivity().getGGraphTileFactory();
                ArrayList<GGraphTile> gGraphTileList = gFactory.getGGraphTileList(bBox);
                multi = new GGraphMulti(gGraphTileList);
                multi.createOverlaysForApproach( gFactory.validateApproachModel(source.selectedApproach) );
                multi.createOverlaysForApproach( gFactory.validateApproachModel(target.selectedApproach) );

                // perform an AStar on this graph
                AStar aStar = new AStar(multi);
                for (GNodeRef gnr : aStar.perform(gStart, gEnd, distLimit, relaxedNodes)){
                    mpm.addPoint(gnr.getNode() );
                }
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+aStar.getResult());

                // optimize, if start and end approach hit the same graph neighbour (overlays for approach doesn't consider potential neighbour approach
                if (mpm.size() == 3){
                    double d = PointModelUtil.distance(mpm.get(0), mpm.get(2) );
                    double d1 = PointModelUtil.distance(mpm.get(0), mpm.get(1) );
                    double d2 = PointModelUtil.distance(mpm.get(1), mpm.get(2) );
                    PointModel pm1 = mpm.get(1);
                    if (d1 > d2){
                        if (Math.abs(d1-(d2+d))<0.1){
                            mpm.removePoint(pm1);
                        }
                    } else {
                        if (Math.abs(d2-(d1+d))<0.1){
                            mpm.removePoint(pm1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
        mpm.setRoute(mpm.size() != 0);

        if (mpm.size() == 0) { // no routing required or routing not possible due to missing approach or no route found
            if (gStart != null){
                mpm.addPoint(gStart);
            } else {
                mpm.addPoint(new PointModelImpl(source.mtlp));
            }
            if (gEnd != null){
                mpm.addPoint(gEnd);
            } else {
                mpm.addPoint(new PointModelImpl(target.mtlp));
            }
        }

        if (hints != null){
            hints.clear();
            for (int idx=1; idx < mpm.size()-1; idx++){
                RoutingHint hint = new RoutingHint();
                hint.pmPrev = mpm.get(idx-1);
                hint.pmCurrent = mpm.get(idx);
                hint.pmNext = mpm.get(idx+1);

                hint.directionDegree = PointModelUtil.calcDegree(hint.pmPrev,hint.pmCurrent,hint.pmNext);

                hint.nextLeftDegree = -1;
                hint.nextRightDegree = 361;
                if (hint.pmCurrent instanceof GNode) {
                    GNode pmgCurrent = (GNode) hint.pmCurrent;
                    GNeighbour neighbour = pmgCurrent.getNeighbour();

                    while ((neighbour = multi.getNextNeighbour(pmgCurrent, neighbour)) != null) {
                        hint.numberOfPathes++;
                        if (neighbour.getNeighbourNode().equals(hint.pmPrev)) continue;
                        if (neighbour.getNeighbourNode().equals(hint.pmNext)) continue;
                        if ((idx == 1) && (multi.oppositeNode((GNode)hint.pmPrev,(GNode)hint.pmCurrent) == neighbour.getNeighbourNode())){
                            hint.numberOfPathes--; // don't count, if add. neighbour is just related to approach overlays
                            continue;
                        }
                        if ((idx == mpm.size()-2) && (multi.oppositeNode((GNode)hint.pmNext,(GNode)hint.pmCurrent) == neighbour.getNeighbourNode())){
                            hint.numberOfPathes--; // don't count, if add. neighbour is just related to approach overlays
                            continue;
                        }

                        double degree = PointModelUtil.calcDegree(hint.pmPrev,hint.pmCurrent,neighbour.getNeighbourNode());
                        if ((hint.nextLeftDegree < degree) && (degree < hint.directionDegree)) hint.nextLeftDegree = degree;
                        if ((hint.directionDegree < degree) && (degree < hint.nextRightDegree)) hint.nextRightDegree = degree;
                    }
                }
                hints.put(hint.pmCurrent, hint);
            }
        }
        Log.d(MGMapApplication.LABEL, NameUtil.context()+" End");
        return mpm;
    }


    void calcApproaches(RoutePointModel rpm){

        if (rpm.getApproach() != null){
            return; // approach calculation is already done
        }

        PointModel pointModel = rpm.mtlp;

        int closeThreshold = PointModelUtil.getCloseThreshold();
        BBox mtlpBBox = new BBox()
                .extend(pointModel)
                .extend(closeThreshold);

        TreeSet<ApproachModel> approaches = new TreeSet<>();
        WriteablePointModel pmApproach = new TrackLogPoint();

        ArrayList<GGraphTile> tiles = gFactory.getGGraphTileList(mtlpBBox);
        GGraphMulti multi = new GGraphMulti(tiles);
        for (GGraphTile gGraphTile : tiles){
            for (GNode node : gGraphTile.getNodes()) {

                GNeighbour neighbour = node.getNeighbour();
                while ((neighbour = gGraphTile.getNextNeighbour(node, neighbour)) != null) {
                    GNode neighbourNode = neighbour.getNeighbourNode();
                    if (PointModelUtil.compareTo(node, neighbourNode) < 0){ // neighbour relations exist in both direction - here we can reduce to one
                        BBox bBoxPart = new BBox().extend(node).extend(neighbourNode);
                        boolean bIntersects = mtlpBBox.intersects(bBoxPart);
                        if (bIntersects){ // ok, is candidate for close
                            if (PointModelUtil.findApproach(pointModel, node, neighbourNode, pmApproach)) {
                                double distance = PointModelUtil.distance(pointModel, pmApproach);
                                if (distance < closeThreshold){ // ok, is close ==> new Approach found
                                    float hgtAlt = AltitudeProvider.getAlt(pmApproach);
                                    GNode approachNode = new GNode(pmApproach.getLat(), pmApproach.getLon(), hgtAlt, distance); // so we get a new node for the approach, since pmApproach will be overwritten in next cycle
                                    ApproachModel approach = new ApproachModel(gGraphTile.getTileX(),gGraphTile.getTileY() ,pointModel, node, neighbour.getNeighbourNode(), approachNode);
                                    approaches.add(approach);
//                                    gGraphTile.addObserver(rpm);
                                }
                            }
                        }
                    }
                }
            }
        }

        ArrayList<ApproachModel> dropApproaches = new ArrayList<>(); // collect approaches that should be deleted
        ArrayList<ApproachModel> listApproaches = new ArrayList<>(approaches); // approaches as a list (not as a treeSet)
        for (int appIdx=0; appIdx<listApproaches.size(); appIdx++){
            ApproachModel approach = listApproaches.get(appIdx);
            if (!dropApproaches.contains( approach )){
                ArrayList<GNode> segmentNodes = multi.segmentNodes(approach.getNode1(), approach.getNode2(), closeThreshold);
                for (int idx=appIdx+1; idx<listApproaches.size(); idx++){
                    ApproachModel other = listApproaches.get(idx);
                    if (segmentNodes.contains(other.getNode1()) && segmentNodes.contains(other.getNode2())){
                        dropApproaches.add(other);
                    }
                }
            }
        }
        approaches.removeAll(dropApproaches);
        rpm.setApproaches(approaches);
    }

    private double acceptedRouteDistance(PointModel pmStart, PointModel pmEnd){
        double distance = PointModelUtil.distance(pmStart,pmEnd);
        double res = 0;
        if (distance < MAX_ROUTE_DISTANCE){ // otherwise it will take too long
            res = Math.min (3 * PointModelUtil.distance(pmStart,pmEnd) + 2 * PointModelUtil.getCloseThreshold(), MAX_ROUTE_DISTANCE);
        }
        return res;
    }

    public HashMap<PointModel, RoutePointModel> getRoutePointMap() {
        return routePointMap;
    }

    public HashMap<PointModel, RoutePointModel> getRoutePointMap2() {
        return routePointMap2;
    }

    public ArrayList<PointModel> getCurrentRelaxedNodes() {
        return currentRelaxedNodes;
    }
}
