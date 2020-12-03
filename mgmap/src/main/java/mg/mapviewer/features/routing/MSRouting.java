/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mapviewer.features.routing;


import android.util.Log;
import android.view.ViewGroup;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.datastore.MapDataStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.features.marker.MSMarker;
import mg.mapviewer.features.rtl.RecordingTrackLog;
import mg.mapviewer.graph.AStar;
import mg.mapviewer.graph.GGraph;
import mg.mapviewer.graph.GGraphMulti;
import mg.mapviewer.graph.GGraphTile;
import mg.mapviewer.graph.GNeighbour;
import mg.mapviewer.graph.GNode;
import mg.mapviewer.graph.GNodeRef;
import mg.mapviewer.graph.ApproachModel;
import mg.mapviewer.model.BBox;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.model.TrackLogRefApproach;
import mg.mapviewer.model.WriteableTrackLog;
import mg.mapviewer.model.MultiPointModel;
import mg.mapviewer.model.MultiPointModelImpl;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogPoint;
import mg.mapviewer.model.TrackLogSegment;
import mg.mapviewer.model.TrackLogStatistic;
import mg.mapviewer.model.WriteablePointModel;
import mg.mapviewer.util.AltitudeProvider;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PointModelUtil;
import mg.mapviewer.util.pref.MGPref;
import mg.mapviewer.view.MVLayer;
import mg.mapviewer.view.MultiPointView;
import mg.mapviewer.view.PointView;

public class MSRouting extends MGMicroService {

    private static final Paint PAINT_ROUTE_STROKE = CC.getStrokePaint(R.color.PURPLE_A150, 15);
    private static final Paint PAINT_ROUTE_STROKE2 = CC.getFillPaint(R.color.PURPLE);
    private static final Paint PAINT_APPROACH = CC.getStrokePaint(R.color.BLACK, 2);
    private static final Paint PAINT_RELAXED = CC.getStrokePaint(R.color.BLUE, 2);

    private static final int ZOOM_LEVEL_APPROACHES_VISIBILITY = 19;
    private static final int ZOOM_LEVEL_RELAXED_VISIBILITY = 17;
    private static final int MAX_ROUTE_DISTANCE = 10000; // maximum allowed route length - otherwise AStar is expected be bee too slow


    HashMap<PointModel, RoutePointModel> routePointMap = new HashMap<>(); // map from mtlp points to corresponding rpms
    HashMap<PointModel, RoutePointModel> routePointMap2 = new HashMap<>(); // map from points of routeTrackLog to corresponding rpms
    private HashMap<ApproachModel, MultiPointView> approachViewMap = new HashMap<>();
    public WriteableTrackLog routeTrackLog = null;
    private ArrayList<PointView> relaxedViews = new ArrayList<>();

    private boolean routeRemainings = true;
    private RoutingLineRefProvider routingLineRefProvider;

    final MGPref<Boolean> prefShowRouting = MGPref.get(R.string.MSRouting_bt_ShowRouting, true);
    private final MGPref<Boolean> prefWayDetails = MGPref.get(R.string.MSGrad_pref_WayDetails_key, false);
    private final MGPref<Boolean> prefSnap2Way = MGPref.get(R.string.MSMarker_pref_snap2way_key, true);
    private final MGPref<Boolean> prefEditMarkerTrack = MGPref.get(R.string.MSMarker_qc_EditMarkerTarck, false);
    private final MGPref<Boolean> prefGps = MGPref.get(R.string.MSPosition_prev_GpsOn, false);

    private ViewGroup dashboardRoute = null;

    public MSRouting(MGMapActivity mmActivity) {
        super(mmActivity);
        ttRefreshTime = 50;
        routingLineRefProvider = new RoutingLineRefProvider();
        getApplication().getMS(MSMarker.class).lineRefProvider = routingLineRefProvider;
    }

    @Override
    public ViewGroup initDashboard(ViewGroup dvg, String info) {
        getControlView().setViewGroupColors(dvg, R.color.WHITE, R.color.PURPLE_A100);
        dashboardRoute = dvg;
        return dvg;
    }


    @Override
    protected void start() {
        getApplication().markerTrackLogObservable.addObserver(refreshObserver);
        getMapView().getModel().mapViewPosition.addObserver(refreshObserver);
        register(new RoutingControlLayer(), false);
    }

    @Override
    protected void stop() {
        getApplication().markerTrackLogObservable.deleteObserver(refreshObserver);
        getMapView().getModel().mapViewPosition.removeObserver(refreshObserver);
        unregisterClass(RoutingControlLayer.class);
    }

    @Override
    protected void doRefresh() {
        WriteableTrackLog mtl = getApplication().markerTrackLogObservable.getTrackLog();
        if (prefShowRouting.getValue() && (mtl!= null)){
            updateRouting(mtl);
        } else {
            hideRouting();
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                checkApproachViews();
            }
        });


    }

    private void hideRouting(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                unregisterAll();
                getControlView().setDashboardValue(false, dashboardRoute,null);
                routeTrackLog = null;
            }
        });
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

    RoutePointModel getRoutePointModel(MapDataStore mapFile, PointModel pm){
        RoutePointModel rpm = getRoutePointModel(pm);
        calcApproaches(mapFile, rpm);
        if (prefSnap2Way.getValue()){
            if (rpm.selectedApproach != null){
                if (PointModelUtil.compareTo(rpm.selectedApproach.getApproachNode() , pm) != 0){
                    if (pm instanceof WriteablePointModel) {
                        Log.i(MGMapApplication.LABEL, NameUtil.context());
                        WriteablePointModel wpm = (WriteablePointModel) pm;
                        wpm.setLat(rpm.selectedApproach.getApproachNode().getLat());
                        wpm.setLon(rpm.selectedApproach.getApproachNode().getLon());
                        rpm.resetApproaches();
                        calcApproaches(mapFile, rpm);
                        getApplication().markerTrackLogObservable.changed();
                    }
                }
            }
        }
        return rpm;
    }



    private void updateRouting(WriteableTrackLog mtl){
        unregisterAll(MultiPointView.class);
        if (mtl.getTrackStatistic().getNumPoints() == 0) return;
        MapDataStore mapFile = getActivity().getMapDataStore(mtl.getBBox());
        if (mapFile == null){
            Log.w(MGMapApplication.LABEL, NameUtil.context() + "mapFile is null, updateRouting is impossible!");
            hideRouting();
            return;
        }
        Log.i(MGMapApplication.LABEL, NameUtil.context()+ " start");

        for (TrackLogSegment segment : mtl.getTrackLogSegments()){
            if (segment.size() < 1) continue;
            Iterator<PointModel> iter = segment.iterator();
            RoutePointModel current = getRoutePointModel( mapFile, iter.next() );
            current.newMPM = null;
            while (iter.hasNext()){
                RoutePointModel prev = current;
                current = getRoutePointModel( mapFile, iter.next() );

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
                    current.newMPM = calcRouting(mapFile, prev, current, current.direct, true, current.routingHints);
                }
            }
        }

        // do three things:
        // 1.) move newMPM to currentMPM information
        // 2.) recalc route statistic
        // 3.) setup routingMPMs
        routePointMap2.clear();
        routeTrackLog = new WriteableTrackLog();
        routeTrackLog.startTrack(0);

        TrackLogStatistic routeStatistic = new TrackLogStatistic(-1);
        ArrayList<MultiPointModel> routingMPMs = new ArrayList<>();
        for (TrackLogSegment segment : mtl.getTrackLogSegments()){
            routeTrackLog.startSegment(0);
            routeStatistic.updateWithPoint(null);
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
                                routeStatistic.updateWithPoint(pm);
                                routeTrackLog.addPoint(pm);
                                routePointMap2.put(pm,rpm);
                            }
                            lastPM = pm;
                        }
                        routingMPMs.add(rpm.newMPM);
                    }
                }
            }
            routeTrackLog.stopSegment(0);
        }
        routeTrackLog.stopTrack(0);

        showTrack(routeTrackLog, PAINT_ROUTE_STROKE, false, 0);
        if (!getSharedPreferences().getBoolean(getResources().getString(R.string.MSMarker_pref_showMtl_key), false)){
            showTrack(mtl, PAINT_ROUTE_STROKE2, false, 15, true);
        }
        calcRemainingStatistic(routeTrackLog);
        getControlView().setDashboardValue(true, dashboardRoute, routeTrackLog.getTrackStatistic());
    }

    private void calcRemainingStatistic(WriteableTrackLog routeTrackLog){
        if (routeRemainings && prefGps.getValue()){
            PointModel lastPos = getApplication().lastPositionsObservable.lastGpsPoint;
            if (lastPos != null){
                TrackLogRefApproach bestMatch = routeTrackLog.getBestDistance(lastPos);
                if ((bestMatch != null) && (bestMatch.getApproachPoint() != null)){
                    routeTrackLog.recalcStatistic(bestMatch.getApproachPoint(), bestMatch.getSegmentIdx(), bestMatch.getEndPointIndex());
                    routeTrackLog.getTrackStatistic().segmentIdx = -2; // indicates Remainings statistic

                }
            }
        }
    }



    MultiPointModelImpl calcRouting(MapDataStore mapFile, RoutePointModel source, RoutePointModel target, boolean direct, boolean distanceCheck) {
        return calcRouting(mapFile,source,target,direct,distanceCheck,null);
    }

    MultiPointModelImpl calcRouting(MapDataStore mapFile, RoutePointModel source, RoutePointModel target, boolean direct, boolean distanceCheck, Map<PointModel, RoutingHint> hints){

        MultiPointModelImpl mpm = new MultiPointModelImpl();
        Log.i(MGMapApplication.LABEL, NameUtil.context());
        boolean bShowRelaxed = (prefWayDetails.getValue() && getMapView().getModel().mapViewPosition.getZoomLevel() >= ZOOM_LEVEL_RELAXED_VISIBILITY);

        GNode gStart = source.getApproachNode();
        GNode gEnd = target.getApproachNode();
        Log.i(MGMapApplication.LABEL, NameUtil.context()+ " start "+gStart+" end "+gEnd);

        double distLimit = distanceCheck? acceptedRouteDistance(source.mtlp, target.mtlp):Double.MAX_VALUE;
        GGraph multi = null;

        if ((gStart != null) && (gEnd != null) && (distLimit > 0) && !direct){
            BBox bBox = new BBox().extend(source.mtlp).extend(target.mtlp);
            bBox.extend( Math.max(PointModelUtil.getCloseThreshold(), PointModelUtil.distance(source.mtlp,target.mtlp)*0.7 + 2*PointModelUtil.getCloseThreshold() ) );
            multi = new GGraphMulti(GGraphTile.getGGraphTileList(mapFile,bBox));
            multi.createOverlaysForApproach(source.selectedApproach);
            multi.createOverlaysForApproach(target.selectedApproach);

            Log.i(MGMapApplication.LABEL, NameUtil.context());
            { // perform an AStar on this graph
                AStar d = new AStar(multi);

                for (PointView pv : relaxedViews){
                    unregister(pv);
                }
                relaxedViews.clear();
                List<GNodeRef> path = d.perform(gStart, gEnd, distLimit);
                if (bShowRelaxed){
                    for (GNode gNode : d.getRelaxedList()){
                        PointView pv = new PointView(gNode, PAINT_RELAXED);
                        relaxedViews.add(pv);
                        register(pv);
                    }
                }

                Log.i(MGMapApplication.LABEL, NameUtil.context()+ " "+ path.size()+" "+d.getResult());
                for (GNodeRef gnr : path){
                    mpm.addPoint(gnr.getNode() );
                }
            }
            Log.i(MGMapApplication.LABEL, NameUtil.context());

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
        mpm.setRoute(mpm.size() != 0);

        if (mpm.size() == 0) { // no bRouting required or bRouting not possible due to missing approach or no route found
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
        Log.i(MGMapApplication.LABEL, NameUtil.context());
        return mpm;
    }


    void calcApproaches(MapDataStore mapFile, RoutePointModel rpm){

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

        ArrayList<GGraphTile> tiles = GGraphTile.getGGraphTileList(mapFile, mtlpBBox);
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
                                    float hgtAlt = AltitudeProvider.getAltitude(pmApproach.getLat(), pmApproach.getLon());
                                    GNode approachNode = new GNode(pmApproach.getLat(), pmApproach.getLon(), hgtAlt, distance); // so we get a new node for the approach, since pmApproach will be overwritten in next cycle
                                    ApproachModel approach = new ApproachModel(pointModel, node, neighbour.getNeighbourNode(), approachNode);
                                    approaches.add(approach);
                                    gGraphTile.addObserver(rpm);
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

    TrackLog calcRouteTrackLog(WriteableTrackLog mtl){
        String name = mtl.getName();
        name = name.replace("MarkerTrack","MarkerRoute");
        RecordingTrackLog rtl = new RecordingTrackLog(false);
        rtl.setName(name);
        rtl.startTrack(mtl.getTrackStatistic().getTStart());
        rtl.setReworkData(false);

        for (TrackLogSegment segment : mtl.getTrackLogSegments()){
            if (segment.size() > 0){
                rtl.startSegment(PointModel.NO_TIME );
                for (PointModel mtlp : segment){
                    RoutePointModel rpm = routePointMap.get(mtlp);
                    if (rpm.currentMPM!= null){
                        for (PointModel pointModel : rpm.currentMPM){
                            rtl.addPoint( pointModel );
                        }
                    }
                }
                rtl.stopSegment(PointModel.NO_TIME );
            }
        }

        rtl.stopTrack(PointModel.NO_TIME );
        return rtl;
    }


    private double acceptedRouteDistance(PointModel pmStart, PointModel pmEnd){
        double distance = PointModelUtil.distance(pmStart,pmEnd);
        double res = 0;
        if (distance < MAX_ROUTE_DISTANCE){ // otherwise it will take too long
            res = Math.min (3 * PointModelUtil.distance(pmStart,pmEnd) + 2 * PointModelUtil.getCloseThreshold(), MAX_ROUTE_DISTANCE);
        }
        return res;
    }

    public Control[] getMenuRouteControls(){
        return new Control[]{
                new RouteOnOffControl(this),
                new RouteOptimizeControl(this),
                new RouteExportControl(this)};


    }


    private void checkApproachViews(){
        WriteableTrackLog mtl = getApplication().markerTrackLogObservable.getTrackLog();

        boolean visibility = ((getMapView().getModel().mapViewPosition.getZoomLevel() >= ZOOM_LEVEL_APPROACHES_VISIBILITY) && (prefWayDetails.getValue()));

        for (MultiPointView approachView : approachViewMap.values()){
            unregister(approachView,false);
        }
        approachViewMap.clear();
        BBox mapViewBBox = getMapViewUtility().getMapViewBBox();

        if ((mtl != null) && (visibility)){
            for (TrackLogSegment segment : mtl.getTrackLogSegments()){
                for (int tlpIdx = 0; tlpIdx<segment.size(); tlpIdx++){
                    RoutePointModel rpm = routePointMap.get(segment.get(tlpIdx));


                    if ((rpm != null) && (rpm.getApproaches() != null)){
                        for (ApproachModel approach : rpm.getApproaches()){

                            if (mapViewBBox.intersects(approach.getBBox())){
                                approachViewMap.put(approach,new MultiPointView(approach, PAINT_APPROACH));
                                register(approachViewMap.get(approach), false);
                            }

                        }
                    }
                }
            }
        }
    }


    void optimize(){
        WriteableTrackLog mtl = getApplication().markerTrackLogObservable.getTrackLog();
        MapDataStore mapFile = getActivity().getMapDataStore(mtl.getBBox());
        RouteOptimizer ro = new RouteOptimizer(this, mapFile);
        ro.optimize(mtl);
        getApplication().markerTrackLogObservable.changed();
    }


    public class RoutingControlLayer extends MVLayer {

        @Override
        public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
            if (!prefEditMarkerTrack.getValue()) return false;
            if (!prefShowRouting.getValue()) return false;

            WriteableTrackLog mtl = getApplication().markerTrackLogObservable.getTrackLog();
            if (mtl != null){
                PointModel pmTap = new PointModelImpl(tapLatLong.latitude, tapLatLong.longitude);
                TrackLogRefApproach pointRef = mtl.getBestPoint(pmTap, getMapViewUtility().getCloseThreshouldForZoomLevel());
                TrackLogRefApproach lineRef = routingLineRefProvider.getBestDistance(mtl, pmTap, getMapViewUtility().getCloseThreshouldForZoomLevel());

                if ((pointRef == null) && (lineRef != null)){
                    TrackLogSegment segment = mtl.getTrackLogSegment(lineRef.getSegmentIdx());
                    int tlpIdx = lineRef.getEndPointIndex();
                    PointModel mtlp = segment.get(tlpIdx);
                    RoutePointModel rpm = routePointMap.get(mtlp);
                    if (rpm != null){
                        rpm.direct = !rpm.direct;
                        rpm.directChanged = true;
                        getApplication().markerTrackLogObservable.changed();
                        return true;
                    }
                }
            }
            return false;
        }

    }

    public class RoutingLineRefProvider implements MSMarker.LineRefProvider{
        @Override
        public TrackLogRefApproach getBestDistance(WriteableTrackLog mtl, PointModel pm, double threshold) {
            return getRoutingLineApproach(pm, threshold);
        }
    }

    public TrackLogRefApproach getRoutingLineApproach(PointModel pm, double threshold){
        if (routeTrackLog != null){
            TrackLogRefApproach bestMatch = routeTrackLog.getBestDistance(pm, threshold);
            if (bestMatch != null){
                TrackLogSegment segment = routeTrackLog.getTrackLogSegment(bestMatch.getSegmentIdx());
                PointModel rtlpm = segment.get(bestMatch.getEndPointIndex());
                RoutePointModel rpm = routePointMap2.get(rtlpm);
                PointModel mtlp = rpm.getMtlp();
                WriteableTrackLog mtl = getApplication().markerTrackLogObservable.getTrackLog();
                TrackLogRefApproach bestPoint = mtl.getBestPoint(mtlp, 1);
                return bestPoint;
            }
        }
        return null;
    }
}
