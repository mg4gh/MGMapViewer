package mg.mapviewer;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;
import androidx.preference.PreferenceManager;
import android.util.Log;

import org.mapsforge.core.model.MapPosition;
import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.model.IMapViewPosition;

import mg.mapviewer.features.routing.MSRoutingHintService;
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
import mg.mapviewer.util.MGPref;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The application of the MgMapActivity.
 * Mainly it provides some Observable objects that represent the application state.
 * These state objects ensure that the applications state survive a restart of the activity.
 * Second this decouples the sensors, which change the model objects from the services, which act depending on these changes.
 */
public class MGMapApplication extends Application {

    // Label for Logging.
    public static final String LABEL = "MGMapViewer";
    private Process pLogcat = null;

    public final LastPositionsObservable lastPositionsObservable = new LastPositionsObservable();
    public final AvailableTrackLogsObservable availableTrackLogsObservable = new AvailableTrackLogsObservable();
    public final TrackLogObservable<RecordingTrackLog> recordingTrackLogObservable = new TrackLogObservable<>();
    public final TrackLogObservable<WriteableTrackLog> markerTrackLogObservable = new TrackLogObservable<>();
    public final TrackLogObservable<WriteableTrackLog> routeTrackLogObservable = new TrackLogObservable<>();
    public final TreeMap<String, TrackLog> metaTrackLogs = new TreeMap<>(Collections.reverseOrder());


    boolean initFinished = false;
    private MGMapActivity mgMapActivity = null;
    ArrayList<MGMicroService> microServices = new ArrayList<>();
    private ArrayList<BgJob> bgJobs = new ArrayList<>();

    MGPref<Boolean> prefAppRestart = null; // property to distinguish ApplicationStart from ActivityRecreate
    MGPref<Boolean> prefGpsOn = null; // property to distinguish ApplicationStart from ActivityRecreate

    public void startLogging(){
        try {
            String cmd = "logcat "+ LABEL+":i -f "+PersistenceManager.getInstance(this).getLogDir().getAbsolutePath()+"/log.txt -r 10000 -n10";
            Log.i(LABEL, NameUtil.context()+" Start Logging: "+cmd);
            pLogcat = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            Log.e(LABEL, NameUtil.context(),e);
        }
        Log.i(LABEL,NameUtil.context()+" Starting Logger finished.");

        Log.v(MGMapApplication.LABEL, NameUtil.context());
        Log.d(MGMapApplication.LABEL, NameUtil.context());
        Log.i(MGMapApplication.LABEL, NameUtil.context());
        Log.w(MGMapApplication.LABEL, NameUtil.context());
        Log.e(MGMapApplication.LABEL, NameUtil.context());
    }

    @Override
    public void onCreate() {
        System.out.println("MGMapViewer Application start!!!!");
        super.onCreate();

        startLogging();
        AndroidGraphicFactory.createInstance(this);
        ExtrasUtil.checkCreateMeta();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        MGPref.init(this);
        prefAppRestart = MGPref.get(R.string.MGMapApplication_pref_Restart, true);
        prefGpsOn = MGPref.get(R.string.MSPosition_prev_GpsOn, false);
        prefAppRestart.setValue(true);
        prefGpsOn.setValue(false);
        MGPref.get(R.string.MSSearch_qc_showSearchResult, false).setValue(false);

        Parameters.LAYER_SCROLL_EVENT = true; // needed to support drag and drop of marker points

        int[] prefIds = new int[]{
                R.string.Layers_pref_chooseMap1_key,
                R.string.Layers_pref_chooseMap2_key,
                R.string.Layers_pref_chooseMap3_key,
                R.string.Layers_pref_chooseMap4_key,
                R.string.Layers_pref_chooseMap5_key};
        for (int id : prefIds){
            mapLayerKeys.add( getResources().getString( id ));
        }



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
                for (TrackLog trackLog : MetaDataUtil.loadMetaData()){
                    metaTrackLogs.put(trackLog.getNameKey(),trackLog);
                }
                return null;
            }
        }.execute();

        // initialize handling of new points from TrackLoggerService
        new Thread(){
            @Override
            public void run() {
                Log.i(LABEL, NameUtil.context()+"handle TrackLogPoints: started ");
                while (true){
                    try {
                        PointModel pointModel = logPoints2process.take();
                        if (recordingTrackLogObservable.getTrackLog() != null){
                            recordingTrackLogObservable.getTrackLog().addPoint(pointModel);
                            Log.v(LABEL, NameUtil.context()+ "handle TrackLogPoints: Processed "+pointModel);
                        }
                    } catch (Exception e) {
                        Log.e(LABEL, NameUtil.context()+"handle TrackLogPoints: "+e.getMessage(),e);
                    }
                }
            }
        }.start();

        new Thread(){
            @Override
            public void run() {
                Log.i(LABEL, NameUtil.context()+"logcat supervision: start ");
                while (true){
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            pLogcat.waitFor(30, TimeUnit.SECONDS );
                        } else {
                            Thread.sleep(30000);
                        }

                        int ec = pLogcat.exitValue(); // normal execution will result in an IllegalStateException
                        Log.e(MGMapApplication.LABEL,NameUtil.context()+"  logcat supervision: logcat process terminated with exitCode "+ec+". Try to start again.");
                        startLogging();
                    } catch (Exception e) {
                        Log.v(LABEL, NameUtil.context()+"logcat supervision: "+e.getMessage());
                    }
                }
            }
        }.start();

    }

    @Override
    public void onTerminate() {
        Log.w(LABEL, NameUtil.context()+" MGMapViewer Application stop");
        try {
            NotificationManagerCompat.from(this).cancelAll();
        } catch (Exception e) {
            Log.e(LABEL, NameUtil.context(),e);
        }
        super.onTerminate();
    }

    protected void initFinished(){
        initFinished = true;
        if (recordingTrackLogObservable.getTrackLog() != null){
            recordingTrackLogObservable.getTrackLog().setRecordRaw(true); // from now on record new entries in the tracklog
            if (recordingTrackLogObservable.getTrackLog().isSegmentRecording()){
                prefGpsOn.setValue(true);
                Intent intent = new Intent(this, TrackLoggerService.class);
                startService(intent);

                TrackLogSegment currentSegment = recordingTrackLogObservable.getTrackLog().getCurrentSegment();
                PointModel lastTlp = currentSegment.getLastPoint();
                if (/* centerCurrentPosition.getValue() && */ (lastTlp != null)){
                    lastPositionsObservable.handlePoint(lastTlp);
                }
            }
        }
    }



    public class LastPositionsObservable extends Observable{
        public PointModel lastGpsPoint = null;
        public PointModel secondLastGpsPoint = null;

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
                metaTrackLogs.put(trackLog.getNameKey(), trackLog);
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




    /** queue for new (unhandled) TrackLogPoint objects */
    private ArrayBlockingQueue<PointModel> logPoints2process = new ArrayBlockingQueue<>(5000);

    private ArrayList<String> mapLayerKeys = new ArrayList<>();

    public ArrayList<String> getMapLayerKeys() {
        return mapLayerKeys;
    }

    public MGMapActivity getMgMapActivity() {
        return mgMapActivity;
    }

    public void setMgMapActivity(MGMapActivity mgMapActivity) {
        this.mgMapActivity = mgMapActivity;
        if (mgMapActivity == null){
            //cleanup according to termination of MGMapActivity
            microServices.clear();
            availableTrackLogsObservable.deleteObservers();
            recordingTrackLogObservable.deleteObservers();
            markerTrackLogObservable.deleteObservers();
            routeTrackLogObservable.deleteObservers();
            lastPositionsObservable.deleteObservers();
        }
    }

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
                Log.i(LABEL, NameUtil.context() +" tlp="+pointModel);
                logPoints2process.add(pointModel);
                lastPositionsObservable.handlePoint(pointModel);
            } catch (Exception e) {
                Log.e(LABEL, NameUtil.context()+" addPoint: "+e.getMessage(),e);
            }
        }
    }

    public float pressure = 0;


    public synchronized void addBgJobs(List<BgJob> jobs){
        if (jobs == null) return;
        if (!bgJobs.isEmpty()){
            bgJobs.addAll(jobs);
        } else { // bgJobs is empty
            if (jobs.size() > 0){ // and if there are new jobs
                bgJobs.addAll(jobs);

                Intent intent = new Intent(this, BgJobService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.startForegroundService(intent);
                } else {
                    this.startService(intent);
                }
            }
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
