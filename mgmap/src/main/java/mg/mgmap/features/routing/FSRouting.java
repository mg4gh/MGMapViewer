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
import android.view.ViewGroup;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.model.DisplayModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observer;
import java.util.TreeSet;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.features.marker.FSMarker;
import mg.mgmap.graph.AStar;
import mg.mgmap.graph.GGraph;
import mg.mgmap.graph.GGraphMulti;
import mg.mgmap.graph.GGraphTile;
import mg.mgmap.graph.GNeighbour;
import mg.mgmap.graph.GNode;
import mg.mgmap.graph.GNodeRef;
import mg.mgmap.graph.ApproachModel;
import mg.mgmap.model.BBox;
import mg.mgmap.model.ExtendedPointModelImpl;
import mg.mgmap.model.PointModelImpl;
import mg.mgmap.model.TrackLogRefApproach;
import mg.mgmap.model.TrackLogStatistic;
import mg.mgmap.model.WriteableTrackLog;
import mg.mgmap.model.MultiPointModelImpl;
import mg.mgmap.model.PointModel;
import mg.mgmap.model.TrackLog;
import mg.mgmap.model.TrackLogPoint;
import mg.mgmap.model.TrackLogSegment;
import mg.mgmap.model.WriteablePointModel;
import mg.mgmap.util.AltitudeProvider;
import mg.mgmap.util.CC;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.PointModelUtil;
import mg.mgmap.util.Pref;
import mg.mgmap.view.ExtendedTextView;
import mg.mgmap.view.LabeledSlider;
import mg.mgmap.view.MVLayer;
import mg.mgmap.view.MultiPointView;
import mg.mgmap.view.PointView;

public class FSRouting extends FeatureService {

    private static final Paint PAINT_ROUTE_STROKE = CC.getStrokePaint(R.color.PURPLE_A150, DisplayModel.getDeviceScaleFactor()*5.0f);
    private static final Paint PAINT_ROUTE_STROKE2 = CC.getFillPaint(R.color.PURPLE_A150);
    private static final Paint PAINT_APPROACH = CC.getStrokePaint(R.color.BLACK, 2);
    private static final Paint PAINT_RELAXED = CC.getStrokePaint(R.color.BLUE, 2);
    private final Paint PAINT_STROKE_GL = CC.getStrokePaint(R.color.GRAY100_A100, getMapViewUtility().getTrackWidth()*1.4f);

    private static final int ZOOM_LEVEL_APPROACHES_VISIBILITY = 19;
    private static final int ZOOM_LEVEL_RELAXED_VISIBILITY = 17;
    private static final int MAX_ROUTE_DISTANCE = 10000; // maximum allowed route length - otherwise AStar is expected be bee too slow


    HashMap<PointModel, RoutePointModel> routePointMap = new HashMap<>(); // map from mtlp points to corresponding rpms
    HashMap<PointModel, RoutePointModel> routePointMap2 = new HashMap<>(); // map from points of routeTrackLog to corresponding rpms
    private final HashMap<ApproachModel, MultiPointView> approachViewMap = new HashMap<>();
    private final ArrayList<PointModel> currentRelaxedNodes = new ArrayList<>();
    private TrackLogStatistic dashboardStatistic = null;

    private final RoutingLineRefProvider routingLineRefProvider;

    private final Pref<Boolean> prefWayDetails = getPref(R.string.FSGrad_pref_WayDetails_key, false);
    private final Pref<Boolean> prefSnap2Way = getPref(R.string.FSRouting_pref_snap2way_key, true);
    private final Pref<Boolean> prefEditMarkerTrack = getPref(R.string.FSMarker_qc_EditMarkerTrack, false);
    private final Pref<Boolean> prefGps = getPref(R.string.FSPosition_pref_GpsOn, false);
    private final Pref<Boolean> prefRouteGL = getPref(R.string.FSRouting_pref_RouteGL, false);

    private final Pref<Boolean> prefAutoSwitcher = getPref(R.string.FSMarker_pref_auto_switcher, true);
    private final Pref<Boolean> prefAutoMarkerSetting = getPref(R.string.FSMarker_pref_auto_key, true);
    private final Pref<Float> prefAlphaMtl = getPref(R.string.FSMarker_pref_alphaMTL, 1.0f);
    private final Pref<Float> prefAlphaRotl = getPref(R.string.FSRouting_pref_alphaRoTL, 1.0f);
    private final Pref<Boolean> prefMtlVisibility = getPref(R.string.FSMarker_pref_MTL_visibility, false);
    private final Pref<Integer> prefZoomLevel = getPref(R.string.FSBeeline_pref_ZoomLevel, 15);
    private final Pref<Boolean> prefMapMatching = new Pref<>(false);
    private final Pref<Boolean> prefMapMatchingEnabled = new Pref<>(false);
    private final Pref<Boolean> prefRoutingHints = getPref(R.string.FSRouting_qc_RoutingHint, false);
    private final Pref<Boolean> prefRoutingHintsEnabled = new Pref<>(false);

    private ViewGroup dashboardRoute = null;
    private volatile boolean refreshRequired = false;
    private boolean runRouteCalcThread = true;
    private final MGMapApplication application;

    public FSRouting(MGMapActivity mmActivity, FSMarker fsMarker) {
        super(mmActivity);
        application = getApplication();
        ttRefreshTime = 50;
        routingLineRefProvider = new RoutingLineRefProvider();
        fsMarker.lineRefProvider = routingLineRefProvider;
        prefMapMatching.addObserver((o, arg) -> optimize());
        Observer matchingEnabledObserver = (o, arg) -> prefMapMatchingEnabled.setValue( prefMtlVisibility.getValue() && (prefAlphaRotl.getValue() > 0.25f) );
        prefMtlVisibility.addObserver(matchingEnabledObserver);
        prefAlphaRotl.addObserver(matchingEnabledObserver);
        prefAutoSwitcher.addObserver((o, arg) -> {
            if (prefAutoMarkerSetting.getValue()){
                prefSnap2Way.setValue(prefAutoSwitcher.getValue()); // smallMTL
                if (prefAlphaRotl.getValue() < 0.75f){
                    prefAlphaRotl.setValue(1.0f);
                }
            }
        });

        new Thread(){
            @Override
            public void run() {
                Log.d(MGMapApplication.LABEL, NameUtil.context()+"  routeCalcThread created");
                while (runRouteCalcThread){
                    try {
                        synchronized (FSRouting.this){
                            FSRouting.this.wait(1000);
                        }
                        if (refreshRequired){
                            refreshRequired = false;
                            updateRouting();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                Log.d(MGMapApplication.LABEL, NameUtil.context()+"  routeCalcThread terminating");
            }
        }.start();



        application.markerTrackLogObservable.addObserver((o, arg) -> {
            refreshRequired = true; // refresh route calculation is required
//          NameUtil.logContext(6); // to see where the update was triggered
            Log.d(MGMapApplication.LABEL, NameUtil.context()+" set refreshRequired");
            synchronized (FSRouting.this){
                FSRouting.this.notifyAll();
            }
        });

        prefZoomLevel.addObserver(refreshObserver);
        prefAlphaRotl.addObserver(refreshObserver);
        prefRouteGL.addObserver(refreshObserver);
        prefGps.addObserver(refreshObserver);
        application.lastPositionsObservable.addObserver(refreshObserver);

        if (getPref(R.string.MGMapApplication_pref_Restart, false).getValue()){
            prefRoutingHints.setValue(false);
        }
        Observer routingHintsEnabledObserver = (o, arg) -> {
            prefRoutingHintsEnabled.setValue( prefGps.getValue() && prefMtlVisibility.getValue());
            if (!prefRoutingHintsEnabled.getValue()){
                prefRoutingHints.setValue(false);
            }
        };
        prefGps.addObserver(routingHintsEnabledObserver);
        prefMtlVisibility.addObserver(routingHintsEnabledObserver);

        register(new RoutingControlLayer(), false);
    }

    @Override
    public ViewGroup initDashboard(ViewGroup dvg, String info) {
        super.initDashboard(dvg,info);
        getControlView().setViewGroupColors(dvg, R.color.WHITE, R.color.PURPLE_A100);
        dashboardRoute = dvg;
        return dvg;
    }

    @Override
    public LabeledSlider initLabeledSlider(LabeledSlider lsl, String info) {
        if ("rotl".equals(info)) {
            lsl.initPrefData(prefMtlVisibility, prefAlphaRotl, CC.getColor(R.color.PURPLE), "RouteTrackLog");
        }
        return lsl;
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info) {
        super.initQuickControl(etv,info);
        if ("matching".equals(info)) {
            etv.setPrAction(prefMapMatching);
            etv.setData(R.drawable.matching);
            etv.setDisabledData(prefMapMatchingEnabled,R.drawable.matching_dis);
            etv.setHelp(r(R.string.FSRouting_qcMapMatching_Help));
        } else if ("routingHint".equals(info)){
            etv.setData(prefRoutingHints,R.drawable.routing_hints2, R.drawable.routing_hints1);
            etv.setPrAction(prefRoutingHints);
            etv.setDisabledData(prefRoutingHintsEnabled, R.drawable.routing_hints_dis);
            etv.setHelp(r(R.string.FSRouting_qcRoutingHint_Help)).setHelp(r(R.string.FSRouting_qcRoutingHint_Help1),r(R.string.FSRouting_qcRoutingHint_Help2));
        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        runRouteCalcThread = false;
        synchronized (FSRouting.this){
            FSRouting.this.notifyAll();
        }
    }

    @Override
    protected void doRefreshResumedUI() {
        unregisterAll();
        WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
        WriteableTrackLog rotl = application.routeTrackLogObservable.getTrackLog();

        if (rotl != null){
            if (prefRouteGL.getValue()){
                CC.initGlPaints( prefAlphaRotl.getValue() );
                showTrack(rotl, CC.getAlphaClone(PAINT_STROKE_GL, prefAlphaRotl.getValue()), true, 0);
            } else {
                showTrack(rotl, CC.getAlphaClone(PAINT_ROUTE_STROKE, prefAlphaRotl.getValue()), false,0);
            }
        }
        if (mtl != null){
            if (prefAlphaMtl.getValue() < 0.5f){ // is considered as low visibility
                showTrack(mtl, CC.getAlphaCloneFill(PAINT_ROUTE_STROKE2, prefAlphaRotl.getValue()) , false,  (int)(DisplayModel.getDeviceScaleFactor()*6.0f), true);
            }
        }
        calcRemainingStatistic(rotl);
        getControlView().setDashboardValue(prefMtlVisibility.getValue(), dashboardRoute, dashboardStatistic);

        checkRelaxedViews(mtl);
        checkApproachViews(mtl);

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
                        Log.d(MGMapApplication.LABEL, NameUtil.context());
                        WriteablePointModel wpm = (WriteablePointModel) pm;
                        wpm.setLat(rpm.selectedApproach.getApproachNode().getLat());
                        wpm.setLon(rpm.selectedApproach.getApproachNode().getLon());
                        rpm.resetApproaches();
                        calcApproaches(mapFile, rpm);
                    }
                }
            }
        }
        return rpm;
    }

    private void updateRouting(){
        WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
        WriteableTrackLog rotl = null;
        if ((mtl != null) && (mtl.getTrackStatistic().getNumPoints() > 0)){
            MapDataStore mapFile = getActivity().getMapDataStore(mtl.getBBox());
            if (mapFile == null){
                Log.w(MGMapApplication.LABEL, NameUtil.context() + "mapFile is null, updateRouting is impossible!");
            } else {
                Log.d(MGMapApplication.LABEL, NameUtil.context()+ " Start");
                rotl = updateRouting2(mapFile, mtl, application.routeTrackLogObservable.getTrackLog());
                Log.d(MGMapApplication.LABEL, NameUtil.context()+" End");
            }
        }
        application.routeTrackLogObservable.setTrackLog(rotl);
        refreshObserver.onChange(); // trigger visualization
    }

    private WriteableTrackLog updateRouting2(MapDataStore mapFile, TrackLog mtl, WriteableTrackLog rotl){
        boolean routeModified = false;

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
                    routeModified = true;
                    current.newMPM = calcRouting(mapFile, prev, current, current.direct, current.routingHints, currentRelaxedNodes);
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

    private void calcRemainingStatistic(WriteableTrackLog routeTrackLog){
        dashboardStatistic = null;
        if (routeTrackLog != null){
            dashboardStatistic = routeTrackLog.getTrackStatistic();
            if (prefGps.getValue()){
                PointModel lastPos = application.lastPositionsObservable.lastGpsPoint;
                if (lastPos != null){
                    TrackLogRefApproach bestMatch = routeTrackLog.getBestDistance(lastPos);
                    if ((bestMatch != null) && (bestMatch.getApproachPoint() != null)){
                        dashboardStatistic = new TrackLogStatistic();
                        routeTrackLog.remainStatistic(dashboardStatistic, bestMatch.getApproachPoint(), bestMatch.getSegmentIdx(), bestMatch.getEndPointIndex());
                        dashboardStatistic.setSegmentIdx(-2); // indicates Remainings statistic
                    }
                }
            }
        }
    }



    MultiPointModelImpl calcRouting(MapDataStore mapFile, RoutePointModel source, RoutePointModel target) {
        return calcRouting(mapFile,source,target,false,null,null);
    }

    MultiPointModelImpl calcRouting(MapDataStore mapFile, RoutePointModel source, RoutePointModel target, boolean direct, Map<PointModel, RoutingHint> hints, ArrayList<PointModel> relaxedNodes){

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
                ArrayList<GGraphTile> gGraphTileList = GGraphTile.getGGraphTileList(mapFile,bBox);
                if (gGraphTileList.size() > GGraphTile.CACHE_LIMIT) throw new RuntimeException("Request for GGraphMulti exceeds cache size.");
                multi = new GGraphMulti(gGraphTileList);
                multi.createOverlaysForApproach(source.selectedApproach);
                multi.createOverlaysForApproach(target.selectedApproach);

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
                                    float hgtAlt = AltitudeProvider.getAlt(pmApproach);
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

    private double acceptedRouteDistance(PointModel pmStart, PointModel pmEnd){
        double distance = PointModelUtil.distance(pmStart,pmEnd);
        double res = 0;
        if (distance < MAX_ROUTE_DISTANCE){ // otherwise it will take too long
            res = Math.min (3 * PointModelUtil.distance(pmStart,pmEnd) + 2 * PointModelUtil.getCloseThreshold(), MAX_ROUTE_DISTANCE);
        }
        return res;
    }

    private void checkRelaxedViews(TrackLog mtl){
        if ((mtl != null) && prefWayDetails.getValue() && getMapView().getModel().mapViewPosition.getZoomLevel() >= ZOOM_LEVEL_RELAXED_VISIBILITY){
            if (mtl.getTrackStatistic().getNumPoints() >= 2){
                for (PointModel pm : currentRelaxedNodes){
                    register( new PointView(pm, PAINT_RELAXED));
                }
            }
        }
    }

    private void checkApproachViews(TrackLog mtl){
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
        WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
        MapDataStore mapFile = getActivity().getMapDataStore(mtl.getBBox());
        RouteOptimizer ro = new RouteOptimizer(this, mapFile);
        ro.optimize(mtl);
        application.markerTrackLogObservable.changed();
    }


    public class RoutingControlLayer extends MVLayer {

        @Override
        public boolean onLongPress(LatLong tapLatLong, Point layerXY, Point tapXY) {
            if (!prefEditMarkerTrack.getValue()) return false;
            if (prefAlphaRotl.getValue() < 0.25f) return false;

            WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
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
                        application.markerTrackLogObservable.changed();
                        return true;
                    }
                }
            }
            return false;
        }

    }

    public class RoutingLineRefProvider implements FSMarker.LineRefProvider{
        @Override
        public TrackLogRefApproach getBestDistance(WriteableTrackLog mtl, PointModel pm, double threshold) {
            return getRoutingLineApproach(pm, threshold);
        }
    }

    public TrackLogRefApproach getRoutingLineApproach(PointModel pm, double threshold){
        TrackLog routeTrackLog = application.routeTrackLogObservable.getTrackLog();
        if (routeTrackLog != null){
            TrackLogRefApproach bestMatch = routeTrackLog.getBestDistance(pm, threshold);
            if (bestMatch != null){
                TrackLogSegment segment = routeTrackLog.getTrackLogSegment(bestMatch.getSegmentIdx());
                PointModel rtlpm = segment.get(bestMatch.getEndPointIndex());
                RoutePointModel rpm = routePointMap2.get(rtlpm);
                PointModel mtlp = rpm.getMtlp();
                WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
                return mtl.getBestPoint(mtlp, 1);
            }
        }
        return null;
    }
}
