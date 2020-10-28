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
package mg.mapviewer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import android.preference.Preference;
import android.util.Log;
import android.view.View;
import android.view.Window;


import androidx.annotation.NonNull;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;

import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.download.TileDownloadLayer;
import org.mapsforge.map.layer.renderer.MapWorkerPool;
import org.mapsforge.map.layer.renderer.TileRendererLayer;

import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleLayer;
import org.mapsforge.map.rendertheme.XmlRenderThemeStyleMenu;

import mg.mapviewer.features.atl.MSAvailableTrackLogs;
import mg.mapviewer.features.bb.MSBB;
import mg.mapviewer.features.gdrive.MSGDrive;
import mg.mapviewer.features.grad.MSGraphDetails;
import mg.mapviewer.features.marker.MSMarker;
import mg.mapviewer.features.motion.MSMotion;
import mg.mapviewer.features.position.MSPosition;
import mg.mapviewer.features.remainings.MSRemainings;
import mg.mapviewer.features.routing.MSRouting;
import mg.mapviewer.features.routing.MSRoutingHint;
import mg.mapviewer.features.rtl.MSRecordingTrackLog;
import mg.mapviewer.features.search.MSSearch;
import mg.mapviewer.features.search.SearchView;
import mg.mapviewer.features.time.MSTime;
import mg.mapviewer.model.BBox;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.model.TrackLogRef;
import mg.mapviewer.model.TrackLogRefApproach;
import mg.mapviewer.model.TrackLogRefZoom;
import mg.mapviewer.model.WriteablePointModel;
import mg.mapviewer.model.WriteableTrackLog;
import mg.mapviewer.util.BgJob;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.GpxImporter;
import mg.mapviewer.util.MapViewUtility;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.OpenAndroMapsUtil;
import mg.mapviewer.util.Permissions;
import mg.mapviewer.util.PersistenceManager;
import mg.mapviewer.util.PointModelUtil;
import mg.mapviewer.util.TopExceptionHandler;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.util.Zipper;
import mg.mapviewer.view.MVLayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
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

    /** Reference to the renderThemeStyleMenu - will be set due to the callback getCategories
     * form XmlRenderThemeMenuCallback after reading the themes xml file */
    protected XmlRenderThemeStyleMenu renderThemeStyleMenu;

    /** Reference to the MapViewUtility - provides so servoices around the MapView object */
    MapViewUtility mapViewUtility = null;
    /** Registry of MicroServices - see also {@link mg.mapviewer.MGMicroService} for the micro service concept. */
    ArrayList<MGMicroService> microServices = null;
    /** A timer object. */
    private Handler timer = new Handler();



    public ControlView getControlView(){
        return (ControlView) findViewById(R.id.controlView);
    }

    SharedPreferences getSharedPreferences(){
        return sharedPreferences;
    }

    /** Retruen the mirco service by type */
    public <T> T getMS(Class<T> tClass){
        for (MGMicroService service : microServices){
            if (tClass.isInstance(service)) return (T)service;
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler());
        Log.w(MGMapApplication.LABEL, NameUtil.context());
        //set fullsrceen mode
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);

        PersistenceManager.getInstance(this); // initialize the PersistenceService
        MGMapLayerFactory.setContext(getApplicationContext());
        MGMapLayerFactory.setActivity(this);
        MGMapLayerFactory.mapLayers.clear();

        application = (MGMapApplication) getApplication();
        createSharedPreferences();
        setContentView(R.layout.mapviewer);

        initMapView();
        createLayers();
        createControls();
        initializePosition(mapView.getModel().mapViewPosition);
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" Tilesize initial " + this.mapView.getModel().displayModel.getTileSize());

        CC.setActivity(this);
        PointModelUtil.init(getResources().getInteger(R.integer.CLOSE_THRESHOLD));

        // don't change orientation when device is rotated
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        coView = getControlView();
        mapViewUtility = new MapViewUtility(getApplicationContext(), mapView);

        // micro service registry is stored in the context of the application
        microServices = application.microServices;
        // but is setup fresh on activity restart
        microServices.clear();


        microServices.add(new MSTime(this));
        microServices.add(new MSMotion(this));
        microServices.add(new MSPosition(this));
        microServices.add(new MSRecordingTrackLog(this));
        microServices.add(new MSAvailableTrackLogs(this));
        microServices.add(new MSMarker(this));
        microServices.add(new MSRouting(this));
        microServices.add(new MSRemainings(this));
        microServices.add(new MSBB(this, getMS(MSAvailableTrackLogs.class)));
        microServices.add(new MSGraphDetails(this));
        microServices.add(new MSSearch(this));
        microServices.add(new MSGDrive(this));

        try{
            Thread.sleep(100);
        }catch (Exception e){}
        coView.init(application, this);
        onNewIntent(getIntent());
    }

    private FullscreenObserver fullscreenObserver = null;

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(MGMapApplication.LABEL, NameUtil.context());
        application = (MGMapApplication) getApplication();

        for (MGMicroService microService : microServices) {
            try {
                microService.start();
            } catch (Exception e) {
                Log.w(MGMapApplication.LABEL, NameUtil.context()+" start " + microService + " failed: " + e.getMessage(), e);
            }
        }
        for (Layer layer : mapView.getLayerManager().getLayers()){
            if (layer instanceof TileDownloadLayer) {
                TileDownloadLayer tileDownloadLayer = (TileDownloadLayer) layer;
                tileDownloadLayer.onResume();
            }
        }
        fullscreenObserver = new FullscreenObserver();
        application.fullscreen.addObserver(fullscreenObserver);

        application.recordingTrackLogObservable.changed();
        application.availableTrackLogsObservable.changed();
        application.lastPositionsObservable.changed();
        application.markerTrackLogObservable.changed();
        application.fullscreen.changed();
    }

    @Override
    protected void onPause() {
        Log.i(MGMapApplication.LABEL, NameUtil.context());

        if (microServices == null) return;
        for (int i = microServices.size() - 1; i >= 0; i--) {
            MGMicroService microService = microServices.get(i);
            try {
                microService.stop();
            } catch (Exception e) {
                Log.w(MGMapApplication.LABEL, NameUtil.context()+" stop " + microService + " failed: " + e.getMessage());
            }
        }
        for (Layer layer : mapView.getLayerManager().getLayers()){
            if (layer instanceof TileDownloadLayer) {
                TileDownloadLayer tileDownloadLayer = (TileDownloadLayer) layer;
                tileDownloadLayer.onPause();
            }
        }
        application.fullscreen.deleteObserver(fullscreenObserver);
        fullscreenObserver = null;

        application.recordingTrackLogObservable.changed();
        application.availableTrackLogsObservable.changed();
        application.lastPositionsObservable.changed();
        application.markerTrackLogObservable.changed();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.w(MGMapApplication.LABEL, NameUtil.context());
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
    }



    @Override
    protected void createSharedPreferences() {
        super.createSharedPreferences();
        MGMapLayerFactory.setSharedPreferences(sharedPreferences);
        MGMapLayerFactory.setXmlRenderTheme(getRenderTheme());

        Log.i(MGMapApplication.LABEL, NameUtil.context() + " Device scale factor " + Float.toString(DisplayModel.getDeviceScaleFactor()));
        Log.i(MGMapApplication.LABEL, NameUtil.context() + " Device screen size " + getResources().getDisplayMetrics().widthPixels + "x" + getResources().getDisplayMetrics().heightPixels);
        float fs = Float.valueOf(sharedPreferences.getString(getResources().getString(R.string.preferences_scale_key), Float.toString(DisplayModel.getDefaultUserScaleFactor())));
        Log.i(MGMapApplication.LABEL, NameUtil.context() + " User ScaleFactor " + Float.toString(fs));
        if (fs != DisplayModel.getDefaultUserScaleFactor()) {
            DisplayModel.setDefaultUserScaleFactor(fs);
        }
        MapFile.wayFilterEnabled = sharedPreferences.getBoolean(getResources().getString(R.string.preferences_wayfiltering_key), true);
        if (MapFile.wayFilterEnabled) {
            MapFile.wayFilterDistance = Integer.parseInt(sharedPreferences.getString(getResources().getString(R.string.preferences_wayfiltering_distance_key), "20"));
        }
        MapWorkerPool.DEBUG_TIMING = sharedPreferences.getBoolean(getResources().getString(R.string.preferences_debug_timing_key), false);

        application.wayDetails.setValue(sharedPreferences.getBoolean(getResources().getString(R.string.preferences_way_details_key), false));
        application.stlWithGL.setValue(sharedPreferences.getBoolean(getResources().getString(R.string.preferences_stl_gl_key), true));

    }

    /** Used if .gpx file is opened from mail oder file manager */
    @Override
    protected void onNewIntent(Intent intent) {
        try {
            Log.w(MGMapApplication.LABEL, NameUtil.context()+"  " + intent);
            if (intent != null) {
                Uri uri = intent.getData();
                if (uri != null) {
                    Log.i(MGMapApplication.LABEL, NameUtil.context() + " uri: " + uri);
                    if (uri.toString().startsWith("content")){ // assume this is a gpx file
                        TrackLog trackLog = GpxImporter.checkLoadGpx(application, uri);
                        if (trackLog != null) {
                            application.lastPositionsObservable.clear();
                            TrackLogRef refSelected = new TrackLogRefZoom(trackLog, trackLog.getNumberOfSegments() - 1, true);
                            application.availableTrackLogsObservable.setSelectedTrackLogRef(refSelected);
                        }
                    } else {
                        // check for download jobs from openandromaps
                        application.addBgJobs(OpenAndroMapsUtil.createBgJobsFromIntentUri(uri));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }




    private static final int READ_EXTERNAL_STORAGE_CODE = 991; // just an id to identify the callback
    private static final int ACCESS_FINE_LOCATION_CODE = 995; // just an id to identify the callback
    /** trigger TrackLoggerService, request permission on demand. */
    public void triggerTrackLoggerService(){
        if (!(Permissions.check(this,  Manifest.permission.ACCESS_FINE_LOCATION))){
            Permissions.request(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.FOREGROUND_SERVICE}, ACCESS_FINE_LOCATION_CODE);
        } else {
            Intent intent = new Intent(this, TrackLoggerService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.startForegroundService(intent);
            } else {
                this.startService(intent);
            }
        }
    }

    public boolean requestReadPermissions(){
        if (!Permissions.check(this, Manifest.permission.READ_EXTERNAL_STORAGE) ) {
            Permissions.request(this, Manifest.permission.READ_EXTERNAL_STORAGE , READ_EXTERNAL_STORAGE_CODE);
            return true;
        }
        return false;
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ACCESS_FINE_LOCATION_CODE ){
            if (Permissions.check(this, new String[]{ Manifest.permission.ACCESS_FINE_LOCATION} )){ //ok, got the permission, start service - dont't check for Manifest.permission.ACCESS_BACKGROUND_LOCATION - this leads to problem in Android 9 on first recording
                triggerTrackLoggerService();
            }
        }
        if (requestCode == READ_EXTERNAL_STORAGE_CODE ){
            if (Permissions.check(this, Manifest.permission.READ_EXTERNAL_STORAGE) ){ //ok, got the read permission -> restart
                Log.w(MGMapApplication.LABEL, NameUtil.context()+ " Restart NOW");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MGMapActivity.this.recreate(); // restart activity
                    }
                },200);
            }
        }
    }




    /**
     * There are are three cases distinguished:
     * 1.) take last position - if it is inside of a Mapsforge Map (of any map layer)
     * 2.) else take the first Mapsforge Map (in the sequece of map layers) and take the startPosition+startZoom from this
     * 3.) else take a hardcoded fix position in Heidelberg :-)
     */
    protected IMapViewPosition initializePosition(IMapViewPosition mvp) {
        LatLong center = mvp.getCenter();
        if (getMapDataStore(new BBox().extend(new PointModelImpl(center))) == null){
            MapDataStore mds = getMapDataStore(null);
            if (mds != null){
                mvp.setMapPosition(new MapPosition(mds.startPosition(), mds.startZoomLevel()));
            } else {
                mvp.setMapPosition(new MapPosition(new LatLong(49.4057, 8.6789), (byte)15));
            }
        };
        mvp.setZoomLevelMax(ZOOM_LEVEL_MAX);
        mvp.setZoomLevelMin(ZOOM_LEVEL_MIN);
        return mvp;
    }




    protected XmlRenderTheme getRenderTheme() {
        try {
            File theme = new File(PersistenceManager.getInstance().getThemesDir(), sharedPreferences.getString(getResources().getString(R.string.preference_choose_theme_key), "Elevate.xml"));
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
    public Handler getTimer(){
        return timer;
    }
    public MapViewUtility getMapViewUtility(){
        return mapViewUtility;
    }


    /** Return a MapDataStore if it contains a given BBox. This is used e.g. to find the MapDataStore for route calculation. */
    public MapDataStore getMapDataStore(BBox bBox) {
        String language = sharedPreferences.getString(getResources().getString(R.string.preferences_language_key), "");
        for (String prefKey : application.getMapLayerKeys()){
            String key = sharedPreferences.getString(prefKey, language);

            Layer layer = MGMapLayerFactory.getMapLayer(key);
            if (layer instanceof TileRendererLayer) {
                MapDataStore mds = ((TileRendererLayer)layer).getMapDataStore();
                if (mds != null){
                    if (bBox == null){
                        return mds;
                    }
                    if (bBox.isPartOf(mds.boundingBox())){
                        return mds;
                    }
                }
            }
        }
        return null;
    }

    /** Depending on the preferences for the five map layers the corresponding layer object are created. */
    protected void createLayers() {
        MGMapLayerFactory.setMapView(mapView);
        Layers layers = mapView.getLayerManager().getLayers();
        for (String prefKey : application.getMapLayerKeys()){
            String key = sharedPreferences.getString(prefKey, "");
            Layer layer = null;
            layer = MGMapLayerFactory.getMapLayer(key);
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
                { // this sectio is just for analysis purposes
                    WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();
                    if (mtl != null){
                        TrackLogRefApproach pointRef = mtl.getBestPoint(point, PointModelUtil.getCloseThreshold());
                        if (pointRef != null){
                            Log.i(MGMapApplication.LABEL, NameUtil.context() + "idx=" +pointRef.getEndPointIndex()+" "+ pointRef.getApproachPoint());
                        }
                    }
                }

                if (!getMS(MSAvailableTrackLogs.class).selectCloseTrack( point )){
                    coView.toggleMenuVisibility();
                }
                return true;
            }

        });
    }

    public class FullscreenObserver implements Observer{
        @Override
        public void update(Observable o, Object arg) {
            if (application.fullscreen.getValue()){
                setFullscreen();
            } else {
                hideFullscreen();
            }
        }
    }

    void setFullscreen() {
        int newUiOptions = this.getWindow().getDecorView().getSystemUiVisibility();
        newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        this.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
    void hideFullscreen() {
        int newUiOptions = this.getWindow().getDecorView().getSystemUiVisibility();
        newUiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        this.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
}
