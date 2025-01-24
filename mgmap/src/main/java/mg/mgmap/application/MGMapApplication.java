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
package mg.mgmap.application;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.SystemClock;

import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import mg.mgmap.BuildConfig;
import mg.mgmap.activity.mgmap.features.rtl.RecordingTrackLog;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.util.BackupUtil;
import mg.mgmap.generic.util.CC;
import mg.mgmap.activity.statistic.TrackStatisticFilter;
import mg.mgmap.application.util.ActivityLifecycleAdapter;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImpl;
import mg.mgmap.application.util.ExtrasUtil;
import mg.mgmap.application.util.GeoidProvider;
import mg.mgmap.application.util.HgtProvider;
import mg.mgmap.application.util.MetaDataUtil;
import mg.mgmap.application.util.NotificationUtil;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogRef;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.ObservableImpl;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.gpx.GpxImporter;
import mg.mgmap.generic.util.hints.HintUtil;
import mg.mgmap.generic.util.gpx.GpxExporter;
import mg.mgmap.service.bgjob.BgJobService;
import mg.mgmap.service.location.BarometerListener;
import mg.mgmap.service.location.TrackLoggerService;
import mg.mgmap.R;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The application of the MgMapActivity.
 * Mainly it provides some Observable objects that represent the application state.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class MGMapApplication extends Application {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    // Label for Logging.
    public static final String LABEL = "mg.mgmap";
    private Process pLogcat = null;

    private HgtProvider hgtProvider;
    private ElevationProvider elevationProvider;
    private GeoidProvider geoidProvider;
    private PersistenceManager persistenceManager;
    private MetaDataUtil metaDataUtil;
    private NotificationUtil notificationUtil;
    private TrackStatisticFilter trackStatisticFilter;
    private HintUtil hintUtil;

    public final LastPositionsObservable lastPositionsObservable = new LastPositionsObservable();
    public final AvailableTrackLogsObservable availableTrackLogsObservable = new AvailableTrackLogsObservable();
    public final TrackLogObservable<RecordingTrackLog> recordingTrackLogObservable = new TrackLogObservable<>("recordingTrackLog",true);
    public final TrackLogObservable<WriteableTrackLog> markerTrackLogObservable = new TrackLogObservable<>("markerTrackLog",false);
    public final TrackLogObservable<WriteableTrackLog> routeTrackLogObservable = new TrackLogObservable<>("routeTrackLog",true);
    public final Map<String, TrackLog> metaTrackLogs = Collections.synchronizedMap( new TreeMap<>(Collections.reverseOrder()) );

    /** queue for new (unhandled) TrackLogPoint objects */
    public final ArrayBlockingQueue<PointModel> logPoints2process = new ArrayBlockingQueue<>(5000);

    private final ArrayList<BgJob> bgJobs = new ArrayList<>();
    private final ArrayList<BgJob> activeBgJobs = new ArrayList<>();

    PrefCache prefCache = null;
    public Pref<Boolean> prefGps = null; // gps target state
    public Pref<Boolean> prefGpsState = new Pref<>(false); // gps current state

    private Setup setup;
    public BaseConfig baseConfig = null;
    public volatile UUID currentRun = null;

    public void startLogging(File logDir){
        try {
            String cmd = "logcat *:"+(BuildConfig.DEBUG?"d":"i")+" mg.mgmap:E -f "+logDir.getAbsolutePath()+"/log.txt -r 10000 -n10";
            mgLog.i("Start Logging: "+cmd);
            pLogcat = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            mgLog.e(e);
        }
        mgLog.i("Starting Logger finished.");

        mgLog.v("");
        mgLog.d("");
        mgLog.i("");
        mgLog.w("");
        mgLog.e("");
    }

    @Override
    public void onCreate() {
        System.out.println("***********************************************************************************************************************");
        System.out.println("****************************************** MGMapViewer Application start!!!! ******************************************");
        System.out.println("***********************************************************************************************************************");
        super.onCreate();

        MGLog.logConfig.put("mg.mgmap", BuildConfig.DEBUG? MGLog.Level.DEBUG:MGLog.Level.INFO);
//        MGLog.logConfig.put("mg.mgmap.test.TestControl", MGLog.Level.VERBOSE);
//        MGLog.logConfig.put("mg.mgmap.activity.mgmap.MultiMapDataStore", MGLog.Level.VERBOSE);
        mgLog.evaluateLevel();

        setup = new Setup(this);
        setup.wantSetup(Setup.WANTED_DEFAULT, getAssets());
    }

    void _init(BaseConfig baseConfig){

        this.baseConfig = baseConfig;

        persistenceManager = new PersistenceManager(this, baseConfig.appDirName());
        startLogging(persistenceManager.getLogDir());
        CC.init(this);
        AndroidGraphicFactory.createInstance(this);
        File svgCacheDir = new File(getCacheDir(), "svgCache");
        if (!svgCacheDir.mkdirs()) mgLog.e("create svgCacheDir failed: "+svgCacheDir.getAbsolutePath());
        AndroidGraphicFactory.INSTANCE.setSvgCacheDir(svgCacheDir);
        prefCache = new PrefCache(this);

        initVersionCode();
        BackupUtil.restore(this, persistenceManager);
        hgtProvider = new HgtProvider(this, persistenceManager, getAssets()); // for hgt file handling
        elevationProvider = new ElevationProviderImpl(hgtProvider); // for height data handling
        geoidProvider = new GeoidProvider(this); // for difference between wgs84 and nmea elevation
        metaDataUtil = new MetaDataUtil(this, persistenceManager);
        notificationUtil = new NotificationUtil(this);
        trackStatisticFilter = new TrackStatisticFilter(prefCache);
        hintUtil = new HintUtil();

        prefGps = prefCache.get(R.string.FSPosition_pref_GpsOn, false);
        prefGps.setValue(false);

        Parameters.LAYER_SCROLL_EVENT = true; // needed to support drag and drop of marker points

        // Recover state of RecordingTrackLog
        new Thread(() -> {
            RecordingTrackLog rtl = RecordingTrackLog.initFromRaw(persistenceManager);
            if ((rtl != null) && !rtl.isTrackRecording()){ // either finished or not yet started
                if (rtl.getNumberOfSegments() > 0){  // is finished
                    GpxExporter.export(persistenceManager, rtl);
                }
                persistenceManager.clearRaw();
                rtl = null;
            }
            recordingTrackLogObservable.setTrackLog(rtl);

            if (rtl != null){
                rtl.setRecordRaw(true); // from now on record new entries in the tracklog
                if (rtl.isSegmentRecording()){
                    prefGps.setValue(true);

                    PointModel lastTlp = rtl.getCurrentSegment().getLastPoint();
                    if (lastTlp != null){
                        lastPositionsObservable.handlePoint(lastTlp);
                    }
                }
            }
            mgLog.i("init finished!");
        }).start();

        if (persistenceManager.isFirstRun()){ // initialize lastFullBackupTime with install time - otherwise the first backup request would appear almost directly after installation.
            prefCache.get(R.string.preferences_last_full_backup_time, 0L).setValue(System.currentTimeMillis());
        }
        BackupUtil.restore2(this, persistenceManager, true); // restore backup_latest.zip (if exists)
        BackupUtil.restore2(this, persistenceManager, false); // restore backup_full.zip (if exists) (Remark: there is no use case, where both backup files have to be restored in the same run)

        // initialize MetaData (as used from AvailableTrackLogs service and statistic)
        new Thread(() -> {
            UUID uuid = currentRun;
            Pref<Boolean> prefMetaLoading = prefCache.get(R.string.MGMapApplication_pref_MetaData_loading, true);
            prefMetaLoading.setValue(true);
            checkCreateLoadMetaData(false);
            prefMetaLoading.setValue(false);
        }).start();

        // initialize handling of new points from TrackLoggerService
        new Thread(() -> {
            mgLog.i("handle TrackLogPoints: started ");
            UUID uuid = currentRun;
            while (uuid == currentRun){
                try {
                    PointModel pointModel = logPoints2process.take();
                    if (uuid != currentRun){
                        break; // leave Thread
                    }
                    if ((pointModel != null) && (!pointModel.equals(new PointModelImpl()))){
                        mgLog.i("handle tlp="+pointModel);
                        if (recordingTrackLogObservable.getTrackLog() != null){
                            recordingTrackLogObservable.getTrackLog().addPoint(pointModel);
                            mgLog.v("handle TrackLogPoints: Processed "+pointModel);
                        }
                        if (logPoints2process.isEmpty()){ // trigger lastPositionsObservable only for the latest point in the queue
                            lastPositionsObservable.handlePoint(pointModel);
                        }
                    }
                } catch (Exception e) {
                    mgLog.e(e);
                }
            }
        }).start();

        // supervise logging and check timing behaviour, escalate if necessary
        new Thread(() -> {
            ActivityLifecycleAdapter activityLifecycleAdapter = null;
            try{
                long TIMEOUT = 10000;
                mgLog.i("logcat supervision: start ");
                UUID uuid = currentRun;
                int cnt = 0;
                final int[] escalationCnt = {0,(int)(SensorManager.PRESSURE_STANDARD_ATMOSPHERE*1000),0,0}; // escalationCnt, last pressure*1000, barometerSensorChangedCnt, pressure*1000
                BarometerListener.setEscalationCnt(escalationCnt);
                activityLifecycleAdapter = new ActivityLifecycleAdapter() {
                    @Override
                    public void onActivityResumed(@NonNull Activity activity) {
                        super.onActivityResumed(activity);
                        escalationCnt[0] =  0; // escalation is reset whenever any activity is resumed
                        finishAlarm();
                    }
                };
                registerActivityLifecycleCallbacks(activityLifecycleAdapter);
                long lastCheck = System.currentTimeMillis();
                while (uuid == currentRun){
                    try {
                        pLogcat.waitFor(TIMEOUT, TimeUnit.MILLISECONDS );
                        if (uuid != currentRun){
                            break; // leave Thread
                        }
                        int ec = pLogcat.exitValue(); // normal execution will result in an IllegalStateException
                        synchronized (MGMapApplication.class){
                            MGMapApplication.class.wait(1000);
                        }
                        mgLog.e("logcat supervision: logcat process terminated with exitCode "+ec+". Try to start again.");
                        startLogging(persistenceManager.getLogDir());
                        lastCheck = System.currentTimeMillis();
                    } catch (Exception e) {
                        long now = System.currentTimeMillis();
                        if (prefGps.getValue() && (recordingTrackLogObservable.getTrackLog() != null) && ((now - lastCheck) > (TIMEOUT*1.5))){ // we might have detected an energy saving problem
                            mgLog.i("Log supervision Timeout exceeded by factor 1.5; lastCheck="+lastCheck+" now="+now);
                            if (escalationCnt[2] != 0){ // there is a new pressure changed event
                                float lastHeight = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE,  escalationCnt[1]/1000f);
                                float currentHeight = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE,  escalationCnt[3]/1000f);
                                if (Math.abs(lastHeight-currentHeight) > 5){ // there is some height movement as indicator that position is changing
                                    escalationCnt[0]++;
                                    escalationCnt[1] = escalationCnt[3]; // set new last height value
                                    mgLog.i("escalationCnt="+ Arrays.toString(escalationCnt));
                                }
                            }
                        } else {
                            escalationCnt[0] = 0;
                        }
                        if (escalationCnt[0] > 3){
                            mgLog.w("try to notify user ...");
                            notifyAlarm();
                        }
                        if (++cnt % 6 == 0){
                            mgLog.i("logcat supervision: OK. (running "+(cnt/6)+" min)");
                        }
                        lastCheck = now;
                        escalationCnt[2] = 0; // reset cnt of pressure change events
                    }
                }

            } finally {
                unregisterActivityLifecycleCallbacks(activityLifecycleAdapter);
            }
        }).start();

        mgLog.i("done.");
    }

    public void checkCreateLoadMetaData(boolean onlyNew){
        File restoreJob = new File(persistenceManager.getRestoreDir(), "restore.job");
        while (restoreJob.exists()){ // don't start as restore may add files
            SystemClock.sleep(1000);
        }
        ArrayList<String> newNames = ExtrasUtil.checkCreateMeta(this, this.currentRun);
    }

    public void addMetaDataTrackLog(TrackLog trackLog){
        trackStatisticFilter.checkFilter(trackLog);
        metaTrackLogs.put(trackLog.getNameKey(),trackLog);
    }

    void cleanup(){
        UUID lastRun = currentRun;
        currentRun = UUID.randomUUID();
        mgLog.i("do cleanup now. lastRun="+lastRun+" currentRun="+currentRun);
        recordingTrackLogObservable.setTrackLog(null);
        markerTrackLogObservable.setTrackLog(null);
        routeTrackLogObservable.setTrackLog(null);
        availableTrackLogsObservable.removeAll();
        lastPositionsObservable.clear();
        if (lastRun != null){
            SystemClock.sleep(20);
        }
        metaTrackLogs.clear();
        if (hgtProvider != null) hgtProvider.cleanup();

        logPoints2process.add(new PointModelImpl()); // abort Thread for TrackLogPoint handling
        if (pLogcat != null) pLogcat.destroy(); // abort logcat and als Logcat supervision thread

        if (baseConfig != null){
            if (!baseConfig.appDirName().equals(Setup.APP_DIR_DEFAULT)){
                File appDir = new File(getExternalFilesDir(null),baseConfig.appDirName());
                PersistenceManager.deleteRecursivly(appDir);
                baseConfig.sharedPreferences().edit().clear().apply();
            }
        }
        if (prefCache != null) prefCache.cleanup();
        NotificationManagerCompat.from(this).cancelAll();
    }

    @Override
    public void onTerminate() {
        mgLog.w("MGMapViewer Application stop");
        try {
            cleanup();
        } catch (Exception e) {
            mgLog.e(e);
        }
        super.onTerminate();
    }

    public static class LastPositionsObservable extends ObservableImpl {

        public LastPositionsObservable(){
            super("lastPosition");
        }
        public PointModel lastGpsPoint = null;
        public PointModel secondLastGpsPoint = null;

        public void handlePoint(PointModel lp){
            secondLastGpsPoint = lastGpsPoint;
            lastGpsPoint = lp;
            changed();
        }
        public void clear(){
            lastGpsPoint = null;
            secondLastGpsPoint = null;
            changed();
        }
    }

    // TODO: Probably it makes sense to split this in a TrackLogObservable for the selected TrackLog and an AvailableTrackLogsObeservable without selected TrackLog information.
    @SuppressWarnings("WeakerAccess")
    public class AvailableTrackLogsObservable extends ObservableImpl{
        public AvailableTrackLogsObservable(){
            super("availableTrackLogs");
        }
        TrackLogRef noRef = new TrackLogRef(null,-1);
        public Set<TrackLog> availableTrackLogs = Collections.synchronizedSet(new TreeSet<>(Collections.reverseOrder()));
        public TrackLogRef selectedTrackLogRef = noRef;

        public Set<TrackLog> getAvailableTrackLogs(){
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
        public void setSelectedTrackLogRef(final TrackLogRef ref){
            if (ref.getTrackLog() != null){
                TrackLog trackLog = ref.getTrackLog();
                if ((metaTrackLogs.get(trackLog.getNameKey()) != null)
                        && (trackLog.getTrackStatistic().getNumPoints()>1)
                        && (!(trackLog.getTrackLogSegment(0).getLastPoint() instanceof TrackLogPoint))){
                    String name = trackLog.getName();
                    new Thread(() -> { // trigger asynchronous reload of gpxTrackLog to ba have also timestamp/duration data available
                        try {
                            TrackLog gpxTrackLog = new GpxImporter(getElevationProvider())
                                    .parseTrackLog(name, getPersistenceManager().openGpxInput(name));
                            if (gpxTrackLog != null) {
                                gpxTrackLog.setName(name);
                                gpxTrackLog.setModified(false);
                                getMetaDataUtil().createMetaData(gpxTrackLog);
                                TrackLogRef gpxTrackLogRef = new TrackLogRef(gpxTrackLog, ref.getSegmentIdx());
                                availableTrackLogs.remove(ref.getTrackLog()); // remove old entry from availableTrackLogs, otherwise the gpxTrackLog will not be entered in the set
                                if (selectedTrackLogRef.getTrackLog().getNameKey().equals(gpxTrackLog.getNameKey())){
                                    setSelectedTrackLogRef(gpxTrackLogRef);
                                }
                            }
                        } catch (Exception e) {
                            mgLog.e(e);
                        }
                    }).start();
                }
                availableTrackLogs.add(ref.getTrackLog());
            }
            selectedTrackLogRef = ref;
            changed();
        }
    }


    @SuppressWarnings("WeakerAccess")
    public class TrackLogObservable<T extends TrackLog> extends ObservableImpl{
        T trackLog = null;
        boolean addToMetaTrackLogs;

        public TrackLogObservable(String observableName, boolean addToMetaTrackLogs){
            super(observableName);
            this.addToMetaTrackLogs = addToMetaTrackLogs;
        }

        public T getTrackLog(){
            return trackLog;
        }

        Observer proxyObserver = (e) -> {
            setChanged();
            notifyObservers();
        };

        public void setTrackLog(T trackLog){
            if (this.trackLog != trackLog){ // if the trackLog object is not changed, do nothing.
                if (this.trackLog != null){
                    this.trackLog.deleteObserver(proxyObserver);
                }
                this.trackLog = trackLog;
                if (this.trackLog != null){
                    this.trackLog.addObserver(proxyObserver);
                    if (addToMetaTrackLogs){
                        metaTrackLogs.put(trackLog.getNameKey(), trackLog);
                    }
                }
                setChanged();
                notifyObservers();
            }
        }

        public void changed(){
            setChanged();
            notifyObservers();
        }
    }

    public void startTrackLoggerService(Context context, boolean gpsTargetState){
        mgLog.i("gpsTargetState="+gpsTargetState+ " prefGpsState="+prefGpsState.getValue());
        if (gpsTargetState != prefGpsState.getValue()){
            Intent intent = new Intent(context, TrackLoggerService.class);
            this.startForegroundService(intent);
        }
    }

    public HgtProvider getHgtProvider() {
        return hgtProvider;
    }

    public ElevationProvider getElevationProvider() {
        return elevationProvider;
    }

    public GeoidProvider getGeoidProvider() {
        return geoidProvider;
    }

    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    public MetaDataUtil getMetaDataUtil() {
        return metaDataUtil;
    }

    public TrackStatisticFilter getTrackStatisticFilter() {
        return trackStatisticFilter;
    }

    public HintUtil getHintUtil() {
        return hintUtil;
    }

    public PrefCache getPrefCache(){
        return prefCache;
    }

    public SharedPreferences getSharedPreferences(){
        return baseConfig.sharedPreferences();
    }

    public String getPreferencesName() {
        return baseConfig.preferencesName();
    }

    public Setup getSetup() {
        return setup;
    }

    public static MGMapApplication getByContext(Context context){
        assert (context.getApplicationContext() instanceof MGMapApplication);
        return (MGMapApplication) context.getApplicationContext();
    }

    public synchronized void addBgJob(BgJob job){
        if (job == null) return;
        if (!bgJobs.isEmpty()){
            bgJobs.add(job);
        } else { // bgJobs is empty
            bgJobs.add(job);
            startBgService();
        }
    }
    public synchronized void addBgJobs(List<BgJob> jobs){
        if (jobs == null) return;
        if (!bgJobs.isEmpty()){
            bgJobs.addAll(jobs);
        } else { // bgJobs is empty
            if (!jobs.isEmpty()){ // and if there are new jobs
                bgJobs.addAll(jobs);
                startBgService();
            }
        }
    }

    private void startBgService(){
        Intent intent = new Intent(this, BgJobService.class);
        this.startForegroundService(intent);
    }

    public synchronized BgJob getBgJob(){
        if (!bgJobs.isEmpty()){
            return bgJobs.remove(0);
        }
        return null;
    }

    public synchronized int numBgJobs(){
        return bgJobs.size();
    }
    public synchronized int totalBgJobs(){
        return bgJobs.size()+activeBgJobs.size();
    }
    public synchronized String bgJobsStatistic(){
        String res = "";
        for (BgJob job : activeBgJobs){
            if ((job.getProgress() != 0) && (job.getMax() != 0)){
                res = " ("+(100-(job.getProgress()*100)/job.getMax())+"%)";
                break;
            }
        }
        return totalBgJobs() +res;
    }

    public synchronized void addActiveJob(BgJob job){
        activeBgJobs.add(job);
    }
    public synchronized void removeActiveJob(BgJob job){
        activeBgJobs.remove(job);
    }

    public void notifyAlarm(){
        notificationUtil.notifyAlarm();
    }
    public void finishAlarm(){
        notificationUtil.finishAlarm();
    }

    private void initVersionCode(){
        Pref<Integer> version = prefCache.get(R.string.MGMapApplication_pref_version, 0);
        if (version.getValue() != BuildConfig.VERSION_CODE){
            version.setValue(BuildConfig.VERSION_CODE);

            if (version.getValue() == 34){ // defaults change with version code 34
                prefCache.get(R.string.preferences_smoothing4routing_key, true).setValue(true);
                prefCache.get(R.string.preferences_routingProfile_key, true).setValue(true);
                prefCache.get(R.string.preferences_display_show_km_key, true).setValue(true);
            }
        }
    }
}
