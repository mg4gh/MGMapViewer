package mg.mapviewer;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.renderer.MapWorkerPool;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.reader.MapFile;

import mg.mapviewer.model.WriteableTrackLog;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLogRef;
import mg.mapviewer.model.TrackLogSegment;
import mg.mapviewer.util.BgJob;
import mg.mapviewer.util.Geoid;
import mg.mapviewer.util.GpxExporter;
import mg.mapviewer.util.MetaDataUtil;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PersistenceManager;
import mg.mapviewer.features.rtl.RecordingTrackLog;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.util.ExtrasUtil;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * The application of the MgMapActivity.
 * Mainly it provides some Observable objects that represent the application state.
 * These state objects ensure that the applications state survive a restart of the activity.
 * Second this decouples the sensors, which change the model objects from the services, which act depending on these changes.
 */
public class MGMapApplication extends Application {

    // Label for Logging.
    public static final String LABEL = "MGMapViewer";

    @Override
    public void onCreate() {
        System.out.println("MGMapViewer Application start!!!!");
        super.onCreate();

        try {
            String cmd = "logcat "+ LABEL+":i  -f "+PersistenceManager.getInstance(this).getLogDir().getAbsolutePath()+"/log.txt -r 10000 -n10";
            Log.i(LABEL, NameUtil.context()+" Start Logging: "+cmd);
            Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            Log.e(LABEL, NameUtil.context(),e);
        }
        Log.i(LABEL,NameUtil.context()+" Starting Logger finished.");


        AndroidGraphicFactory.createInstance(this);
        Log.v(MGMapApplication.LABEL, NameUtil.context());
        Log.d(MGMapApplication.LABEL, NameUtil.context());
        Log.i(MGMapApplication.LABEL, NameUtil.context());
        Log.w(MGMapApplication.LABEL, NameUtil.context());
        Log.e(MGMapApplication.LABEL, NameUtil.context());

        ExtrasUtil.checkCreateMeta();

        Log.i(LABEL, NameUtil.context()+" onCreate: Device scale factor "+ Float.toString(DisplayModel.getDeviceScaleFactor()));
        Log.i(LABEL, NameUtil.context()+" onCreate: Device screen size "+ getResources().getDisplayMetrics().widthPixels+"x"+getResources().getDisplayMetrics().heightPixels);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        float fs = Float.valueOf(preferences.getString(getResources().getString(R.string.preferences_scale_key),Float.toString(DisplayModel.getDefaultUserScaleFactor())));
        Log.i(LABEL, NameUtil.context()+" onCreate: User ScaleFactor " + Float.toString(fs));
        if (fs != DisplayModel.getDefaultUserScaleFactor()) {
            DisplayModel.setDefaultUserScaleFactor(fs);
        }

        MapFile.wayFilterEnabled = preferences.getBoolean(getResources().getString(R.string.preferences_wayfiltering_key), true);
        if (MapFile.wayFilterEnabled) {
            MapFile.wayFilterDistance = Integer.parseInt(preferences.getString(getResources().getString(R.string.preferences_wayfiltering_distance_key), "20"));
        }
        MapWorkerPool.DEBUG_TIMING = preferences.getBoolean(getResources().getString(R.string.preferences_debug_timing_key), false);
        wayDetails.setValue( preferences.getBoolean(getResources().getString(R.string.preferences_way_details_key), false) );
        stlWithGL.setValue( preferences.getBoolean(getResources().getString(R.string.preferences_stl_gl_key), true) );

        centerCurrentPosition.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                lastPositionsObservable.changed();
            }
        });


        // Recover state of RecordingTrackLog
        new AsyncTask<Object, Integer, RecordingTrackLog>() {
            @Override
            protected RecordingTrackLog doInBackground(Object... objects) {
                RecordingTrackLog rtl = RecordingTrackLog.initFromRaw();
                if ((rtl != null) && !rtl.isTrackRecording()){ // either finished or not yet started
                    if (rtl.getNumberOfSegments() > 0){  // is finished
                        GpxExporter.export(rtl);
                    }
                    PersistenceManager.getInstance(null).clearRaw();
                    rtl = null;
                }
                return rtl;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
            }

            @Override
            protected void onPostExecute(RecordingTrackLog recordingTrackLog) {
                recordingTrackLogObservable.setTrackLog(recordingTrackLog);
                initFinished();
            }
        }.execute();


        // initialize hgt data handling
        new Geoid(this);

        // initialize MetaData (as used from AvailableTrackLogs service
        new AsyncTask<Object, Object, Object>(){
            @Override
            protected Object doInBackground(Object... objects) {
                metaTrackLogs.addAll(MetaDataUtil.loadMetaData());
                return null;
            }
        }.execute();

        // initialize handling of new points from TrackLoggerService
        new AsyncTask<Object, Object, Object>(){
            @Override
            protected Object doInBackground(Object... objects) {
                Log.i(LABEL, NameUtil.context()+"doInBackground: handle TrackLogPoints started ");
                while (true){
                    try {
                        PointModel pointModel = logPoints2process.take();
                        if (recordingTrackLogObservable.getTrackLog() != null){
                            recordingTrackLogObservable.getTrackLog().addPoint(pointModel);
                            Log.v(LABEL, NameUtil.context()+ " doInBackground: Processed TrackLogPoint: "+pointModel);
                        }
                    } catch (Exception e) {
                        Log.e(LABEL, NameUtil.context()+" doInBackground: "+e.getMessage(),e);
                    }
                }

            }
        }.execute();

//        ExtrasUtil.generate(new BoundingBox(49, 8, 50, 9.5));
//        ExtrasUtil.downloadTest();
    }

    @Override
    public void onTerminate() {
        Log.e(LABEL, NameUtil.context());
        System.out.println("MGMapViewer Application stop");
        super.onTerminate();
    }

    protected void initFinished(){
        initFinished = true;
        if (recordingTrackLogObservable.getTrackLog() != null){
            recordingTrackLogObservable.getTrackLog().setRecordRaw(true); // from now on record new entries in the tracklog
            if (recordingTrackLogObservable.getTrackLog().isSegmentRecording()){
                gpsOn.setValue(true);
                Intent intent = new Intent(this, TrackLoggerService.class);
                startService(intent);

                TrackLogSegment currentSegment = recordingTrackLogObservable.getTrackLog().getCurrentSegment();
                PointModel lastTlp = currentSegment.getLastPoint();
                if (centerCurrentPosition.getValue() && (lastTlp != null)){
                    lastPositionsObservable.handlePoint(lastTlp);
                }
            }
        }
    }



    public class LastPositionsObservable extends Observable{
        public PointModel lastGpsPoint = null;
        PointModel secondLastGpsPoint = null;

        public void handlePoint(PointModel lp){
            secondLastGpsPoint = lastGpsPoint;
            lastGpsPoint = lp;
            changed();
        }
        public void changed(){
            setChanged();
            notifyObservers();
        }
        public void clear(){
            lastGpsPoint = null;
            secondLastGpsPoint = null;
            changed();
        }
    }

    // TODO: Probably it makes sense to split this in a TrackLogObservable for the selected TrackLog and an AvailableTrackLogsObeservable without selected TrackLog information.
    @SuppressWarnings("WeakerAccess")
    public class AvailableTrackLogsObservable extends Observable{
        TrackLogRef noRef = new TrackLogRef(null,-1);
        public TreeSet<TrackLog> availableTrackLogs = new TreeSet<>(Collections.<TrackLog>reverseOrder());
        public TrackLogRef selectedTrackLogRef = noRef;

        public TreeSet<TrackLog> getAvailableTrackLogs(){
            return availableTrackLogs;
        }
        public TrackLogRef getSelectedTrackLogRef(){
            return selectedTrackLogRef;
        }

        public void removeAll(){
            availableTrackLogs.clear();
            selectedTrackLogRef = noRef;
            changed();
        }
        public void removeUnselected(){
            availableTrackLogs.clear();
            if (selectedTrackLogRef.getTrackLog() != null){
                availableTrackLogs.add(selectedTrackLogRef.getTrackLog());
            }
            changed();
        }
        public void removeSelected(){
            availableTrackLogs.remove(selectedTrackLogRef.getTrackLog());
            selectedTrackLogRef = noRef;
            changed();
        }
        public void setSelectedTrackLogRef(TrackLogRef ref){
            selectedTrackLogRef = ref;
            if (ref.getTrackLog() != null){
                availableTrackLogs.add(ref.getTrackLog());
            }
            changed();
        }
        public void changed(){
            setChanged();
            notifyObservers();
        }
    }


    @SuppressWarnings("WeakerAccess")
    public class TrackLogObservable<T extends TrackLog> extends Observable{
        T trackLog = null;

        public T getTrackLog(){
            return trackLog;
        }

        Observer proxyObserver = new Observer() {
            @Override
            public void update(Observable observable, Object o) {
                setChanged();
                notifyObservers(o);
            }
        };

        public void setTrackLog(T trackLog){
            if (this.trackLog != null){
                this.trackLog.deleteObserver(proxyObserver);
            }
            this.trackLog = trackLog;
            if (this.trackLog != null){
                this.trackLog.addObserver(proxyObserver);
            }
            setChanged();
            notifyObservers();
        }

        public void changed(){
            setChanged();
            notifyObservers();
        }
    }

    public class BooleanObservable extends Observable{
        private boolean value;

        public BooleanObservable(boolean initValue){
            this.value = initValue;
        }

        public void toggle(){
            setValue(!getValue());
        }

        public void setValue(boolean value) {
            this.value = value;
            changed();
        }

        public boolean getValue() {
            return value;
        }

        public void changed(){
            setChanged();
            notifyObservers();
        }
    }



    public final LastPositionsObservable lastPositionsObservable = new LastPositionsObservable();
    public final AvailableTrackLogsObservable availableTrackLogsObservable = new AvailableTrackLogsObservable();
    public final TrackLogObservable<RecordingTrackLog> recordingTrackLogObservable = new TrackLogObservable<>();
    public final TrackLogObservable<WriteableTrackLog> markerTrackLogObservable = new TrackLogObservable<>();
    public final TreeSet<TrackLog> metaTrackLogs = new TreeSet<>(Collections.<TrackLog>reverseOrder());

    public BooleanObservable gpsOn = new BooleanObservable(false);;
    public BooleanObservable centerCurrentPosition = new BooleanObservable(true);
    public BooleanObservable wayDetails = new BooleanObservable(false);
    public BooleanObservable showAlphaSliders = new BooleanObservable(false);
    public BooleanObservable editMarkerTrack = new BooleanObservable(false);
    public BooleanObservable showRouting = new BooleanObservable(true);
    public BooleanObservable stlWithGL = new BooleanObservable(true);
    public BooleanObservable fullscreen = new BooleanObservable(true);
    public BooleanObservable searchOn = new BooleanObservable(false);

    boolean initFinished = false;
    ArrayList<MGMicroService> microServices = new ArrayList<>();
    private ArrayList<BgJob> bgJobs = new ArrayList<>();

    /** queue for new (unhandled) TrackLogPoint objects */
    private ArrayBlockingQueue<PointModel> logPoints2process = new ArrayBlockingQueue<>(5000);

    /** Retruen the mirco service by type  - duplicate code of MGMapActivity, but here it is also available for other activities */
    public <T> T getMS(Class<T> tClass){
        for (MGMicroService service : microServices){
            if (tClass.isInstance(service)) return (T)service;
        }
        return null;
    }


    public void addTrackLogPoint(final PointModel pointModel){
        if (pointModel != null){
            try {
                Log.i(LABEL, NameUtil.context() +" tlp="+pointModel+" height="+ pointModel.getEleA());
                logPoints2process.add(pointModel);
                lastPositionsObservable.handlePoint(pointModel);
            } catch (Exception e) {
                Log.e(LABEL, NameUtil.context()+" addPoint: "+e.getMessage(),e);
            }
        }
    }

    public float pressure = 0;


    public synchronized void addBgJobs(ArrayList<? extends BgJob> jobs){
        bgJobs.addAll(jobs);
        Intent intent = new Intent(this, BgJobService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(intent);
        } else {
            this.startService(intent);
        }
    }
    public synchronized BgJob getBgJob(){
        if (bgJobs.size() > 0){
            return bgJobs.remove(0);
        }
        return null;
    }

    public synchronized int numBgJobs(){
        return bgJobs.size();
    }

    public void refresh(){
        if (!microServices.isEmpty()){
            MGMicroService ms = microServices.get(0);
            MapView mapView = ms.getMapView();

            IMapViewPosition mvp = mapView.getModel().mapViewPosition;
            mvp.setMapPosition(new MapPosition(mvp.getCenter(), mvp.getZoomLevel()));
        }
    }
}
