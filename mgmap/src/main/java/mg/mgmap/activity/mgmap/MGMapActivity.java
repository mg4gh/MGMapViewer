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
package mg.mgmap.activity.mgmap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;


import androidx.annotation.NonNull;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;

import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.download.TileDownloadLayer;

import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;

import mg.mgmap.activity.settings.MainPreferenceScreen;
import mg.mgmap.activity.settings.MapLayersPreferenceScreen;
import mg.mgmap.activity.settings.SettingsActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.atl.FSAvailableTrackLogs;
import mg.mgmap.activity.mgmap.features.bb.FSBB;
import mg.mgmap.activity.mgmap.features.control.FSControl;
import mg.mgmap.activity.mgmap.features.alpha.FSAlpha;
import mg.mgmap.activity.mgmap.features.grad.FSGraphDetails;
import mg.mgmap.activity.mgmap.features.marker.FSMarker;
import mg.mgmap.activity.mgmap.features.beeline.FSBeeline;
import mg.mgmap.activity.mgmap.features.position.FSPosition;
import mg.mgmap.activity.mgmap.features.remainings.FSRemainings;
import mg.mgmap.activity.mgmap.features.routing.FSRouting;
import mg.mgmap.activity.mgmap.features.rtl.FSRecordingTrackLog;
import mg.mgmap.activity.mgmap.features.search.FSSearch;
import mg.mgmap.activity.mgmap.features.time.FSTime;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.graph.GGraphTileFactory;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.TrackLogRef;
import mg.mgmap.generic.model.TrackLogRefApproach;
import mg.mgmap.generic.model.TrackLogRefZoom;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.activity.mgmap.util.CC;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.BgJobGroup;
import mg.mgmap.generic.util.BgJobGroupCallback;
import mg.mgmap.generic.util.FullscreenUtil;
import mg.mgmap.generic.util.SshSyncUtil;
import mg.mgmap.generic.util.gpx.GpxImporter;
import mg.mgmap.activity.mgmap.util.MapDataStoreUtil;
import mg.mgmap.activity.mgmap.util.MapViewUtility;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.activity.mgmap.util.OpenAndroMapsUtil;
import mg.mgmap.activity.mgmap.util.Permissions;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.TopExceptionHandler;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.activity.mgmap.view.MVLayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * The main activity of the MgMapViewer. It is based on the mapsforge MapView and provides track logging
 * and modification functionality.
 */
public class MGMapActivity extends MapViewerBase implements XmlRenderThemeMenuCallback {

    /** Reference to the MGMapApplication object */
    MGMapApplication application = null;
    /** Reference to the control view object.
     *  @see <a href="../../../images/MGMapViewer_ViewModel.PNG">MGMapActivity_ViewModel</a> */
    ControlView coView = null;

    ArrayList<FeatureService> featureServices = new ArrayList<>();

    /** Reference to the renderThemeStyleMenu - will be set due to the callback getCategories
     * form XmlRenderThemeMenuCallback after reading the themes xml file */
    protected XmlRenderThemeStyleMenu renderThemeStyleMenu;

    MGMapLayerFactory mapLayerFactory = null;
    /** Reference to the MapViewUtility - provides so services around the MapView object */
    MapViewUtility mapViewUtility = null;

    private PrefCache prefCache;

    private MapDataStoreUtil mapDataStoreUtil = null;
    private GGraphTileFactory gGraphTileFactory = null;

    public MGMapApplication getMGMapApplication(){
        return application;
    }
    public MGMapLayerFactory getMapLayerFactory(){
        return mapLayerFactory;
    }
    public ControlView getControlView(){
        return findViewById(R.id.controlView);
    }

    SharedPreferences getSharedPreferences(){
        return sharedPreferences;
    }
    public PrefCache getPrefCache(){
        return prefCache;
    }
    public GGraphTileFactory getGGraphTileFactory() {
        return gGraphTileFactory;
    }
    public MapDataStoreUtil getMapDataStoreUtil() {
        return mapDataStoreUtil;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(MGMapApplication.LABEL, NameUtil.context());
        application = (MGMapApplication) getApplication();
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(application.getPersistenceManager()));
        //for fullscreen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        createSharedPreferences();
        if (Build.VERSION.SDK_INT >= 27){
            setShowWhenLocked(true);
        }
        setContentView(R.layout.mgmapactivity);

        CC.setActivity(this);
        PointModelUtil.init(getResources().getInteger(R.integer.CLOSE_THRESHOLD));

        mapLayerFactory = new MGMapLayerFactory(this);

        prefCache = new PrefCache(this);

        initMapView();
        createLayers();

        mapDataStoreUtil = new MapDataStoreUtil().onCreate(mapLayerFactory, sharedPreferences);
        initializePosition(mapView.getModel().mapViewPosition);
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" Tilesize initial " + this.mapView.getModel().displayModel.getTileSize());

        // don't change orientation when device is rotated
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        coView = getControlView();
        mapViewUtility = new MapViewUtility(this, mapView);
        gGraphTileFactory = new GGraphTileFactory().onCreate(mapDataStoreUtil, application.getElevationProvider());

        featureServices.add(new FSTime(this));
        featureServices.add(new FSBeeline(this));
        featureServices.add(new FSPosition(this));
        featureServices.add(new FSRecordingTrackLog(this));
        featureServices.add(new FSAvailableTrackLogs(this));
        featureServices.add(new FSMarker(this));
        featureServices.add(new FSRouting(this, getFS(FSMarker.class)));

        featureServices.add(new FSRemainings(this));
        featureServices.add(new FSBB(this));
        featureServices.add(new FSGraphDetails(this));
        featureServices.add(new FSSearch(this));
        featureServices.add(new FSAlpha(this));
        featureServices.add(new FSControl(this));

        try{
            Thread.sleep(100);
        }catch(Exception e){
            Log.w(MGMapApplication.LABEL, NameUtil.context());
        }
        coView.init(application, this);
        onNewIntent(getIntent());
        // use prefGps and prefRestart from applications prefCache to prevent race conditions during startup phase
        application.prefGps.addObserver((o, arg) -> triggerTrackLoggerService());
        Log.d(MGMapApplication.LABEL, NameUtil.context()+" prefGps="+application.prefGps.getValue()+" prefRestart="+application.prefRestart.getValue() );
        if (application.prefGps.getValue() && application.prefRestart.getValue()){
            triggerTrackLoggerService(); // restart track logger service after app restart while track recording is on
        }
        application.prefRestart.setValue(false);
        prefCache.get(R.string.preferences_ssh_uploadGpxTrigger, false).addObserver((o, arg) -> new SshSyncUtil().trySynchronisation(application));
        prefCache.dumpPrefs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(MGMapApplication.LABEL, NameUtil.context());
        application = (MGMapApplication) getApplication();

        // This is a workaround for frozen Screen after 2 times switch off/on with setShowWhenLocked(true);
        // Hint found at: https://stackoverflow.com/questions/55462980/android-9-frozen-ui-after-unlocking-screen
        if ((Build.VERSION.SDK_INT >= 27) && (Build.VERSION.SDK_INT <= 28)){
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("");
            alertDialog.create();
            ColorDrawable dialogColor = new ColorDrawable(Color.GRAY);
            dialogColor.setAlpha(0);
            alertDialog.getWindow().setBackgroundDrawable(dialogColor);
            alertDialog.show();
            new Thread(() -> {
                try {
                    Thread.sleep(3);
                } catch (Exception e){ Log.e(MGMapApplication.LABEL, NameUtil.context()+" "+e.getMessage()); }
                alertDialog.dismiss();
            }).start();
        }

        for (FeatureService microService : featureServices) {
            try {
                Log.d(MGMapApplication.LABEL, NameUtil.context()+" onResume " + microService + " beginning ");
                microService.onResume();
            } catch (Exception e) {
                Log.w(MGMapApplication.LABEL, NameUtil.context()+" onResume " + microService + " failed: " + e.getMessage(), e);
            }
        }
        for (Layer layer : mapView.getLayerManager().getLayers()){
            if (layer instanceof TileDownloadLayer) {
                TileDownloadLayer tileDownloadLayer = (TileDownloadLayer) layer;
                tileDownloadLayer.onResume();
            }
        }

        application.recordingTrackLogObservable.changed();
        application.availableTrackLogsObservable.changed();
        application.lastPositionsObservable.changed();
        application.markerTrackLogObservable.changed();

        new Handler().postDelayed(() -> prefCache.get(R.string.preferences_ssh_uploadGpxTrigger, false).toggle(), 60*1000);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus){
            FullscreenUtil.enforceState(this);
            Log.i(MGMapApplication.LABEL, NameUtil.context());
        }
    }

    @Override
    protected void onPause() {
        Log.i(MGMapApplication.LABEL, NameUtil.context());
        for (int i = featureServices.size() - 1; i >= 0; i--) { // reverse order
            FeatureService microService = featureServices.get(i);
            try {
                microService.onPause();
            } catch (Exception e) {
                Log.w(MGMapApplication.LABEL, NameUtil.context()+" onPause " + microService + " failed: " + e.getMessage());
            }
        }
        for (Layer layer : mapView.getLayerManager().getLayers()){
            if (layer instanceof TileDownloadLayer) {
                TileDownloadLayer tileDownloadLayer = (TileDownloadLayer) layer;
                tileDownloadLayer.onPause();
            }
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.w(MGMapApplication.LABEL, NameUtil.context() + " Destroy started");
        for (int i = featureServices.size() - 1; i >= 0; i--) { // reverse order
            FeatureService microService = featureServices.get(i);
            try {
                microService.onDestroy();
            } catch (Exception e) {
                Log.w(MGMapApplication.LABEL, NameUtil.context()+" stop " + microService + " failed: " + e.getMessage());
            }
        }
        application.availableTrackLogsObservable.deleteObservers();
        application.recordingTrackLogObservable.deleteObservers();
        application.markerTrackLogObservable.deleteObservers();
        application.routeTrackLogObservable.deleteObservers();
        application.lastPositionsObservable.deleteObservers();

        mapView.destroyAll();
        mapDataStoreUtil.onDestroy();
        gGraphTileFactory.onDestroy();
        prefCache.cleanup();
        AndroidGraphicFactory.clearResourceMemoryCache(); // do this as very last action - otherwise some crash might occur
        Log.w(MGMapApplication.LABEL, NameUtil.context() + " Destroy finished");
        super.onDestroy();
    }

    /** Return the feature service by type  */
    @SuppressWarnings("unchecked")
    public <T> T getFS(Class<T> tClass){
        for (FeatureService service : featureServices){
            if (tClass.isInstance(service)) return (T)service;
        }
        return null;
    }


    @Override
    protected void createSharedPreferences() {
        super.createSharedPreferences();
        String prefLang = sharedPreferences.getString(getResources().getString(R.string.preferences_language_key), "de");
        sharedPreferences.edit().putString(getResources().getString(R.string.preferences_language_key), prefLang).apply();

        Log.i(MGMapApplication.LABEL, NameUtil.context() + " Device scale factor " + DisplayModel.getDeviceScaleFactor());
        Log.i(MGMapApplication.LABEL, NameUtil.context() + " Device screen size " + getResources().getDisplayMetrics().widthPixels + "x" + getResources().getDisplayMetrics().heightPixels);
        float fs = Float.parseFloat(sharedPreferences.getString(getResources().getString(R.string.preferences_scale_key), Float.toString(DisplayModel.getDefaultUserScaleFactor())));
        Log.i(MGMapApplication.LABEL, NameUtil.context() + " User ScaleFactor " + fs);
        if (fs != DisplayModel.getDefaultUserScaleFactor()) {
            DisplayModel.setDefaultUserScaleFactor(fs);
        }
    }

    /** Used if
     * <ul>
     *     <li>Map download is triggered from openandromaps page (via scheme "mf-v4-map")</li>
     *     <li>Theme download is triggered from openandromaps page (via scheme "mf-theme")</li>
     *     <li>one or multiple tracks are shown from statistic view</li>
     *     <li>a new marker tracks is set from statistic view</li>
     *     <li>a .gpx file is opened from mail oder file manager</li>
     * </ul>
     * */
    @Override
    protected void onNewIntent(Intent intent) {
        try {
            super.onNewIntent(intent);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+"  " + intent);
            if (intent != null) {
                String paramKey = getResources().getString(R.string.activity_param_key);
                if (intent.hasExtra(paramKey)){
                    String value = intent.getStringExtra(paramKey);
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+paramKey+" "+value );
                    prefCache.get(R.string.activity_param_key, "").setValue(value);
                }

                if (intent.getType() == null){
                    Uri uri = intent.getData();
                    if (uri != null) {
                        Log.i(MGMapApplication.LABEL, NameUtil.context() + " uri: " + uri);
                        PersistenceManager pm = application.getPersistenceManager();
                        if (uri.getScheme().equals("mf-v4-map")){
                            BgJobGroup bgJobGroup = new BgJobGroup(application, this, "Download map", new BgJobGroupCallback() {
                                @Override
                                public void afterGroupFinished(BgJobGroup jobGroup, int total, int success, int fail) {
                                    if (success > 0){
                                        Intent intent = new Intent(MGMapActivity.this, SettingsActivity.class);
                                        intent.putExtra("FSControl.info", MapLayersPreferenceScreen.class.getName());
                                        startActivity(intent);
                                    }
                                }
                            }){
                                @Override
                                public String getResultDetails() {
                                    if (successCounter > 0){
                                        return super.getDetails() + " finished successful.\n\n Now you can assign this map to a layer";
                                    } else {
                                        return super.getResultDetails();
                                    }
                                }
                            };
                            String sUrl = uri.toString().replaceFirst("mf-v4-map", "https");
                            bgJobGroup.addJob(OpenAndroMapsUtil.createBgJobsFromIntentUriMap(pm, new URL(sUrl)));
                            bgJobGroup.setConstructed("Download mapsforge map from "+sUrl);
                        } else if (uri.getScheme().equals("mf-theme")){
                            BgJobGroup bgJobGroup = new BgJobGroup(application, this, "Download map theme", new BgJobGroupCallback() {
                                @Override
                                public void afterGroupFinished(BgJobGroup jobGroup, int total, int success, int fail) {
                                    if (success > 0) {
                                        Intent intent = new Intent(MGMapActivity.this, SettingsActivity.class);
                                        intent.putExtra("FSControl.info", MainPreferenceScreen.class.getName());
                                        startActivity(intent);
                                    }
                                }
                            }){
                                @Override
                                public String getResultDetails() {
                                    if (successCounter > 0){
                                        return super.getDetails() + " finished successful.\n\n Now you can select downloaded theme.";
                                    } else {
                                        return super.getResultDetails();
                                    }
                                }
                            };
                            String sUrl = uri.toString().replaceFirst("mf-theme", "https");
                            bgJobGroup.addJob(OpenAndroMapsUtil.createBgJobsFromIntentUriTheme(pm, new URL(sUrl)));
                            bgJobGroup.setConstructed("Download mapsforge theme from "+sUrl);
                        }
                    }

                } else if (intent.getType().equals("mgmap/showTrack")){
                    String stl = intent.getStringExtra("stl");
                    String atl = intent.getStringExtra("atl");
                    List<String> atls = (atl==null)?new ArrayList<>():Arrays.asList( atl.substring(1,atl.length()-1).split(", ") );
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+"  " + intent.getType()+" stl="+stl+" atl="+atl);

                    BBox bBox2show = new BBox();
                    TrackLog selectedTrackLog = application.metaTrackLogs.get(stl);
                    if (selectedTrackLog != null){
                        application.getMetaDataUtil().checkAvailability(selectedTrackLog);
                        TrackLogRef selectedRef = new TrackLogRef(selectedTrackLog, -1);
                        application.availableTrackLogsObservable.setSelectedTrackLogRef(selectedRef);
                        bBox2show.extend(selectedTrackLog.getBBox());
                    }
                    for (String aatl : atls){
                        TrackLog aTrackLog = application.metaTrackLogs.get(aatl);
                        if (aTrackLog != null){
                            application.getMetaDataUtil().checkAvailability(aTrackLog);
                            application.availableTrackLogsObservable.availableTrackLogs.add(aTrackLog);
                            bBox2show.extend(aTrackLog.getBBox());
                        }
                    }

                    if (!bBox2show.isInitial()){
                        application.availableTrackLogsObservable.changed();
                        getMapViewUtility().zoomForBoundingBox(bBox2show);
                    }

                } else if (intent.getType().equals("mgmap/markTrack")){
                    String stl = intent.getStringExtra("stl");
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+"  " + intent.getType()+" stl="+stl);
                    if (stl != null){
                        BBox bBox2show = new BBox();
                        TrackLog selectedTrackLog = application.metaTrackLogs.get(stl);
                        if (selectedTrackLog != null){
                            String name = selectedTrackLog.getName();
                            TrackLog aTrackLog = new GpxImporter(getMGMapApplication().getElevationProvider())
                                    .parseTrackLog(name, getMGMapApplication().getPersistenceManager().openGpxInput(name));
                            if (aTrackLog.getReferencedTrackLog() != null){
                                selectedTrackLog = aTrackLog.getReferencedTrackLog();
                                getFS(FSMarker.class).createMarkerTrackLog(selectedTrackLog);
                            } else {
                                selectedTrackLog = aTrackLog;
                                TrackLogRef selectedRef = new TrackLogRef(selectedTrackLog, -1);
                                application.availableTrackLogsObservable.setSelectedTrackLogRef(selectedRef);
                                application.addBgJob (new BgJob(){
                                    @Override
                                    protected void doJob() {
                                        synchronized (getFS(FSRouting.class)){ // prevents that the Routing calculation thread is working
                                            getFS(FSMarker.class).createMarkerTrackLog(aTrackLog);
                                            getFS(FSRouting.class).optimize2(application.markerTrackLogObservable.getTrackLog());
                                        }
                                    }
                                });
                            }
                            bBox2show.extend(selectedTrackLog.getBBox());
                        }
                        if (!bBox2show.isInitial()){
                            getMapViewUtility().zoomForBoundingBox(bBox2show);
                        }
                    }

                } else {
                    Uri uri = intent.getData();
                    if (uri != null) {
                        Log.i(MGMapApplication.LABEL, NameUtil.context() + " uri: " + uri);
                        if (uri.toString().startsWith("content")){ // assume this is a gpx file
                            TrackLog trackLog = GpxImporter.checkLoadGpx(application, uri);
                            if (trackLog != null) {
                                application.metaTrackLogs.put(trackLog.getNameKey(), trackLog);
                                application.lastPositionsObservable.clear();
                                TrackLogRef refSelected = new TrackLogRefZoom(trackLog, trackLog.getNumberOfSegments() - 1, true);
                                application.availableTrackLogsObservable.setSelectedTrackLogRef(refSelected);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }

    }



    private static final int ACCESS_FINE_LOCATION_CODE = 995; // just an id to identify the callback
    private static final int ACCESS_BACKGROUND_LOCATION = 997; // just an id to identify the callback
    /** trigger TrackLoggerService, request permission on demand. */
    public void triggerTrackLoggerService(){
        Log.i(MGMapApplication.LABEL, NameUtil.context()+"  ");
        if (!(Permissions.check(this,  Manifest.permission.ACCESS_FINE_LOCATION))){
            Log.i(MGMapApplication.LABEL, NameUtil.context()+"  ");
            if (Build.VERSION.SDK_INT < 28){
                Permissions.request(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_CODE);
            } else {
                Permissions.request(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.FOREGROUND_SERVICE}, ACCESS_FINE_LOCATION_CODE);
            }

        } else {
            application.startTrackLoggerService(this);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACCESS_FINE_LOCATION_CODE) {
            if (Permissions.check(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION})) { //ok, got the permission, start service - dont't check for Manifest.permission.ACCESS_BACKGROUND_LOCATION - this leads to problem in Android 9 on first recording
                if (Build.VERSION.SDK_INT < 29) {
                    triggerTrackLoggerService();
                } else {
                    Permissions.request(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, ACCESS_BACKGROUND_LOCATION);
                }
            }
        }
        if (requestCode == ACCESS_BACKGROUND_LOCATION) {
            if (Permissions.check(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION})) { //ok, got the permission, start service - dont't check for Manifest.permission.ACCESS_BACKGROUND_LOCATION - this leads to problem in Android 9 on first recording
                triggerTrackLoggerService();
            }
        }
    }




    /**
     * There are are three cases distinguished:
     * 1.) take last position - if it is inside of a Mapsforge Map (of any map layer)
     * 2.) else take the first Mapsforge Map (in the sequece of map layers) and take the startPosition+startZoom from this
     * 3.) else take a hardcoded fix position in Heidelberg :-)
     */
    protected void initializePosition(IMapViewPosition mvp) {
        LatLong center = mvp.getCenter();
        if (mapDataStoreUtil.getMapDataStore(new BBox().extend(new PointModelImpl(center))) == null){
            MapDataStore mds = mapDataStoreUtil.getMapDataStore();
            if (mds != null){
                mvp.setMapPosition(new MapPosition(mds.startPosition(), mds.startZoomLevel()));
            } else {
                mvp.setMapPosition(new MapPosition(new LatLong(49.4057, 8.6789), (byte)15));
            }
        }
        mvp.setZoomLevelMax(ZOOM_LEVEL_MAX);
        mvp.setZoomLevelMin(ZOOM_LEVEL_MIN);
    }




    protected XmlRenderTheme getRenderTheme() {
        try {
            File theme = new File(application.getPersistenceManager().getThemesDir(), sharedPreferences.getString(getResources().getString(R.string.preference_choose_theme_key), "Elevate.xml"));
            ExternalRenderTheme renderTheme = new ExternalRenderTheme( theme.getAbsolutePath() );
            renderTheme.setMenuCallback(this);
            return renderTheme;
        } catch (FileNotFoundException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context()+" "+e.getMessage());

            return InternalRenderTheme.DEFAULT;
        }
    }

    public XmlRenderThemeStyleMenu getRenderThemeStyleMenu(){
        return renderThemeStyleMenu;
    }

    /** Callback from XmlRenderThemeMenuCallback. This callback provides the current renderThemeStyleMenu. */
    @Override
    public Set<String> getCategories(XmlRenderThemeStyleMenu menuStyle) {
        this.renderThemeStyleMenu = menuStyle;
        String id = this.sharedPreferences.getString(this.renderThemeStyleMenu.getId(),this.renderThemeStyleMenu.getDefaultValue());

        XmlRenderThemeStyleLayer baseLayer = this.renderThemeStyleMenu.getLayer(id);
        if (baseLayer == null) {
            Log.w(MGMapApplication.LABEL, NameUtil.context()+" Invalid style " + id );
            return null;
        }
        Set<String> result = baseLayer.getCategories();

        // add the categories from overlays that are enabled
        for (XmlRenderThemeStyleLayer overlay : baseLayer.getOverlays()) {
            if (this.sharedPreferences.getBoolean(overlay.getId(), overlay.isEnabled())) {
                result.addAll(overlay.getCategories());
            }
        }
        return result;
    }


    public MapView getMapsforgeMapView(){
        return mapView;
    }

    public MapViewUtility getMapViewUtility(){
        return mapViewUtility;
    }


    /** Depending on the preferences for the five map layers the corresponding layer object are created. */
    protected void createLayers() {
        Layers layers = mapView.getLayerManager().getLayers();
        for (String prefKey : mapLayerFactory.getMapLayerKeys()){
            String key = sharedPreferences.getString(prefKey, "");
            Log.d(MGMapApplication.LABEL, NameUtil.context()+" prefKey="+prefKey+" key="+key);
            Layer layer = mapLayerFactory.getMapLayer(key);
            if (layer != null){
                if (!layers.contains(layer)){
                    layers.add(layer);

                }
            }
        }
        // create additional control layer to be handle tap events. */
        layers.add(new MVLayer() {
            @Override
            protected boolean onTap(WriteablePointModel point) {
                if (getFS(FSPosition.class).check4MapMovingOff(point)) return true;
                TrackLogRef ref = selectCloseTrack( point );
                if (ref.getTrackLog() != null){
                    application.availableTrackLogsObservable.setSelectedTrackLogRef(ref);
                }
                return true;
            }

            @Override
            protected boolean onLongPress(PointModel point) {
                TrackLogRefApproach bestMatch = selectCloseTrack( point );
                TrackLog rtl = application.recordingTrackLogObservable.getTrackLog();
                if (rtl != null){
                    TrackLogRefApproach currentMatch = rtl.getBestDistance(point,bestMatch.getDistance());
                    if (currentMatch != null){
                        bestMatch = currentMatch;
                    }
                }
                TrackLog rotl = application.routeTrackLogObservable.getTrackLog();
                if (rotl != null){
                    TrackLogRefApproach currentMatch = rotl.getBestDistance(point,bestMatch.getDistance());
                    if (currentMatch != null){
                        bestMatch = currentMatch;
                    }
                }
                if (bestMatch.getTrackLog() != null){
                    if ((bestMatch.getTrackLog() == application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog()) && (bestMatch.getTrackLog().hasGainLoss())){
                        prefCache.get(R.string.FSATL_pref_stlGl, false).toggle();
                        return true;
                    }
                    if ((bestMatch.getTrackLog() == application.recordingTrackLogObservable.getTrackLog()) && (bestMatch.getTrackLog().hasGainLoss())) {
                        prefCache.get(R.string.FSRecording_pref_rtlGl, false).toggle();
                        return true;
                    }
                    if ((bestMatch.getTrackLog() == application.routeTrackLogObservable.getTrackLog()) && (bestMatch.getTrackLog().hasGainLoss())){
                        prefCache.get(R.string.FSRouting_pref_RouteGL, false).toggle();
                        return true;
                    }
                }
                return super.onLongPress(point);
            }

        });
    }

    public TrackLogRefApproach selectCloseTrack(PointModel pmTap) {
        TrackLogRefApproach bestMatch = new TrackLogRefApproach(null, -1,getMapViewUtility().getCloseThreshouldForZoomLevel());
        for (TrackLog trackLog : application.availableTrackLogsObservable.availableTrackLogs){
            TrackLogRefApproach currentMatch = trackLog.getBestDistance(pmTap,bestMatch.getDistance());
            if (currentMatch != null){
                bestMatch = currentMatch;
            }
        }
        return bestMatch;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Pref<Boolean> prefRoutingHints = prefCache.get(R.string.FSRouting_qc_RoutingHint, false);
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (!prefRoutingHints.getValue()){
                        getMapsforgeMapView().getModel().mapViewPosition.zoomIn();
                        return true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    if (!prefRoutingHints.getValue()){
                        getMapsforgeMapView().getModel().mapViewPosition.zoomOut();
                        return true;
                    }
                }
                break;
        }
        return super.dispatchKeyEvent(event);
    }
}
