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

import android.util.Log;
import android.view.ViewGroup;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.model.DisplayModel;

import java.util.HashMap;
import java.util.Observer;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.marker.FSMarker;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.TrackLogRefApproach;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.activity.mgmap.util.CC;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.activity.mgmap.view.LabeledSlider;
import mg.mgmap.activity.mgmap.view.MVLayer;
import mg.mgmap.activity.mgmap.view.MultiPointView;
import mg.mgmap.activity.mgmap.view.PointView;

public class FSRouting extends FeatureService {

    private static final Paint PAINT_ROUTE_STROKE = CC.getStrokePaint(R.color.PURPLE_A150, DisplayModel.getDeviceScaleFactor()*5.0f);
    private static final Paint PAINT_ROUTE_STROKE2 = CC.getFillPaint(R.color.PURPLE_A150);
    private static final Paint PAINT_APPROACH = CC.getStrokePaint(R.color.BLACK, 2);
    private static final Paint PAINT_RELAXED = CC.getStrokePaint(R.color.BLUE, 2);
    private final Paint PAINT_STROKE_GL = CC.getStrokePaint(R.color.GRAY100_A100, getMapViewUtility().getTrackWidth()*1.4f);

    private static final int ZOOM_LEVEL_APPROACHES_VISIBILITY = 19;
    private static final int ZOOM_LEVEL_RELAXED_VISIBILITY = 17;

    private final HashMap<ApproachModel, MultiPointView> approachViewMap = new HashMap<>();
    final RoutingEngine routingEngine;

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
        routingEngine = new RoutingEngine(mmActivity.getGGraphTileFactory(), prefSnap2Way);
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
        getControlView().setDashboardValue(prefMtlVisibility.getValue(), dashboardRoute, calcRemainingStatistic(rotl));

        checkRelaxedViews(mtl);
        checkApproachViews(mtl);
    }

    private void updateRouting(){
        WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
        WriteableTrackLog rotl = null;
        if ((mtl != null) && (mtl.getTrackStatistic().getNumPoints() > 0)){
            Log.d(MGMapApplication.LABEL, NameUtil.context()+ " Start");
            rotl = routingEngine.updateRouting2(mtl, application.routeTrackLogObservable.getTrackLog());
            Log.d(MGMapApplication.LABEL, NameUtil.context()+" End");
        }
        application.routeTrackLogObservable.setTrackLog(rotl);
        refreshObserver.onChange(); // trigger visualization
    }

    private TrackLogStatistic calcRemainingStatistic(WriteableTrackLog routeTrackLog){
        TrackLogStatistic dashboardStatistic = null;
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
        return dashboardStatistic;
    }

    private void checkRelaxedViews(TrackLog mtl){
        if ((mtl != null) && prefWayDetails.getValue() && getMapView().getModel().mapViewPosition.getZoomLevel() >= ZOOM_LEVEL_RELAXED_VISIBILITY){
            if (mtl.getTrackStatistic().getNumPoints() >= 2){
                for (PointModel pm : routingEngine.getCurrentRelaxedNodes()){
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
                    RoutePointModel rpm = routingEngine.getRoutePointMap().get(segment.get(tlpIdx));


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
        RouteOptimizer ro = new RouteOptimizer(getActivity().getGGraphTileFactory(), routingEngine);
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
                    RoutePointModel rpm = routingEngine.getRoutePointMap().get(mtlp);
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
                RoutePointModel rpm = routingEngine.getRoutePointMap2().get(rtlpm);
                PointModel mtlp = (rpm==null)?null:rpm.getMtlp();
                WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
                return mtl.getBestPoint(mtlp, 1);
            }
        }
        return null;
    }
}
