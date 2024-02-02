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

import android.view.View;
import android.view.ViewGroup;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.map.model.DisplayModel;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K1S1;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K1S2;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K1S3;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S1;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S2;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S3;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K3S1;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K3S2;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K3S3;
import mg.mgmap.activity.mgmap.features.routing.profile.ShortestDistance;
import mg.mgmap.activity.mgmap.features.routing.profile.TrekkingBike;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.marker.FSMarker;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.GGraphTile;
import mg.mgmap.generic.graph.GGraphTileFactory;
import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogRefApproach;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.gpx.GpxExporter;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.activity.mgmap.view.LabeledSlider;
import mg.mgmap.activity.mgmap.view.MultiPointView;
import mg.mgmap.activity.mgmap.view.PointView;

public class FSRouting extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final Paint PAINT_ROUTE_STROKE = CC.getStrokePaint(R.color.CC_PURPLE, DisplayModel.getDeviceScaleFactor()*5.0f);
    private static final Paint PAINT_ROUTE_STROKE2 = CC.getFillPaint(R.color.CC_PURPLE);
    private static final Paint PAINT_RELAXED = CC.getStrokePaint(R.color.CC_BLUE, 2);
    private static final Paint PAINT_RELAXED2 = CC.getStrokePaint(R.color.CC_GREEN, 2);
    private final Paint PAINT_STROKE_GL = CC.getStrokePaint(R.color.CC_GRAY100_A100, getMapViewUtility().getTrackWidth()*1.4f);

    private static final int ZOOM_LEVEL_RELAXED_VISIBILITY = 16;

    private static ArrayList<RoutingProfile> definedRoutingProfiles = null;

    public static ArrayList<RoutingProfile> getDefinedRoutingProfiles(){
        return definedRoutingProfiles;
    }

    private final RoutingEngine routingEngine;
    private final GGraphTileFactory gFactory;
    private final RoutingContext interactiveRoutingContext = new RoutingContext(
            1000000, // no limit
            false, // no extra snap, since FSMarker snaps point zoom level dependent
            20, // accept long detours in interactive mode
            1); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent


    private final FSMarker.MtlSupportProvider mtlSupportProvider;

    private final Pref<Boolean> prefWayDetails = getPref(R.string.FSGrad_pref_WayDetails_key, false);
    private final Pref<Boolean> prefGps = getPref(R.string.FSPosition_pref_GpsOn, false);
    private final Pref<Boolean> prefRouteGL = getPref(R.string.FSRouting_pref_RouteGL, false);

    private final Pref<Float> prefAlphaRotl = getPref(R.string.FSRouting_pref_alphaRoTL, 1.0f);
    private final Pref<Boolean> prefMtlVisibility = getPref(R.string.FSMarker_pref_MTL_visibility, false);
    private final Pref<Boolean> prefStlVisibility = getPref(R.string.FSATL_pref_STL_visibility, false);
    private final Pref<Integer> prefZoomLevel = getPref(R.string.FSBeeline_pref_ZoomLevel, 15);
    private final Pref<Boolean> prefMapMatching = new Pref<>(false);
    private final Pref<Boolean> prefRoutingHints = getPref(R.string.FSRouting_qc_RoutingHint, false);
    private final Pref<Boolean> prefRoutingHintsEnabled = new Pref<>(false);
    private final Pref<Boolean> prefEditMarkerTrack =  getPref(R.string.FSMarker_qc_EditMarkerTrack, false);
    private final String defaultRoutingProfileId = RoutingProfile.constructId(ShortestDistance.class);
    private final Pref<String> prefRoutingProfileId = getPref(R.string.FSRouting_pref_currentRoutingProfile, defaultRoutingProfileId);
    private final Pref<Boolean> prefCalcRouteInProgress = getPref(R.string.FSRouting_pref_calcRouteInProgress, false);
    private final ArrayList<ExtendedTextView> profileETVs = new ArrayList<>();
    private final Pref<Boolean> prefRouteSavable = new Pref<>(false); // when MTL is changed


    private ViewGroup dashboardRoute = null;
    private final AtomicInteger refreshRequired = new AtomicInteger(0);
    private boolean runRouteCalcThread = true;
    private final MGMapApplication application;
    private MultiPointView dndVisualisationLayer = null;
    private final PrefCache prefCache;

    public FSRouting(MGMapActivity mgActivity, FSMarker fsMarker, GGraphTileFactory gFactory) {
        super(mgActivity);
        this.gFactory = gFactory;
        application = getApplication();
        routingEngine = new RoutingEngine(gFactory, interactiveRoutingContext);
        ttRefreshTime = 50;
        mtlSupportProvider = new AdvancedMtlSupportProvider();
        fsMarker.mtlSupportProvider = mtlSupportProvider;
        prefMapMatching.addObserver((e) -> {
            TrackLog selectedTrackLog = getApplication().availableTrackLogsObservable.selectedTrackLogRef.getTrackLog();
            if (selectedTrackLog != null){
                prepareOptimize();
                application.addBgJob (new BgJob(){
                    @Override
                    protected void doJob() {
                        fsMarker.createMarkerTrackLog(selectedTrackLog);
                        optimize();
                    }
                });
            }
        });
        new Thread(() -> {
            int lastRefreshRequired = refreshRequired.get();
            mgLog.d("routeCalcThread created");
            while (runRouteCalcThread){
                try {
                    synchronized (FSRouting.this){
                        FSRouting.this.wait(100);
                    }
                    if (refreshRequired.get() > 0){
                        if (lastRefreshRequired == refreshRequired.get()){ // no further refreshRequest within the last 100ms -> start calculation
                            refreshRequired.set(0);
                            lastRefreshRequired = 0;
                            prefCalcRouteInProgress.setValue(true);
                            updateRouting();
                            prefCalcRouteInProgress.setValue(false);
                        } else {
                            lastRefreshRequired = refreshRequired.get(); // save current value of refreshRequired -> enable detection of further changes in next loop cycle
                        }

                    } else { //just to make sure, nothing is left
                        if (dndVisualisationLayer != null){
                            dndVisualisationLayer = null;
                            doRefresh();
                        }
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mgLog.d("routeCalcThread terminating");
        }).start();

        application.markerTrackLogObservable.addObserver((e) -> {
            TrackLog mtl = application.markerTrackLogObservable.getTrackLog();
            if ((mtl != null) && (mtl.getRoutingProfileId() != null) && ( !prefRoutingProfileId.getValue().equals(mtl.getRoutingProfileId()) )){
                mgLog.d("mtl doesn't match prefRoutingProfileId");
                TrackLog rotl = application.routeTrackLogObservable.getTrackLog();
                if ((rotl == null) || (rotl.getReferencedTrackLog() != mtl)){
                    mgLog.d("set prefRoutingProfileId to mtl value: "+mtl.getRoutingProfileId());
                    prefRoutingProfileId.setValue(mtl.getRoutingProfileId());
                }
            }
            refreshRequired.incrementAndGet(); // refresh route calculation is required
            mgLog.d("set refreshRequired");
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
        Observer routingHintsEnabledObserver = (e) -> {
            prefRoutingHintsEnabled.setValue( prefGps.getValue() && prefMtlVisibility.getValue());
            if (!prefRoutingHintsEnabled.getValue()){
                prefRoutingHints.setValue(false);
            }
        };
        prefGps.addObserver(routingHintsEnabledObserver);
        prefMtlVisibility.addObserver(routingHintsEnabledObserver);

        Pref<Boolean> prefUseRoutingProfiles = getPref(R.string.preferences_routingProfile_key, false);
        prefUseRoutingProfiles.addObserver(evt -> {
            mgLog.d("reset to defaultRoutingProfileId");
            prefRoutingProfileId.setValue(defaultRoutingProfileId);
        });
        prefCache = getActivity().getPrefCache();
        prefEditMarkerTrack.addObserver(evt -> {
            ViewGroup parent = activity.findViewById(R.id.routingProfiles);
            parent.removeAllViews();
            if (prefUseRoutingProfiles.getValue() && prefEditMarkerTrack.getValue()){
                for (int i=0; i<definedRoutingProfiles.size(); i++){
                    RoutingProfile routingProfile = definedRoutingProfiles.get(i);
                    ExtendedTextView etvRoutingProfile = profileETVs.get(i); // number should correspond
                    if (prefCache.get(routingProfile.getId(), false).getValue()){ // all entries are already in cache, so default is irrelevant
                         parent.addView(etvRoutingProfile);
                    }
                }
            }
            getActivity().findViewById(R.id.routingProfiles).setVisibility((prefUseRoutingProfiles.getValue() && prefEditMarkerTrack.getValue())?View.VISIBLE:View.INVISIBLE);
        });
        definedRoutingProfiles = new ArrayList<>();
        addDefinedRoutingProfile(prefCache, new ShortestDistance(), true);
        addDefinedRoutingProfile(prefCache, new MTB_K1S1(), true);
        addDefinedRoutingProfile(prefCache, new MTB_K1S2(), false);
        addDefinedRoutingProfile(prefCache, new MTB_K1S3(), false);
        addDefinedRoutingProfile(prefCache, new MTB_K2S1(), false);
        addDefinedRoutingProfile(prefCache, new MTB_K2S2(), true);
        addDefinedRoutingProfile(prefCache, new MTB_K2S3(), false);
        addDefinedRoutingProfile(prefCache, new MTB_K3S1(), false);
        addDefinedRoutingProfile(prefCache, new MTB_K3S2(), false);
        addDefinedRoutingProfile(prefCache, new MTB_K3S3(), true);
        addDefinedRoutingProfile(prefCache, new TrekkingBike(), true);

        prefRoutingProfileId.addObserver(evt -> {
            String id = prefRoutingProfileId.getValue();
            boolean profileSet = false;
            for (RoutingProfile routingProfile : definedRoutingProfiles){
                if (routingProfile.getId().equals(id)){
                    getTimer().postDelayed(() -> {
                        prefCache.get(routingProfile.getId(),false).setValue(true); // whatever the visibility was, set it to true
                        if (routingEngine.setRoutingProfile(routingProfile)){
                            application.markerTrackLogObservable.changed();
                        }
                    },1);
                    profileSet = true;
                    break;
                }
            }
            if (! profileSet){ //
                prefRoutingProfileId.setValue(defaultRoutingProfileId);
            }
        } );
        prefCalcRouteInProgress.setValue(false);
        prefRoutingProfileId.changed();
        application.routeTrackLogObservable.addObserver((e) -> {
            TrackLog rotl = application.routeTrackLogObservable.getTrackLog();
            prefRouteSavable.setValue((rotl != null) && rotl.isModified() );
        });
    }

    private void addDefinedRoutingProfile(PrefCache prefCache, RoutingProfile routingProfile, boolean defaultVisibility){
        definedRoutingProfiles.add(routingProfile);
        prefCache.get(routingProfile.getId(), defaultVisibility);
    }

    public ArrayList<GGraphTile> getGGraphTileList(BBox bBox) {
        return routingEngine.getGGraphTileList(bBox);
    }

    @Override
    public ViewGroup initDashboard(ViewGroup dvg, String info) {
        super.initDashboard(dvg,info);
        getControlView().setViewGroupColors(dvg, R.color.CC_WHITE, R.color.CC_PURPLE_A100);
        dashboardRoute = dvg;
        return dvg;
    }

    public ExtendedTextView initRoutingProfile(ExtendedTextView etv, RoutingProfile routingProfile){
        super.initQuickControl(etv,routingProfile.getId());
        routingProfile.initETV(etv,prefRoutingProfileId,prefCalcRouteInProgress);
        profileETVs.add(etv);
        return etv;
    }

    @Override
    public LabeledSlider initLabeledSlider(LabeledSlider lsl, String info) {
        if ("rotl".equals(info)) {
            lsl.initPrefData(prefMtlVisibility, prefAlphaRotl, CC.getColor(R.color.CC_PURPLE), "RouteTrackLog");
        }
        return lsl;
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info) {
        super.initQuickControl(etv,info);
        if ("matching".equals(info)) {
            etv.setPrAction(prefMapMatching);
            etv.setData(R.drawable.matching);
            etv.setDisabledData(prefStlVisibility,R.drawable.matching_dis);
            etv.setHelp(r(R.string.FSRouting_qcMapMatching_Help));
        } else if ("routingHint".equals(info)){
            etv.setData(prefRoutingHints,R.drawable.routing_hints2, R.drawable.routing_hints1);
            etv.setPrAction(prefRoutingHints);
            etv.setDisabledData(prefRoutingHintsEnabled, R.drawable.routing_hints_dis);
            etv.setHelp(r(R.string.FSRouting_qcRoutingHint_Help)).setHelp(r(R.string.FSRouting_qcRoutingHint_Help1),r(R.string.FSRouting_qcRoutingHint_Help2));
        } else if ("routingSave".equals(info)){
            etv.setData(R.drawable.save);
            etv.setOnClickListener(v -> {
                TrackLog trackLog = application.routeTrackLogObservable.getTrackLog();
                mgLog.i("save "+trackLog.getName());
                GpxExporter.export(application.getPersistenceManager(), trackLog);
                application.getMetaDataUtil().createMetaData(trackLog);
            });
            etv.setDisabledData(prefRouteSavable, R.drawable.save2);
            etv.setHelp("save marker track with route");
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
        prefRoutingProfileId.deleteObservers();
    }

    @Override
    protected void doRefreshResumedUI() {
        unregisterAll();
        WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
        WriteableTrackLog rotl = application.routeTrackLogObservable.getTrackLog();

        if (dndVisualisationLayer != null){
            if (checkMtlpMovement( dndVisualisationLayer.getModel().get(0), false )){
                register(dndVisualisationLayer);
            }
        }
        showTrack(rotl,prefRouteGL,PAINT_STROKE_GL,PAINT_ROUTE_STROKE, prefAlphaRotl.getValue(), 0);
        if (mtl != null){
            showTrack(mtl, CC.getAlphaCloneFill(PAINT_ROUTE_STROKE2, prefAlphaRotl.getValue()) , false,  (int)(DisplayModel.getDeviceScaleFactor()*6.0f), true);
        }
        getControlView().setDashboardValue(prefMtlVisibility.getValue(), dashboardRoute, calcRemainingStatistic(rotl));

        checkRelaxedViews(mtl);
    }

    private void updateRouting(){
        WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
        WriteableTrackLog rotl = null;
        if ((mtl != null) && (mtl.getTrackStatistic().getNumPoints() > 0)){
            mgLog.d("Start");
            long tStart = System.currentTimeMillis();
            rotl = routingEngine.updateRouting2(mtl, application.routeTrackLogObservable.getTrackLog());
//            mtl.setRoutingProfileId(prefRoutingProfileId.getValue());
            mgLog.d("End duration="+(System.currentTimeMillis()-tStart)+"ms");
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
                synchronized (routingEngine){
                    for (PointModel pm : routingEngine.getCurrentRelaxedNodes()){
                        register( new PointView(pm, ((GNode)pm).getNodeRef().isSetteled()?PAINT_RELAXED:PAINT_RELAXED2 ));
                    }
                }
            }
        }
    }


    void optimize(){ // needs to be reworked
        synchronized (routingEngine){
            prefCalcRouteInProgress.setValue(true);
            routingEngine.setRoutingContext( new RoutingContext(1000, false, 10, PointModelUtil.getCloseThreshold()) );
            WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
            RouteOptimizer ro = new RouteOptimizer(gFactory, routingEngine);
            ro.optimize(mtl);
            routingEngine.setRoutingContext( new RoutingContext(1000, true, 3, PointModelUtil.getCloseThreshold()) );
            updateRouting();
            routingEngine.setRoutingContext(interactiveRoutingContext);
            prefCalcRouteInProgress.setValue(false);
        }
    }

    public void optimize2(TrackLog trackLog){
        synchronized (routingEngine){
            prefCalcRouteInProgress.setValue(true);
            routingEngine.setRoutingContext( new RoutingContext(10000, true, 3, PointModelUtil.getCloseThreshold()) );
            RouteOptimizer2 ro = new RouteOptimizer2(gFactory, routingEngine);
            ro.optimize(trackLog);
            routingEngine.setRoutingContext(interactiveRoutingContext);
            prefCalcRouteInProgress.setValue(false);
        }
    }

    public class AdvancedMtlSupportProvider implements FSMarker.MtlSupportProvider{
        @Override
        public TrackLogRefApproach getBestDistance(WriteableTrackLog mtl, PointModel pm, double threshold) {
            return getRoutingLineApproach(pm, threshold);
        }

        @Override
        public void optimizePosition(WriteablePointModel wpm, double threshold) {
            mgLog.d("pos="+wpm+" threshold="+threshold);
            TreeSet<ApproachModel> approaches = routingEngine.calcApproaches(wpm, (int)threshold);
            if (approaches.size() > 0){
                GNode pos = approaches.first().getApproachNode();
                mgLog.i("optimize Pos "+wpm+" to "+pos +String.format(Locale.ENGLISH," dist=%.1fm",pos.getNeighbour().getCost()));
                wpm.setLat(pos.getLat());
                wpm.setLon(pos.getLon());
            } else {
                mgLog.i("optimize Pos "+wpm + " no approach");
            }
        }

        // if rpm not yet exists - create dummy one - needed for MTLP dnd visualisation
        @Override
        public void pointAddedCallback(PointModel pm) {
            RoutePointModel rpm = routingEngine.getRoutePointMap().get(pm);
            if (rpm == null){
                rpm = new RoutePointModel(pm);
                rpm.currentMPM = new MultiPointModelImpl().addPoint(new PointModelImpl(pm));
                routingEngine.getRoutePointMap().put(pm, rpm);
            }
        }

        // check for dnd visualisation
        @Override
        public void pointMovedCallback(PointModel pm) {
            checkMtlpMovement(pm, true);
        }

        @Override
        public void pointDeletedCallback(PointModel pm) {
            if (dndVisualisationLayer != null){
                dndVisualisationLayer = null;
            }
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
                if (mtlp != null){ // due to concurrent routing update actions the reference from the rtlpm to the mtlp might have failed
                    WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
                    TrackLogRefApproach mtlApproach = mtl.getBestPoint(mtlp, 1);
                    mtlApproach.setApproachPoint(bestMatch.getApproachPoint()); // take the approach point from the routing line
                    return mtlApproach;
                }
            }
        }
        return null;
    }

    private boolean checkMtlpMovement(PointModel mtlp, boolean triggerRefresh){
        dndVisualisationLayer = null;
        WriteablePointModelImpl wpm = new WriteablePointModelImpl();
        if (checkMtlpMovementPoint(mtlp, wpm)){
            MultiPointModelImpl mpmi = new MultiPointModelImpl();
            mpmi.addPoint(mtlp).addPoint(wpm);
            Paint paint = CC.getStrokePaint(R.color.CC_BLACK_A150, 3);
            dndVisualisationLayer = new MultiPointView(mpmi, paint);
            if (triggerRefresh){
                long saveTTRefreshTime = ttRefreshTime;
                ttRefreshTime = 1; // need quick refresh
                refreshObserver.onChange();
                ttRefreshTime = saveTTRefreshTime;
            }
        }
        return (dndVisualisationLayer != null);
    }

    public boolean checkMtlpMovementPoint(PointModel mtlp, WriteablePointModel currentRoutingPos){
        RoutePointModel rpm = routingEngine.getRoutePointMap().get(mtlp);
        if ((rpm != null) && (rpm.currentMPM != null)){
            PointModel last = rpm.currentMPM.get(rpm.currentMPM.size() - 1);
            if ((last != null) && (mtlp != null)){
                if (PointModelUtil.distance(last, mtlp) > getMapViewUtility().getCloseThresholdForZoomLevel()/3){
                    currentRoutingPos.setLat(last.getLat());
                    currentRoutingPos.setLon(last.getLon());
                    return true;
                }
            }
        }
        return false;
    }

    public void prepareOptimize(){
        prefRoutingProfileId.setValue(defaultRoutingProfileId);
        prefEditMarkerTrack.setValue(true);
    }
}
