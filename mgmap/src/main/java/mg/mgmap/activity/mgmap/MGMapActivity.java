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
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.EditText;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
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

import mg.mgmap.activity.mgmap.features.rtl.RecordingTrackLog;
import mg.mgmap.activity.mgmap.features.trad.FSTrackDetails;
import mg.mgmap.activity.mgmap.view.ControlMVLayer;
import mg.mgmap.activity.mgmap.view.MVLayer;
import mg.mgmap.activity.settings.MainPreferenceScreen;
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
import mg.mgmap.activity.mgmap.features.routing.FSRouting;
import mg.mgmap.activity.mgmap.features.rtl.FSRecordingTrackLog;
import mg.mgmap.activity.mgmap.features.search.FSSearch;
import mg.mgmap.activity.mgmap.features.time.FSTime;
import mg.mgmap.application.Setup;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.graph.GGraphTileFactory;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.TrackLogRef;
import mg.mgmap.generic.model.TrackLogRefApproach;
import mg.mgmap.generic.model.TrackLogRefZoom;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.BgJobGroup;
import mg.mgmap.generic.util.BgJobGroupCallback;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.FullscreenUtil;
import mg.mgmap.generic.util.GpxSyncUtil;
import mg.mgmap.generic.util.Zipper;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.gpx.GpxImporter;
import mg.mgmap.activity.mgmap.util.MapDataStoreUtil;
import mg.mgmap.activity.mgmap.util.MapViewUtility;
import mg.mgmap.activity.mgmap.util.OpenAndroMapsUtil;
import mg.mgmap.activity.mgmap.util.Permissions;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.TopExceptionHandler;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.util.hints.AbstractHint;
import mg.mgmap.generic.util.hints.HintAccessBackgroundLocation;
import mg.mgmap.generic.util.hints.HintAccessFineLocation;
import mg.mgmap.generic.util.hints.HintBatteryUsage;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
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

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    /** Reference to the MGMapApplication object */
    MGMapApplication application = null;
    /** Reference to the control view object.
     *  @see <a href="../../../images/MGMapViewer_ViewModel.PNG">MGMapActivity_ViewModel</a> */
    ControlView coView = null;

    final ArrayList<FeatureService> featureServices = new ArrayList<>();

    /** Reference to the renderThemeStyleMenu - will be set due to the callback getCategories
     * form XmlRenderThemeMenuCallback after reading the themes xml file */
    protected XmlRenderThemeStyleMenu renderThemeStyleMenu;

    MGMapLayerFactory mapLayerFactory = null;
    /** Reference to the MapViewUtility - provides so services around the MapView object */
    MapViewUtility mapViewUtility = null;

    private PrefCache prefCache;

    private MapDataStoreUtil mapDataStoreUtil = null;
    private GGraphTileFactory gGraphTileFactory = null;
    private final Runnable ttUploadGpxTrigger = () -> prefCache.get(R.string.preferences_sftp_uploadGpxTrigger, false).toggle();
    private final PropertyChangeListener prefGpsObserver = (e) -> triggerTrackLoggerService();
    private Pref<Boolean> prefTracksVisible;

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
    public MapDataStoreUtil getMapDataStoreUtil() {
        return mapDataStoreUtil;
    }
    public GGraphTileFactory getGGraphTileFactory() {
        return gGraphTileFactory;
    }
    public boolean getTrackVisibility(){ return prefTracksVisible.getValue(); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mgLog.i();
        application = (MGMapApplication) getApplication();
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler(application.getPersistenceManager()));
        //for fullscreen mode
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        createSharedPreferences();
        Setup.loadPropertiesToPreferences(getSharedPreferences(), application.getPersistenceManager().getConfigProperties("load", ".*.properties"));
        if (Build.VERSION.SDK_INT >= 27){
            setShowWhenLocked(true);
        }
        setContentView(R.layout.mgmapactivity);

        PointModelUtil.init(getResources().getInteger(R.integer.CLOSE_THRESHOLD));

        mapLayerFactory = new MGMapLayerFactory(this);

        prefCache = new PrefCache(this);

        initMapView();
        createLayers();

        mapDataStoreUtil = new MapDataStoreUtil().onCreate(mapLayerFactory, sharedPreferences); // includes init of mapLayerKeys with "none"
        String themeKey = getResources().getString(R.string.preference_choose_theme_key);
        getSharedPreferences().edit().putString(themeKey, sharedPreferences.getString(themeKey, "Elevate5.2/Elevate.xml") ).apply(); // set default for theme
        initSharedPreferencesDone(); // after MapDatastoreUtil creation
        mapViewUtility = new MapViewUtility(this, mapView);
        initializePosition();
        mgLog.i("Tilesize initial " + this.mapView.getModel().displayModel.getTileSize());

        // don't change orientation when device is rotated
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        coView = getControlView();
        boolean wayDetails = prefCache.get(R.string.FSGrad_pref_WayDetails_key, false).getValue();
        Pref<Boolean> prefSmooth4Routing = prefCache.get(R.string.preferences_smoothing4routing_key, false);
        gGraphTileFactory = new GGraphTileFactory().onCreate(mapDataStoreUtil, application.getElevationProvider(), wayDetails, prefSmooth4Routing);

        featureServices.add(new FSTime(this));
        featureServices.add(new FSAlpha(this));
        featureServices.add(new FSControl(this));

        featureServices.add(new FSAvailableTrackLogs(this));
        featureServices.add(new FSMarker(this));
        featureServices.add(new FSRouting(this, getFS(FSMarker.class), gGraphTileFactory));
        featureServices.add(new FSRecordingTrackLog(this));

        featureServices.add(new FSGraphDetails(this));
        featureServices.add(new FSSearch(this));
        featureServices.add(new FSPosition(this));
        featureServices.add(new FSBeeline(this));
        featureServices.add(new FSBB(this));
        featureServices.add(new FSTrackDetails(this));
        createLayers2();

        coView.init(application, this);
        if (!getIntent().toString().contains(" act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] "))
            onNewIntent(getIntent());
        // use prefGps from applications prefCache to prevent race conditions during startup phase
        application.prefGps.addObserver(prefGpsObserver);
        application.prefGps.onChange();
        prefCache.get(R.string.preferences_sftp_uploadGpxTrigger, false).addObserver((e) -> new GpxSyncUtil().trySynchronisation(application));
        prefTracksVisible = prefCache.get(R.string.preferences_tracks_visible, true);
        prefTracksVisible.addObserver(pcl -> getFS(FSAvailableTrackLogs.class).redraw());
    }

    @Override
    protected void onResume() {
        super.onResume();
        mgLog.i();
        application = (MGMapApplication) getApplication();

        // This is a workaround for frozen Screen after 2 times switch off/on with setShowWhenLocked(true);
        // Hint found at: https://stackoverflow.com/questions/55462980/android-9-frozen-ui-after-unlocking-screen
        if ((Build.VERSION.SDK_INT >= 27) && (Build.VERSION.SDK_INT <= 28)){
            AlertDialog alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle("");
            alertDialog.create();
            ColorDrawable dialogColor = new ColorDrawable(Color.GRAY);
            dialogColor.setAlpha(0);
            if (alertDialog.getWindow() != null){
                alertDialog.getWindow().setBackgroundDrawable(dialogColor);
            }
            alertDialog.show();
            new Thread(() -> {
                try {
                    Thread.sleep(3);
                } catch (Exception e){ mgLog.e(e.getMessage()); }
                alertDialog.dismiss();
            }).start();
        }

        for (FeatureService microService : featureServices) {
            try {
                mgLog.d("onResume " + microService + " beginning ");
                microService.onResume();
            } catch (Exception e) {
                mgLog.w("onResume " + microService + " failed: " + e.getMessage(), e);
            }
        }
        for (Layer layer : mapView.getLayerManager().getLayers()){
            if (layer instanceof TileDownloadLayer tileDownloadLayer) {
                tileDownloadLayer.onResume();
            }
        }

        application.recordingTrackLogObservable.changed();
        application.availableTrackLogsObservable.changed();
        application.lastPositionsObservable.changed();

        FeatureService.getTimer().postDelayed(ttUploadGpxTrigger, 25*1000);
        application.finishAlarm(); // just in case there is one
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus){
            FullscreenUtil.enforceState(this);
            mgLog.i();
        }
    }

    @Override
    protected void onPause() {
        mgLog.i();
        for (int i = featureServices.size() - 1; i >= 0; i--) { // reverse order
            FeatureService microService = featureServices.get(i);
            try {
                microService.onPause();
            } catch (Exception e) {
                mgLog.w("onPause " + microService + " failed: " + e.getMessage());
            }
        }
        for (Layer layer : mapView.getLayerManager().getLayers()){
            if (layer instanceof TileDownloadLayer tileDownloadLayer) {
                tileDownloadLayer.onPause();
            }
        }
        FeatureService.getTimer().removeCallbacks(ttUploadGpxTrigger);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mgLog.w("Destroy started");
        for (int i = featureServices.size() - 1; i >= 0; i--) { // reverse order
            FeatureService microService = featureServices.get(i);
            try {
                microService.onDestroy();
            } catch (Exception e) {
                mgLog.w("stop " + microService + " failed: " + e.getMessage());
            }
        }
        application.availableTrackLogsObservable.deleteObservers();
        application.recordingTrackLogObservable.deleteObservers();
        application.markerTrackLogObservable.deleteObservers();
        application.routeTrackLogObservable.deleteObservers();
        application.lastPositionsObservable.deleteObservers();

        application.prefGps.deleteObserver(prefGpsObserver);

        mapView.destroyAll();
        mapDataStoreUtil.onDestroy();
        gGraphTileFactory.onDestroy();
        prefCache.cleanup();
        AndroidGraphicFactory.clearResourceMemoryCache(); // do this as very last action - otherwise some crash might occur
        mgLog.w("Destroy finished");
        super.onDestroy();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        mgLog.i("level: "+level);
        for (int i = featureServices.size() - 1; i >= 0; i--) { // reverse order
            FeatureService microService = featureServices.get(i);
            try {
                microService.onTrimMemory(level);
            } catch (Exception e) {
                mgLog.w("onTrimMemory " + microService + " failed: " + e.getMessage());
            }
        }
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

        mgLog.i("Device scale factor " + DisplayModel.getDeviceScaleFactor());
        mgLog.i("Device screen size " + getResources().getDisplayMetrics().widthPixels + "x" + getResources().getDisplayMetrics().heightPixels);
        float fs = Float.parseFloat(sharedPreferences.getString(getResources().getString(R.string.preferences_scale_key), Float.toString(DisplayModel.getDefaultUserScaleFactor())));
        mgLog.i("User ScaleFactor " + fs);
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
            mgLog.i(intent);
            if ((intent != null)){
                mgLog.d("intent action="+intent.getAction());
                mgLog.d("intent categories="+intent.getCategories());
                mgLog.d("intent scheme="+intent.getScheme());
                mgLog.d("intent data="+intent.getData());
                mgLog.d("intent type="+intent.getType());
                if (intent.getData()!=null)
                    mgLog.d("intent data.path="+intent.getData().getPath());
                if (intent.getData()!=null)
                    mgLog.d("intent data.host="+intent.getData().getHost());
                mgLog.d("intent flags="+ Integer.toHexString( intent.getFlags() ));
                if ((intent.getAction() != null) && (intent.getAction().endsWith("DUPLICATE"))){
                    mgLog.i("duplicate intent detected, don't handle.");
                    return;
                }
                intent.setAction(intent.getAction()+".DUPLICATE");
                String paramKey = getResources().getString(R.string.activity_param_key);
                if (intent.hasExtra(paramKey)){
                    String value = intent.getStringExtra(paramKey);
                    mgLog.i(paramKey+" "+value );
                    prefCache.get(R.string.activity_param_key, "").setValue(value);
                }


                Uri uri = intent.getData();
                mgLog.i("uri: " + uri);
                PersistenceManager pm = application.getPersistenceManager();
                if ("mgmap/showTrack".equals(intent.getType())){
                    String stl = intent.getStringExtra("stl");
                    String atl = intent.getStringExtra("atl");
                    List<String> atls = (atl==null)?new ArrayList<>():Arrays.asList( atl.substring(1,atl.length()-1).split(", ") );
                    mgLog.i(intent.getType()+" stl="+stl+" atl="+atl);

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
                } else if ("mgmap/markTrack".equals(intent.getType())){
                    String stl = intent.getStringExtra("stl");
                    mgLog.i(intent.getType()+" stl="+stl);
                    if (stl != null){
                        BBox bBox2show = new BBox();
                        TrackLog selectedTrackLog = application.metaTrackLogs.get(stl);
                        if (selectedTrackLog != null){
                            String name = selectedTrackLog.getName();
                            TrackLog aTrackLog = new GpxImporter(getMGMapApplication().getElevationProvider())
                                    .parseTrackLog(name, getMGMapApplication().getPersistenceManager().openGpxInput(name));
                            if (aTrackLog != null){
                                if (aTrackLog.getReferencedTrackLog() != null){
                                    selectedTrackLog = aTrackLog.getReferencedTrackLog();
                                    getFS(FSMarker.class).createMarkerTrackLog(selectedTrackLog);
                                } else {
                                    selectedTrackLog = aTrackLog;
                                    getFS(FSRouting.class).prepareOptimize();
                                    TrackLogRef selectedRef = new TrackLogRef(selectedTrackLog, -1);
                                    application.availableTrackLogsObservable.setSelectedTrackLogRef(selectedRef);
                                    application.addBgJob (new BgJob(){
                                        @Override
                                        protected void doJob() {
                                            getFS(FSMarker.class).createMarkerTrackLog(aTrackLog);
                                            getFS(FSRouting.class).optimize2(application.markerTrackLogObservable.getTrackLog());
                                        }
                                    });
                                }
                                bBox2show.extend(selectedTrackLog.getBBox());
                            }
                        }
                        if (!bBox2show.isInitial()){
                            getMapViewUtility().zoomForBoundingBox(bBox2show);
                        }
                    }
                } else if (uri == null){
                    mgLog.i("No further processing due to uri=null");
                } else if ("mf-v4-map".equals(uri.getScheme())){
                    BgJobGroup bgJobGroup = new BgJobGroup(application, this, "Download map", new BgJobGroupCallback() {
                        @Override
                        public void afterGroupFinished(BgJobGroup jobGroup, int total, int success, int fail) {
                            if (success > 0){
                                Intent intent = new Intent(MGMapActivity.this, SettingsActivity.class);
                                intent.putExtra("FSControl.info", MainPreferenceScreen.class.getName());
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
                } else if ("mf-theme".equals(uri.getScheme())){
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
                } else if ("mgmap-install".equals(uri.getScheme())){
                    final EditText etPassword = new androidx.appcompat.widget.AppCompatEditText(MGMapActivity.this);
                    BgJobGroupCallback bgJobGroupCallback = new BgJobGroupCallback() {
                        @Override
                        public View getContentView() {
                            etPassword.setHint("Password (optional)");
                            etPassword.setHintTextColor(CC.getColor(R.color.CC_GRAY200));
                            etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
                            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                            return etPassword;
                        }
                        @Override
                        public void afterGroupFinished(BgJobGroup jobGroup, int total, int success, int fail) {
                            BgJobGroupCallback.super.afterGroupFinished(jobGroup, total, success, fail);
                            prefCache.get(R.string.MGMapActivity_trigger_recreate,"").setValue("trigger recreate at "+System.currentTimeMillis());
                        }
                    };
                    BgJobGroup bgJobGroup = new BgJobGroup(application, this, "Install zip archive", bgJobGroupCallback );
                    String sUrl = uri.toString().replaceFirst("mgmap-install", "https");
                    bgJobGroup.addJob( new BgJob() {
                        @Override
                        protected void doJob() throws Exception {
                            super.doJob();
                            Editable edPassword = etPassword.getText();
                            Zipper zipper = new Zipper(edPassword==null?null:edPassword.toString());
                            zipper.unpack(new URL(sUrl), pm.getAppDir(), null, this);
                        }
                    } );
                    bgJobGroup.setConstructed("Download and install "+sUrl);
                } else if ("geo".equals(uri.getScheme())){
                    String sUri = Uri.decode(uri.toString());
                    getFS(FSSearch.class).processGeoIntent(sUri);
                } else if ("content".equals(uri.getScheme())) {
                    ContentResolver contentResolver = application.getContentResolver();

                    try (Cursor cursor = contentResolver.query(uri, null, null, null, null)) {
                        if (cursor != null && cursor.moveToFirst()) {
                            @SuppressLint("Range")
                            String filename = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                            mgLog.i("found Filename " + filename);
                            // unfortunately the filename does not necessarily correspond to the name that is found here.
                            // This implies also, that a check fpr a ".gpx" file is simply not possible. As a consequence
                            // other files will run into an parse exception ...
                            try (InputStream is = contentResolver.openInputStream(uri)){
                                if (is != null) {
                                    mgLog.i("Track loaded for " + uri);
                                    mgLog.i("Track loaded to " + filename);
                                    TrackLog trackLog = new GpxImporter(application.getElevationProvider()).parseTrackLog( filename, is);
                                    if (trackLog != null) {
                                        application.metaTrackLogs.put(trackLog.getNameKey(), trackLog);
                                        application.lastPositionsObservable.clear();
                                        TrackLogRef refSelected = new TrackLogRefZoom(trackLog, trackLog.getNumberOfSegments() - 1, true);
                                        application.availableTrackLogsObservable.setSelectedTrackLogRef(refSelected);
                                    }
                                } else {
                                    mgLog.e("InputStream is null");
                                }
                            } // try is
                        } // if (cursor != null && cursor.moveToFirst())
                    } // try (Cursor cursor = ...

                } // if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme()))

            } // if ((intent != null))
        } catch (Exception e) {
            mgLog.e(e);
        }

    }



    private static final int ACCESS_FINE_LOCATION_CODE = 995; // just an id to identify the callback
    private static final int ACCESS_BACKGROUND_LOCATION = 997; // just an id to identify the callback
    /** trigger TrackLoggerService, request permission on demand. */
    @SuppressLint("BatteryLife")
    public void triggerTrackLoggerService(){
        mgLog.i();
        if (application.prefGps.getValue()){
            if (!(Permissions.check(this,  Manifest.permission.ACCESS_FINE_LOCATION))){
                mgLog.i();
                AbstractHint hint = new HintAccessFineLocation(this).addGotItAction(() -> {
                    if (Build.VERSION.SDK_INT < 28){
                        Permissions.request(MGMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_FINE_LOCATION_CODE);
                    } else {
                        Permissions.request(MGMapActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.FOREGROUND_SERVICE}, ACCESS_FINE_LOCATION_CODE);
                    }
                });
                application.getHintUtil().showHint(hint);
            } else {
                application.startTrackLoggerService(this, true);
                getMGMapApplication().getHintUtil().showHint(new HintBatteryUsage(this)
                        .addGotItAction(()-> {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                            intent.setData(Uri.parse("package:"+getPackageName()));
                            startActivity(intent);
                        }));
            }
        } else {
            application.startTrackLoggerService(this, false);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACCESS_FINE_LOCATION_CODE) {
            if (Permissions.check(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION})) { //ok, got the permission, start service - dont't check for Manifest.permission.ACCESS_BACKGROUND_LOCATION - this leads to problem in Android 9 on first recording
                if (Build.VERSION.SDK_INT < 29) {
                    triggerTrackLoggerService();
                } else {
                    if (Permissions.check(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION})) {
                        triggerTrackLoggerService();
                    } else  {
                        getMGMapApplication().getHintUtil().showHint(new HintAccessBackgroundLocation(this)
                                .addGotItAction(()->Permissions.request(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, ACCESS_BACKGROUND_LOCATION)));
                    }
                }
            } else {
                application.prefGps.setValue(false);
                RecordingTrackLog rtl = application.recordingTrackLogObservable.getTrackLog();
                if (rtl != null){
                    rtl.stopTrack(0);
                    application.recordingTrackLogObservable.setTrackLog(null);
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
    protected void initializePosition() {
        IMapViewPosition mvp = mapView.getModel().mapViewPosition;
        if (mapDataStoreUtil.getMapDataStore(new BBox().extend(mapViewUtility.getCenter())) == null){ // current position is no inside any Mapsforge Map layer
            MapDataStore mds = mapDataStoreUtil.getMapDataStore();
            if (mds != null){ // is there any mapsforge layer
                mapViewUtility.setCenter(MapViewUtility.getMapDataStoreCenter(mds));
                mvp.setZoomLevel(mds.startZoomLevel());
            } else {
                mapViewUtility.setCenter(new PointModelImpl(49.4057, 8.6789));
                mvp.setZoomLevel((byte)15);
            }
        }
        mvp.setZoomLevelMax(MapViewUtility.ZOOM_LEVEL_MAX);
        mvp.setZoomLevelMin(MapViewUtility.ZOOM_LEVEL_MIN);
    }




    protected XmlRenderTheme getRenderTheme() {
        try {
            File theme = new File(application.getPersistenceManager().getThemesDir(), sharedPreferences.getString(getResources().getString(R.string.preference_choose_theme_key), "invalid.xml"));
            ExternalRenderTheme renderTheme = new ExternalRenderTheme( theme.getAbsolutePath() );
            renderTheme.setMenuCallback(this);
            return renderTheme;
        } catch (FileNotFoundException e) {
            if ((e.getMessage()!=null) && (!e.getMessage().contains("invalid.xml"))){
                mgLog.e(e.getMessage());
            }
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
            mgLog.w("Invalid style " + id );
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
            mgLog.d("prefKey="+prefKey+" key="+key);
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
                if (toggleGlOnMatch(bestMatch, application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog(), prefCache.get(R.string.FSATL_pref_stlGl, false))) return true;
                if (toggleGlOnMatch(bestMatch, application.recordingTrackLogObservable.getTrackLog(), prefCache.get(R.string.FSRecording_pref_rtlGl, false))) return true;
                if (toggleGlOnMatch(bestMatch, application.routeTrackLogObservable.getTrackLog(), prefCache.get(R.string.FSRouting_pref_RouteGL, false))) return true;
                if (prefTracksVisible.getValue()){
                    prefTracksVisible.setValue(Boolean.FALSE);
                    FeatureService.getTimer().postDelayed(() -> prefTracksVisible.setValue(Boolean.TRUE), 1200);
                    return true;
                }
                return super.onLongPress(point);
            }

        });
    }

    public void createLayers2(){
        Layers layers = mapView.getLayerManager().getLayers();

        // create additional control layer to handle dashboard drag events */
        layers.add(new ControlMVLayer<String>(FeatureService.getTimer()) {
            @Override
            public boolean onTap(LatLong tapLatLong, Point layerXY, Point tapXY) {
                // we need to use this for EnlargeControl of Dashboard (instead of OnClickListener of dashboard views) - otherwise we don't get the drag events here
                if (coView.checkDashboardEntryView((float)tapXY.x, (float)tapXY.y)) return true;
                return super.onTap(tapLatLong, layerXY, tapXY);
            }

            @Override
            protected boolean checkDrag(float scrollX, float scrollY) {
                String dashboardName = coView.checkDashboardEntry(scrollX,scrollY);
                setDragObject(dashboardName);
                if (dashboardName != null){
                    getFS(FSTrackDetails.class).initDashboardDrag(dashboardName);
                    return true;
                }
                return false;
            }

            @Override
            protected void handleDrag(float scrollX1, float scrollY1, float scrollX2, float scrollY2) {
                if (getFS(FSTrackDetails.class).handleDashboardDrag(scrollX1,scrollY1,scrollX2,scrollY2)){
                    abortDrag(scrollX1, scrollY1);
                }
            }

            @Override
            protected void abortDrag(float scrollX, float scrollY) {
                super.abortDrag(scrollX, scrollY);
                getFS(FSTrackDetails.class).abortDashboardDrag();
            }
        });

    }

    private boolean toggleGlOnMatch(TrackLogRefApproach bestMatch, TrackLog candidate, Pref<Boolean> candidatesPref){
        if ((bestMatch.getTrackLog() == candidate) && (candidate != null)){
            if (candidate.hasGainLoss() || candidatesPref.getValue()){
                candidatesPref.toggle();
                return true;
            }
        }
        return false;
    }

    public TrackLogRefApproach selectCloseTrack(PointModel pmTap) {
        TrackLogRefApproach bestMatch = new TrackLogRefApproach(null, -1,getMapViewUtility().getCloseThresholdForZoomLevel());
        for (TrackLog trackLog : new ArrayList<>( application.availableTrackLogsObservable.availableTrackLogs) ){
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
