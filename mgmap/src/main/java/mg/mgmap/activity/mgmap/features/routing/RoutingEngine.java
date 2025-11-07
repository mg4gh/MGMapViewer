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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.graph.GraphAlgorithm;
import mg.mgmap.generic.graph.GraphFactory;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.ExtendedPointModelImpl;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.util.Observable;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.model.PointModelUtil;

public class RoutingEngine {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final GraphFactory gFactory;

    final HashMap<PointModel, RoutePointModel> routePointMap = new HashMap<>(); // map from mtlp points to corresponding rpms
    final HashMap<PointModel, RoutePointModel> routePointMap2 = new HashMap<>(); // map from points of routeTrackLog to corresponding rpms
    private final ArrayList<PointModel> currentRelaxedNodes = new ArrayList<>();
    private RoutingContext routingContext;
    RoutingProfile routingProfile;
    final Object routingProfileMonitor = new Object();
    final AtomicInteger refreshRequired = new AtomicInteger(0);
    private final Observable routeIntermediatesObservable;

    public RoutingEngine(GraphFactory gFactory, RoutingContext routingContext, Observable routeIntermediatesObservable){
        this.gFactory = gFactory;
        this.routingContext = routingContext;
        this.routeIntermediatesObservable = routeIntermediatesObservable;
    }

    public RoutingContext getRoutingContext() {
        return routingContext;
    }

    public void setRoutingContext(RoutingContext routingContext){
        this.routingContext = routingContext;
        for (RoutePointModel rpm : routePointMap.values()){ // invalidate Approaches
            rpm.resetApproaches();
        }
    }

    void setRoutingProfile(RoutingProfile routingProfile){
        synchronized (routingProfileMonitor){
            if (this.routingProfile != routingProfile){
                mgLog.i("profile changed to: "+routingProfile.getId());
                this.routingProfile = routingProfile;
                synchronized (this){ // wait for updateRouting2 to be finished (if running)
                    gFactory.resetCosts();
                }
                refreshRequired.incrementAndGet();
            }
        }
    }

    void checkRoutingProfileMonitor() { // this method blocks while a routing profile change is in progress - this may take some time, since profile change may need to wait for a running route action
        synchronized (routingProfileMonitor) {
            mgLog.d("got routingProfileMonitor");
        }
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
                    if (pm instanceof WriteablePointModel wpm) {
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
                } catch (Exception e){
                    //routeModel can be null, approach can be null, ...
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
            routeTrackLog.setRoutingProfileId(mtl.getRoutingProfileId());
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

        PointModel gStart = source.getApproachNode();
        PointModel gEnd = target.getApproachNode();
        mgLog.i("start "+gStart+" end "+gEnd);

        Graph graph = null;
        ApproachModel sourceApproachModel = null;
        ApproachModel targetApproachModel = null;
        try {
            if ((gStart != null) && (gEnd != null)){
                graph = gFactory.getGraph(gStart, gEnd);
                sourceApproachModel = gFactory.validateApproachModel(source.selectedApproach);
                targetApproachModel = gFactory.validateApproachModel(target.selectedApproach);
                gFactory.connectApproach2Graph(graph, sourceApproachModel);
                gFactory.connectApproach2Graph(graph, targetApproachModel);
                gFactory.checkDirectConnectApproaches(graph, sourceApproachModel, targetApproachModel);

                double distLimit = Math.min(routingContext.maxBeelineDistance, routingContext.maxRouteLengthFactor * routingProfile.heuristic(gStart, gEnd) + 500);

                GraphAlgorithm graphAlgorithm = gFactory.getAlgorithmForGraph(graph, routingProfile);
                graphAlgorithm.setRouteIntermediatesObservable(routeIntermediatesObservable);
                MultiPointModel mpmRaw = graphAlgorithm.perform(sourceApproachModel, targetApproachModel, distLimit, refreshRequired, relaxedNodes);
                mgLog.i(graphAlgorithm.getResult());

                long duration = -1;
                for (int idx=0; idx < ((mpmRaw==null)?0:mpmRaw.size()); idx++){
                    RoutingHint hint = null;
                    if (idx == 0){
                        duration = 0;
                    } else {
                        WayAttributs wayAttributs = null;
                        if (mpmRaw.get(idx) instanceof ExtendedPointModelImpl<?>){
                            //noinspection unchecked
                            wayAttributs =((ExtendedPointModelImpl<WayAttributs>)mpmRaw.get(idx)).getExtent();
                        }
                        duration += routingProfile.getDuration(wayAttributs,mpmRaw.get(idx-1), mpmRaw.get(idx));

                        if (idx < mpmRaw.size()-1){
                            hint = new RoutingHint();
                            hint.pmPrev = mpmRaw.get(idx-1);
                            hint.pmCurrent = mpmRaw.get(idx);
                            hint.pmNext = mpmRaw.get(idx+1);

                            hint.directionDegree = PointModelUtil.calcDegree(hint.pmPrev,hint.pmCurrent,hint.pmNext);

                            hint.nextLeftDegree = -1;
                            hint.nextRightDegree = 361;

                            for (PointModel neighbourPoint : graph.getNeighbours(hint.pmCurrent, new ArrayList<>())){
                                if (sourceApproachModel.getApproachNode() == neighbourPoint) continue; // skip neighbour to source approach node
                                if (targetApproachModel.getApproachNode() == neighbourPoint) continue; // skip neighbour to target approach node
                                hint.numberOfPathes++;
                                // use approach segments as path, but not as concurrent path with degree calculation
                                if ((idx == 1) && (source.verifyApproach(hint.pmCurrent,hint.pmPrev,neighbourPoint))){
                                    continue;
                                }
                                if ((idx == mpmRaw.size()-2) && (target.verifyApproach(hint.pmCurrent,hint.pmNext,neighbourPoint))){
                                    continue;
                                }

                                double degree = PointModelUtil.calcDegree(hint.pmPrev,hint.pmCurrent,neighbourPoint);
                                if ((hint.nextLeftDegree < degree) && (degree < hint.directionDegree)) hint.nextLeftDegree = degree;
                                if ((hint.directionDegree < degree) && (degree < hint.nextRightDegree)) hint.nextRightDegree = degree;
                            }
                        }
                    }
                    ExtendedPointModelImpl<RoutingHint> epm = new ExtendedPointModelImpl<>(mpmRaw.get(idx),hint);
                    epm.setTimestamp(duration);
                    mpm.addPoint(epm);
                }
            } else {
                mgLog.d("no approach, take bee line");
            }
        } catch (Exception e) {
            mgLog.e(e);
        } finally {
            if (graph != null) {
                graph.finalizeUsage();
            }
            gFactory.serviceCache();
            gFactory.disconnectApproach2Graph(graph, sourceApproachModel);
            gFactory.disconnectApproach2Graph(graph, targetApproachModel);
        }
        mpm.setRoute(mpm.size() != 0);

        if (mpm.size() == 0) { // no routing required or routing not possible due to missing approach or no route found
            ExtendedPointModelImpl<RoutingHint> epm0 = new ExtendedPointModelImpl<>((gStart==null)?source.mtlp:gStart, null);
            mpm.addPoint(epm0);

            ExtendedPointModelImpl<RoutingHint> epm1 = new ExtendedPointModelImpl<>((gEnd==null)?target.mtlp:gEnd, null);
            epm1.setTimestamp(routingProfile.getDuration(null, epm0, epm1));
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
        rpm.setApproach(gFactory.calcApproach(pointModel, closeThreshold));
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
