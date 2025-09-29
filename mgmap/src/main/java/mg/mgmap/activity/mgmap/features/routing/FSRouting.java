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

import android.content.ComponentCallbacks2;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.map.model.DisplayModel;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.features.routing.profile.Hiking;
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
import mg.mgmap.activity.mgmap.view.MultiMultiPointView;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.marker.FSMarker;
import mg.mgmap.generic.graph.GraphFactory;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.model.MultiPointModel;
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
    private final Paint PAINT_STROKE_GL = CC.getStrokePaint(R.color.CC_GRAY100_A100, getMapViewUtility().getTrackWidth()*1.4f);

    private static final int ZOOM_LEVEL_RELAXED_VISIBILITY = 16;

    private static ArrayList<RoutingProfile> definedRoutingProfiles = null;

    public static ArrayList<RoutingProfile> getDefinedRoutingProfiles(){
        return definedRoutingProfiles;
    }

    private final RoutingEngine routingEngine;
    private final GraphFactory gFactory;
    private final RoutingContext interactiveRoutingContext = new RoutingContext(
            1000000, // no limit
            false, // no extra snap, since FSMarker snaps point zoom level dependent
            20, // accept long detours in interactive mode
            10); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent
                 // 30.11.24: changed to 10 - new maps may contain slight changes, adopt routes to current map
                 // but still do not snap - so it's possible to recognize this

    private final FSMarker.MtlSupportProvider mtlSupportProvider;

    private final Pref<Boolean> prefWayDetails = getPref(R.string.FSGrad_pref_WayDetails_key, false);
    private final Pref<Boolean> prefGps = getPref(R.string.FSPosition_pref_GpsOn, false);
    private final Pref<Boolean> prefRouteGL = getPref(R.string.FSRouting_pref_RouteGL, false);

    private final Pref<Float> prefAlphaRotl = getPref(R.string.FSRouting_pref_alphaRoTL, 1.0f);
    private final Pref<Boolean> prefMtlVisibility = getPref(R.string.FSMarker_pref_MTL_visibility, false);
    private final Pref<Integer> prefZoomLevel = getPref(R.string.FSBeeline_pref_ZoomLevel, 15);
    private final Pref<Boolean> prefRoutingHints = getPref(R.string.FSRouting_qc_RoutingHint, false);
    private final Pref<Boolean> prefRoutingHintsEnabled = new Pref<>(false);
    private final Pref<Boolean> prefEditMarkerTrack =  getPref(R.string.FSMarker_qc_EditMarkerTrack, false);
    private final String defaultRoutingProfileId = RoutingProfile.constructId(ShortestDistance.class);
    private final Pref<String> prefRoutingProfileId = getPref(R.string.FSRouting_pref_currentRoutingProfile, defaultRoutingProfileId);
    private final Pref<Boolean> prefCalcRouteInProgress = getPref(R.string.FSRouting_pref_calcRouteInProgress, false);
    private final ArrayList<ExtendedTextView> profileETVs = new ArrayList<>();
    private final Pref<Boolean> prefRouteSavable = new Pref<>(false); // when MTL is changed
    private MultiPointView routingIntermediateMPV = null;


    private ViewGroup dashboardRoute = null;
    private boolean runRouteCalcThread = true;
    private final MGMapApplication application;
    private MultiPointView dndVisualisationLayer = null;
    private final PrefCache prefCache;
    private final Thread routingThread;

    public FSRouting(MGMapActivity mgActivity, FSMarker fsMarker, GraphFactory gFactory) {
        super(mgActivity);
        this.gFactory = gFactory;
        application = getApplication();
        routingEngine = new RoutingEngine(gFactory, interactiveRoutingContext, application.routeIntermediatesObservable);
        ttRefreshTime = 50;
        mtlSupportProvider = new AdvancedMtlSupportProvider();
        fsMarker.mtlSupportProvider = mtlSupportProvider;

        application.markerTrackLogObservable.addObserver((e) -> {
            TrackLog mtl = application.markerTrackLogObservable.getTrackLog();
            if (mtl != null) {
                if (mtl.getRoutingProfileId() == null) {
                    mtl.setRoutingProfileId(prefRoutingProfileId.getValue());
                } else { // mtl.getRoutingProfileId() != null
                    if (!prefRoutingProfileId.getValue().equals(mtl.getRoutingProfileId())) { // mtl has a different profile than current setting
                        mgLog.d("mtl doesn't match prefRoutingProfileId: mtl=" + mtl.getRoutingProfileId() + " prefRoutingProfileId=" + prefRoutingProfileId.getValue());
                        if (isRoutingProfileIdValid(mtl.getRoutingProfileId())) {              // and mtl profile is valid -> so take it
                            mgLog.d("set prefRoutingProfileId to mtl value: " + mtl.getRoutingProfileId());
                            prefRoutingProfileId.setValue(mtl.getRoutingProfileId());
                        }
                    }
                }
            }
            routingEngine.refreshRequired.incrementAndGet(); // refresh route calculation is required
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

        Pref<Boolean> prefUseRoutingProfiles = getPref(R.string.preferences_routingProfile_key, true);
        prefUseRoutingProfiles.addObserver(evt -> {
            mgLog.d("reset to defaultRoutingProfileId");
            prefRoutingProfileId.setValue(defaultRoutingProfileId);
        });
        prefCache = getActivity().getPrefCache();
        prefEditMarkerTrack.addObserver(evt -> {
            ViewGroup parent = activity.findViewById(R.id.routingProfiles);
            parent.removeAllViews();
            boolean shouBeVisible = prefUseRoutingProfiles.getValue() && prefEditMarkerTrack.getValue();
            if (shouBeVisible){
                for (int i=0; i<definedRoutingProfiles.size(); i++){
                    RoutingProfile routingProfile = definedRoutingProfiles.get(i);
                    ExtendedTextView etvRoutingProfile = profileETVs.get(i); // number should correspond
                    if (prefCache.get(routingProfile.getId(), false).getValue()){ // all entries are already in cache, so default is irrelevant
                         parent.addView(etvRoutingProfile);
                    }
                }
            }
            getActivity().findViewById(R.id.routingProfiles).setVisibility(shouBeVisible?View.VISIBLE:View.INVISIBLE);
        });
        definedRoutingProfiles = new ArrayList<>();
        addDefinedRoutingProfile(prefCache, new ShortestDistance(), true);
        addDefinedRoutingProfile(prefCache, new Hiking(), true);
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
            RoutingProfile routingProfile = getRoutingProfile(id);
            if (routingProfile != null){
                new Thread(()->routingEngine.setRoutingProfile(routingProfile)).start();
            } else {
                prefRoutingProfileId.setValue(defaultRoutingProfileId);
            }
        } );
        prefCalcRouteInProgress.setValue(false);
        prefCalcRouteInProgress.addObserver((e)-> getActivity().runOnUiThread(() -> {
            if (prefCalcRouteInProgress.getValue()) {
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }));

        prefRoutingProfileId.changed();
        application.routeTrackLogObservable.addObserver((e) -> {
            TrackLog rotl = application.routeTrackLogObservable.getTrackLog();
            prefRouteSavable.setValue((rotl != null) && rotl.isModified() );
        });

        application.routeIntermediatesObservable.addObserver(e -> {
            unregister(routingIntermediateMPV);
            if (e.getNewValue() instanceof ArrayList<?> routeIntermediates){
                //noinspection unchecked
                routingIntermediateMPV = new MultiMultiPointView((ArrayList<MultiPointModel>) routeIntermediates, PAINT_ROUTE_STROKE);
                register(routingIntermediateMPV);
            } else {
                routingIntermediateMPV = null;
            }
        });

        routingThread = createRoutingThread();
    }

    @NonNull
    private Thread createRoutingThread() {
        Thread routingThread = new Thread(() -> {
            AtomicInteger refreshRequired = routingEngine.refreshRequired;
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
                        if (refreshRequired.get() != 0){
                            mgLog.d("reset refreshRequired from "+refreshRequired.get()+" to 0");
                            refreshRequired.set(0);
                        }
                        if (dndVisualisationLayer != null){
                            dndVisualisationLayer = null;
                            doRefresh();
                        }
                    }

                } catch (InterruptedException e) {
                    mgLog.e(e);
                }
            }
            mgLog.d("routeCalcThread terminating");
        });
        routingThread.setName("routing");
        return routingThread;
    }

    private void addDefinedRoutingProfile(PrefCache prefCache, RoutingProfile routingProfile, boolean defaultVisibility){
        definedRoutingProfiles.add(routingProfile);
        prefCache.get(routingProfile.getId(), defaultVisibility);
    }

    private RoutingProfile getRoutingProfile(String routingProfileId) {
        for (RoutingProfile routingProfile : definedRoutingProfiles) {
            if (routingProfile.getId().equals(routingProfileId)) {
                return routingProfile;
            }
        }
        return null;
    }

    private boolean isRoutingProfileIdValid(String routingProfileId){
        return getRoutingProfile(routingProfileId) != null;
    }

    public String getDefaultRoutingProfileId(){
        return defaultRoutingProfileId;
    }

    @Override
    public ViewGroup initDashboard(ViewGroup dvg, String info) {
        super.initDashboard(dvg,info);
        getControlView().setViewGroupColors(dvg, R.color.CC_WHITE, R.color.CC_PURPLE_A100);
        dashboardRoute = dvg;
        return dvg;
    }

    public ExtendedTextView initRoutingProfile(ExtendedTextView etv, RoutingProfile routingProfile){
        String id = routingProfile.getId();
        super.initQuickControl(etv,id);

        Pref<Boolean> rpState = new Pref<>(id.equals(prefRoutingProfileId.getValue()));
        prefRoutingProfileId.addObserver(evt -> rpState.setValue( id.equals(prefRoutingProfileId.getValue()) ));
        etv.setData(rpState, prefCalcRouteInProgress,
                routingProfile.getIconIdInactive(), routingProfile.getIconIdActive(), routingProfile.getIconIdInactive(), routingProfile.getIconIdCalculating());
        etv.setOnClickListener(v -> {
            if (!id.equals(prefRoutingProfileId.getValue())){
                prefRoutingProfileId.setValue(id);
                WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
                if (mtl != null) {
                    WriteableTrackLog newMtl = mtl.cloneTrackLog(false);
                    newMtl.setRoutingProfileId(id);
                    application.markerTrackLogObservable.setTrackLog(newMtl);
                }
            } else {
                if (prefCalcRouteInProgress.getValue()){
                    routingEngine.refreshRequired.set(-1000);
                } else {
                    routingEngine.refreshRequired.incrementAndGet();
                }
            }
        });
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
            etv.setData(R.drawable.matching);
            etv.setDisabledData(new Pref<>(false),R.drawable.matching_dis);
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
                getApplication().getMetaDataUtil().writeMetaData(getApplication().getPersistenceManager().openMetaOutput(trackLog.getName()), trackLog);
            });
            etv.setDisabledData(prefRouteSavable, R.drawable.save2);
            etv.setHelp("save marker track with route");
        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (routingThread.getState() == Thread.State.NEW){
            routingThread.start(); // don't start earlier, otherwise concurrent modification exception on observers of prefCalcRouteInProgress may occur (observers are added during ControlComposers work)
        }
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
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        if (level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL){
            if (prefCalcRouteInProgress.getValue()){
                TrackLog mtl = application.markerTrackLogObservable.getTrackLog();
                if (mtl != null){
                    routingEngine.refreshRequired.set(-1000);
                    mgLog.w("abort routing due to onTrimMemory callback with TRIM_MEMORY_RUNNING_CRITICAL");
                }
            }
            gFactory.clearCache(); // clear GGraphTileFactory cache
            application.getHgtProvider().clearCache(); // clear HgtCache
        }
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
            showTrack(mtl, CC.getAlphaCloneFill(PAINT_ROUTE_STROKE2, prefAlphaRotl.getValue()) , false,  6, true);
        }
        if (getPref(R.string.preferences_display_show_km_key, true).getValue()){
            showTrack(rotl,CC.getAlphaCloneFill(PAINT_ROUTE_STROKE2, prefAlphaRotl.getValue()),false, 3, true, true);
        }
        getControlView().setDashboardValue(prefMtlVisibility.getValue(), dashboardRoute, calcRemainingStatistic(rotl));

        checkRelaxedViews(mtl);
    }

    private void updateRouting(){
        WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
        WriteableTrackLog rotl = null;
        routingEngine.checkRoutingProfileMonitor();
        if ((mtl != null) && (mtl.getTrackStatistic().getNumPoints() > 0)){
            mgLog.d("Start");
            long tStart = System.currentTimeMillis();
            rotl = routingEngine.updateRouting2(mtl, application.routeTrackLogObservable.getTrackLog());
            application.getMetaDataUtil().createMetaData(rotl);
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
                MultiPointModelImpl mpm = new MultiPointModelImpl();
                for (PointModel pm : new ArrayList<>( routingEngine.getCurrentRelaxedNodes() )){ // use a copy of the list to iterate over, since a synchronized access would block the UI thread
                    mpm.addPoint(pm);
                    register( new PointView(pm, PAINT_RELAXED ));
                }
                MultiPointView mpv = new MultiPointView(mpm, PAINT_RELAXED);
                mpv.setShowIntermediates(true);
                mpv.setShowPointsOnly(true);
                register(mpv);
            }
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
            // 29.05.25 Due to the increase of interactiveRoutingContext.approachLimit from 1 to 10, this limit can be higher than closeThresholdForZoomLevel
            // In this case, the point doesn't snap to the way, but the route is drawn on the way - which feels wrong. So now use interactiveRoutingContext.approachLimit
            // as a lower boundary for closeThreshold.
            int closeThreshold = Math.max(interactiveRoutingContext.approachLimit, (int) threshold);
            mgLog.d("pos="+wpm+" threshold="+closeThreshold);
            ApproachModel approachModel = gFactory.calcApproach(wpm, closeThreshold);
            if (approachModel != null){
                PointModel pos = approachModel.getApproachNode();
                mgLog.i("optimize Pos "+wpm+" to "+pos +String.format(Locale.ENGLISH," dist=%.1fm", approachModel.getApproachDistance() ));
                wpm.setLat(pos.getLat());
                wpm.setLon(pos.getLon());
                wpm.setEle(pos.getEle());
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
                    if (mtlApproach != null){ // might be null due to concurrent change of mtl
                        mtlApproach.setApproachPoint(bestMatch.getApproachPoint()); // take the approach point from the routing line
                        return mtlApproach;
                    }
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
