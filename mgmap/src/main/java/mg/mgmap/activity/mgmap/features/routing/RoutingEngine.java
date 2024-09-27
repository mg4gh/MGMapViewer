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
package mg.mgmap.activity.mgmap.features.routing;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.BidirectionalAStar;
import mg.mgmap.generic.graph.GGraphMulti;
import mg.mgmap.generic.graph.GGraphSearch;
import mg.mgmap.generic.graph.GGraphTile;
import mg.mgmap.generic.graph.GGraphTileFactory;
import mg.mgmap.generic.graph.GNeighbour;
import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.ExtendedPointModelImpl;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.model.PointModelUtil;

public class RoutingEngine {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final GGraphTileFactory gFactory;

    HashMap<PointModel, RoutePointModel> routePointMap = new HashMap<>(); // map from mtlp points to corresponding rpms
    HashMap<PointModel, RoutePointModel> routePointMap2 = new HashMap<>(); // map from points of routeTrackLog to corresponding rpms
    private final ArrayList<PointModel> currentRelaxedNodes = new ArrayList<>();
    private RoutingContext routingContext;
    RoutingProfile routingProfile;
    final AtomicInteger refreshRequired = new AtomicInteger(0);
    private GGraphSearch gGraphSearch = null;
    String routingAlgorithm;

    public RoutingEngine(GGraphTileFactory gFactory, RoutingContext routingContext, String routingAlgorithm){
        this.gFactory = gFactory;
        this.routingContext = routingContext;
        this.routingAlgorithm = routingAlgorithm;
    }

    public RoutingContext getRoutingContext() {
        return routingContext;
    }

    public GGraphSearch getGGraphSearch() {
        return gGraphSearch;
    }

    public void setRoutingContext(RoutingContext routingContext){
        this.routingContext = routingContext;
        for (RoutePointModel rpm : routePointMap.values()){ // invalidate Approaches
            rpm.resetApproaches();
        }
    }

    boolean setRoutingProfile(RoutingProfile routingProfile){
        if (this.routingProfile != routingProfile){
            mgLog.i("profile changed to: "+routingProfile.getId());
            this.routingProfile = routingProfile;
            synchronized (this){
                gFactory.resetCosts();
            }
            return true;
        }
        return false;
    }

    public ArrayList<GGraphTile> getGGraphTileList(BBox bBox) {
        return gFactory.getGGraphTileList(bBox);
    }

    public ApproachModel validateApproachModel(ApproachModel am) {
        return gFactory.validateApproachModel(am);
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
        calcApproaches(rpm, routingContext.approachLimit);
        if (routingContext.snap2Way){
            if (rpm.selectedApproach != null){
                if (PointModelUtil.compareTo(rpm.selectedApproach.getApproachNode() , pm) != 0){
                    if (pm instanceof WriteablePointModel) {
                        mgLog.d("");
                        WriteablePointModel wpm = (WriteablePointModel) pm;
                        wpm.setLat(rpm.selectedApproach.getApproachNode().getLat());
                        wpm.setLon(rpm.selectedApproach.getApproachNode().getLon());
                        rpm.resetApproaches();
                        calcApproaches(rpm,1);
                    }
                }
            }
        }
        return rpm;
    }

    synchronized WriteableTrackLog updateRouting2(TrackLog mtl, WriteableTrackLog rotl){
        boolean routeModified = false;
        if (routingProfile == null){
            mgLog.e("routing profile is null; cannot route");
            return rotl;
        }
        boolean routingProfileChanged = routingProfile.getId().equals(mtl.getRoutingProfileId());
        mtl.setRoutingProfileId(routingProfile.getId());

        for (TrackLogSegment segment : mtl.getTrackLogSegments()){
            if (segment.size() < 1) continue;
            Iterator<PointModel> iter = segment.iterator();
            RoutePointModel current = getVerifyRoutePointModel( iter.next() );
            current.newMPM = null;
            current.currentMPM = new MultiPointModelImpl().addPoint(new PointModelImpl(current.mtlp));
            while (iter.hasNext()){
                RoutePointModel prev = current;
                current = getVerifyRoutePointModel( iter.next() );

                boolean bRecalcRoute = true;
                try {
                    if (routingProfileChanged){
                        if ((prev.getApproach() != null) && (current.getApproach() != null)) {
                            PointModel pmFirst = current.currentMPM.get(0);
                            PointModel pmLast = current.currentMPM.get(current.currentMPM.size() - 1);
                            PointModel approachFirst = prev.getApproach().getApproachNode();
                            PointModel approachLast = current.getApproach().getApproachNode();
                            if ((PointModelUtil.compareTo(pmFirst, approachFirst) == 0) &&
                                    (PointModelUtil.compareTo(pmLast, approachLast) == 0) &&
                                    !current.aborted)
                                bRecalcRoute = false;
                        }
                    }

                } catch (Exception e){
                    //routeModel can be null, approachSet can be empty, ...
                }

                if (bRecalcRoute){
                    routeModified = true;
                    current.aborted = false;
                    current.newMPM = calcRouting(prev, current, currentRelaxedNodes);
                }
                if (refreshRequired.get() != 0) {
                    current.aborted = true;
                    break;
                }
            }
            if (refreshRequired.get() != 0) {
                routeModified = true;
                break;
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
            routeTrackLog.setModified(mtl.isModified());
            routeTrackLog.setReferencedTrackLog(mtl);
            mgLog.i("Route modified: "+name);

            ArrayList<PointModel> rpmKeys = new ArrayList<>(routePointMap.keySet()); // create a list to detect unused entries in routePointMap
            for (TrackLogSegment segment : mtl.getTrackLogSegments()){
                segment.removeSegmentPointsFrom(rpmKeys); // remove used entries
                long timestamp = 0;
                routeTrackLog.startSegment(timestamp);
                PointModel lastPM = null;
                for (int idx=1; idx<segment.size(); idx++){ // skip first point of segment, since it doesn't contain route information
                    RoutePointModel rpm = routePointMap.get(segment.get(idx));
                    if (rpm != null){
                        rpm.currentMPM = rpm.newMPM;
                        if (rpm.newMPM != null){
                            rpm.currentDistance = PointModelUtil.distance(rpm.currentMPM);
                            long mpmDuration = 0;
                            for (PointModel pm : rpm.newMPM){
                                if ((lastPM == null) || (pm.getLaLo() != lastPM.getLaLo())) { // don't add, if the same point already exists (connecting point of two routes should belong to the first one)
                                    RoutingHint hint = null;
                                    if (pm instanceof ExtendedPointModelImpl<?>) {
                                        //noinspection unchecked
                                        ExtendedPointModelImpl<RoutingHint> epm = (ExtendedPointModelImpl<RoutingHint>) pm;
                                        hint = epm.getExtent();
                                        mpmDuration = epm.getTimestamp();
                                    }
                                    ExtendedPointModelImpl<RoutingHint> epmr = new ExtendedPointModelImpl<>(pm, hint);
                                    epmr.setTimestamp(timestamp + mpmDuration);
                                    routeTrackLog.addPoint(epmr);
                                    routePointMap2.put(epmr, rpm);


                                }
                                lastPM = pm;

                            }
                            timestamp += mpmDuration;
                        }
                    }
                }
                if (segment.getStatistic().getNumPoints() == 1){ // indicates that no rout was calculated
                    routeTrackLog.addPoint(new PointModelImpl(segment.get(0)));
                }
                routeTrackLog.stopSegment(timestamp);
            }
            routeTrackLog.stopTrack(0);

            // remaining entries are unused - remove them from routePointMap
            for (PointModel unusedMtlp : rpmKeys){
                routePointMap.remove(unusedMtlp);
            }
        } else {
            mgLog.d("route unchanged");
        }
        return routeTrackLog;
    }

    MultiPointModelImpl calcRouting(RoutePointModel source, RoutePointModel target) {
        return calcRouting(source,target,null);
    }

    MultiPointModelImpl calcRouting(RoutePointModel source, RoutePointModel target, ArrayList<PointModel> relaxedNodes){

        MultiPointModelImpl mpm = new MultiPointModelImpl();
        mgLog.d("Start");

        GNode gStart = source.getApproachNode();
        GNode gEnd = target.getApproachNode();
        mgLog.i("start "+gStart+" end "+gEnd);

        GGraphMulti multi = null;
        ApproachModel sourceApproachModel = null;
        ApproachModel targetApproachModel = null;
        try {
            if ((gStart != null) && (gEnd != null)){
                ArrayList<GGraphTile> gGraphTiles = getGGraphTileList (new BBox().extend(gStart).extend(PointModelUtil.getCloseThreshold()));
                gGraphTiles.addAll(getGGraphTileList(new BBox().extend(gEnd).extend(PointModelUtil.getCloseThreshold())));
                multi = new GGraphMulti(gFactory, gGraphTiles);

                sourceApproachModel = validateApproachModel(source.selectedApproach);
                targetApproachModel = validateApproachModel(target.selectedApproach);
                connectApproach2Graph(sourceApproachModel);
                connectApproach2Graph(targetApproachModel);

                double distLimit = Math.min(routingContext.maxBeelineDistance, routingContext.maxRouteLengthFactor * routingProfile.heuristic(gStart, gEnd) + 500);

                // perform an AStar on this graph - ProfiledAStar may adopt the heuristic calculation depending on the current routingProfile
                try {
                    Class<?> clazz = Class.forName("mg.mgmap.generic.graph."+routingAlgorithm);
                    Constructor<?> constructor = clazz.getConstructor(GGraphMulti.class, RoutingProfile.class);
                    Object object = constructor.newInstance(multi, routingProfile);
                    if (object instanceof GGraphSearch) {
                        gGraphSearch = (GGraphSearch) object;
                    }
                } catch (Exception e) {
                    mgLog.e(e);
                    gGraphSearch = new BidirectionalAStar(multi, routingProfile); // fallback
                }
                MultiPointModel mpmRaw = gGraphSearch.perform(gStart, gEnd, distLimit, refreshRequired, relaxedNodes);
                mgLog.i(gGraphSearch.getResult());
                gGraphSearch = null;

                long duration = -1;
                for (int idx=0; idx < mpmRaw.size(); idx++){
                    RoutingHint hint = null;
                    if (idx == 0){
                        duration = 0;
                    } else {
                        GNeighbour lastNeighbour = null;
                        if ((mpmRaw.get(idx-1) instanceof  GNode) && (mpmRaw.get(idx) instanceof GNode)){
                            lastNeighbour = ((GNode)mpmRaw.get(idx-1)).getNeighbour((GNode)mpmRaw.get(idx));
                        }
                        duration += routingProfile.getDuration(mpmRaw.get(idx-1), lastNeighbour, mpmRaw.get(idx));

                        if (idx < mpmRaw.size()-1){
                            hint = new RoutingHint();
                            hint.pmPrev = mpmRaw.get(idx-1);
                            hint.pmCurrent = mpmRaw.get(idx);
                            hint.pmNext = mpmRaw.get(idx+1);

                            hint.directionDegree = PointModelUtil.calcDegree(hint.pmPrev,hint.pmCurrent,hint.pmNext);

                            hint.nextLeftDegree = -1;
                            hint.nextRightDegree = 361;
                            if (hint.pmCurrent instanceof GNode) {
                                GNode pmgCurrent = (GNode) hint.pmCurrent;
                                GNeighbour neighbour = pmgCurrent.getNeighbour();

                                while ((neighbour = multi.getNextNeighbour(pmgCurrent, neighbour)) != null) {
                                    if (sourceApproachModel.getApproachNode() == neighbour.getNeighbourNode()) continue; // skip neighbour to source approach node
                                    if (targetApproachModel.getApproachNode() == neighbour.getNeighbourNode()) continue; // skip neighbour to target approach node
                                    hint.numberOfPathes++;
                                    // use approach segments as path, but not as concurrent path with degree calculation
                                    if ((idx == 1) && (source.verifyApproach(hint.pmCurrent,hint.pmPrev,neighbour.getNeighbourNode()))){
                                        continue;
                                    }
                                    if ((idx == mpmRaw.size()-2) && (target.verifyApproach(hint.pmCurrent,hint.pmNext,neighbour.getNeighbourNode()))){
                                        continue;
                                    }

                                    double degree = PointModelUtil.calcDegree(hint.pmPrev,hint.pmCurrent,neighbour.getNeighbourNode());
                                    if ((hint.nextLeftDegree < degree) && (degree < hint.directionDegree)) hint.nextLeftDegree = degree;
                                    if ((hint.directionDegree < degree) && (degree < hint.nextRightDegree)) hint.nextRightDegree = degree;
                                }
                            }

                        }
                    }
                    ExtendedPointModelImpl<RoutingHint> epm = new ExtendedPointModelImpl<>(mpmRaw.get(idx),hint);
                    epm.setTimestamp(duration);
                    mpm.addPoint(epm);
                }


                // optimize, if start and end approach hit the same graph neighbour (overlays for approach doesn't consider potential neighbour approach
                if (mpm.size() == 3){
                    double d = PointModelUtil.distance(mpm.get(0), mpm.get(2) );
                    double d1 = PointModelUtil.distance(mpm.get(0), mpm.get(1) );
                    double d2 = PointModelUtil.distance(mpm.get(1), mpm.get(2) );
                    PointModel pm1 = mpm.get(1);

                    if (((d1 > d2) && (Math.abs(d1-(d2+d))<0.1)) || ((d1 <= d2) && (Math.abs(d2-(d1+d))<0.1))){
                        // lastNeighbour will only be used for duration calculation - only distance and wayAttributes are used - so incorrect neighbour object doesn't matter
                        GNeighbour lastNeighbour = ((GNode)mpmRaw.get(0)).getNeighbour((GNode)mpmRaw.get(1));
                        lastNeighbour.setDistance(PointModelUtil.distance(mpm.get(0),mpm.get(2)));
                        duration = routingProfile.getDuration(mpm.get(0), lastNeighbour, mpm.get(2));
                        if (mpm.get(2) instanceof ExtendedPointModelImpl<?>){
                            ((ExtendedPointModelImpl<?>)mpm.get(2)).setTimestamp(duration);
                        }
                        mpm.removePoint(pm1);
                    }
                }
            }
        } catch (Exception e) {
            mgLog.e(e);
        } finally {
            if (multi != null) {
                multi.finalizeUsage();
            }
            gFactory.serviceCache();
            if (sourceApproachModel != null){
                if (sourceApproachModel.getNode1() != null){
                    sourceApproachModel.getNode1().removeNeighbourNode(0);
                }
                if (sourceApproachModel.getNode2() != null){
                    sourceApproachModel.getNode2().removeNeighbourNode(0);
                }
                gStart.getNeighbour().setNextNeighbour(null);
            }
            if (targetApproachModel != null){
                if (targetApproachModel.getNode1() != null){
                    targetApproachModel.getNode1().removeNeighbourNode(0);
                }
                if (targetApproachModel.getNode2() != null){
                    targetApproachModel.getNode2().removeNeighbourNode(0);
                }
                gEnd.getNeighbour().setNextNeighbour(null);
            }
        }
        mpm.setRoute(mpm.size() != 0);

        if (mpm.size() == 0) { // no routing required or routing not possible due to missing approach or no route found
            ExtendedPointModelImpl<RoutingHint> epm0 = new ExtendedPointModelImpl<>((gStart==null)?source.mtlp:gStart, null);
            mpm.addPoint(epm0);

            ExtendedPointModelImpl<RoutingHint> epm1 = new ExtendedPointModelImpl<>((gEnd==null)?target.mtlp:gEnd, null);
            epm1.setTimestamp(routingProfile.getDuration(epm0, null, epm1));
            mpm.addPoint(epm1);
        }
        mgLog.d("End");
        return mpm;
    }

    void calcApproaches(RoutePointModel rpm, int closeThreshold) {
        if (rpm.getApproach() != null){
            return; // approach calculation is already done
        }
        PointModel pointModel = rpm.mtlp;
        rpm.setApproaches(calcApproaches(pointModel, closeThreshold));
    }

    TreeSet<ApproachModel> calcApproaches(PointModel pointModel, int closeThreshold){
        BBox mtlpBBox = new BBox()
                .extend(pointModel)
                .extend(closeThreshold);

        TreeSet<ApproachModel> approaches = new TreeSet<>();
        WriteablePointModel pmApproach = new TrackLogPoint();

        ArrayList<GGraphTile> tiles = getGGraphTileList(mtlpBBox);
        for (GGraphTile gGraphTile : tiles){
            for (GNode node : gGraphTile.getNodes()) {

                GNeighbour neighbour = node.getNeighbour();
                while ((neighbour = gGraphTile.getNextNeighbour(node, neighbour)) != null) {
                    GNode neighbourNode = neighbour.getNeighbourNode();
                    if (GNode.sameTile(node, neighbourNode) && (PointModelUtil.compareTo(node, neighbourNode) < 0)){ // neighbour relations exist in both direction - here we can reduce to one
                        BBox bBoxPart = new BBox().extend(node).extend(neighbourNode);
                        boolean bIntersects = mtlpBBox.intersects(bBoxPart);
                        if (bIntersects){ // ok, is candidate for close
                            if (PointModelUtil.findApproach(pointModel, node, neighbourNode, pmApproach , closeThreshold)) {
                                double distance = PointModelUtil.distance(pointModel, pmApproach)+0.0001;
                                if (distance < closeThreshold){ // ok, is close ==> new Approach found
                                    GNode approachNode = new GNode(pmApproach.getLat(), pmApproach.getLon(), pmApproach.getEle(), pmApproach.getEleAcc(), distance); // so we get a new node for the approach, since pmApproach will be overwritten in next cycle
                                    ApproachModel approach = new ApproachModel(gGraphTile.getTileX(),gGraphTile.getTileY() ,pointModel, node, neighbourNode, approachNode);
                                    approaches.add(approach);
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
                GGraphTile gGraphTile = approach.getNode1().getTile(gFactory);
                ArrayList<GNode> segmentNodes = gGraphTile.segmentNodes(approach.getNode1(), approach.getNode2(), closeThreshold);
                for (int idx=appIdx+1; idx<listApproaches.size(); idx++){
                    ApproachModel other = listApproaches.get(idx);
                    if (segmentNodes.contains(other.getNode1()) && segmentNodes.contains(other.getNode2())){
                        dropApproaches.add(other);
                    }
                }
            }
        }
        dropApproaches.forEach(approaches::remove); // seems to have better performance
        return approaches;
    }

    private void connectApproach2Graph(ApproachModel approach){
        GNode approachNode = approach.getApproachNode();
        GNode node1 = approach.getNode1();
        GNode node2 = approach.getNode2();
        GNeighbour neighbour12 = node1.getNeighbour(node2);
        GNeighbour neighbour21 = neighbour12.getReverse();
        node1.bidirectionalConnect(approachNode, neighbour12);
        node2.bidirectionalConnect(approachNode, neighbour21);
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
