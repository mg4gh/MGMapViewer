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

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.util.Log;

import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.util.OpenAndroMapsUtil;
import mg.mgmap.activity.statistic.TrackStatisticFilter;
import mg.mgmap.application.util.GpsSupervisorWorker;
import mg.mgmap.application.util.HgtProvider;
import mg.mgmap.application.util.NotificationUtil;
import mg.mgmap.generic.util.BgJobGroup;
import mg.mgmap.generic.util.BgJobGroupCallback;
import mg.mgmap.generic.util.hints.HintUtil;
import mg.mgmap.service.bgjob.BgJobService;
import mg.mgmap.R;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLogRef;
import mg.mgmap.service.location.TrackLoggerService;
import mg.mgmap.test.TestControl;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.application.util.GeoidProvider;
import mg.mgmap.generic.util.gpx.GpxExporter;
import mg.mgmap.application.util.MetaDataUtil;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.activity.mgmap.features.rtl.RecordingTrackLog;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.application.util.ExtrasUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;


import java.io.File;
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
 */
public class MGMapApplication extends Application {

    // Label for Logging.
    public static final String LABEL = "MGMap";
    private Process pLogcat = null;

    private HgtProvider hgtProvider;
    private ElevationProvider elevationProvider;
    private GeoidProvider geoidProvider;
    private PersistenceManager persistenceManager;
    private MetaDataUtil metaDataUtil;
    private TestControl testControl;
    private NotificationUtil notificationUtil;
    private TrackStatisticFilter trackStatisticFilter;
    private HintUtil hintUtil;

    public final LastPositionsObservable lastPositionsObservable = new LastPositionsObservable();
    public final AvailableTrackLogsObservable availableTrackLogsObservable = new AvailableTrackLogsObservable();
    public final TrackLogObservable<RecordingTrackLog> recordingTrackLogObservable = new TrackLogObservable<>(true);
    public final TrackLogObservable<WriteableTrackLog> markerTrackLogObservable = new TrackLogObservable<>(false);
    public final TrackLogObservable<WriteableTrackLog> routeTrackLogObservable = new TrackLogObservable<>(true);
    public final TreeMap<String, TrackLog> metaTrackLogs = new TreeMap<>(Collections.reverseOrder());

    /** queue for new (unhandled) TrackLogPoint objects */
    public final ArrayBlockingQueue<PointModel> logPoints2process = new ArrayBlockingQueue<>(5000);

    private final ArrayList<BgJob> bgJobs = new ArrayList<>();
    private final ArrayList<BgJob> activeBgJobs = new ArrayList<>();

    PrefCache prefCache = null;
    public Pref<Boolean> prefRestart = null; // property to distinguish ApplicationStart from ActivityRecreate
    public Pref<Boolean> prefGps = null;

    public void startLogging(File logDir){
        try {
            String cmd = "logcat "+ LABEL+":i *:W -f "+logDir.getAbsolutePath()+"/log.txt -r 10000 -n10";
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

        persistenceManager = new PersistenceManager(this);
        startLogging(persistenceManager.getLogDir());
        AndroidGraphicFactory.createInstance(this);
        prefCache = new PrefCache(this);

        hgtProvider = new HgtProvider(persistenceManager, getAssets()); // for hgt file handling
        elevationProvider = new ElevationProvider(hgtProvider); // for height data handling
        geoidProvider = new GeoidProvider(this); // for difference between wgs84 and nmea elevation
        metaDataUtil = new MetaDataUtil(persistenceManager);
        testControl = new TestControl(this, prefCache);
        notificationUtil = new NotificationUtil(this);
        trackStatisticFilter = new TrackStatisticFilter(prefCache);
        hintUtil = new HintUtil();

        prefRestart = prefCache.get(R.string.MGMapApplication_pref_Restart, true);
        prefGps = prefCache.get(R.string.FSPosition_pref_GpsOn, false);
        prefRestart.setValue(true);
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
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" init finished!");
        }).start();



        // initialize Theme and MetaData (as used from AvailableTrackLogs service and statistic)
        new Thread(() -> {
            if (persistenceManager.getThemeNames().length == 0){
                BgJobGroup jobGroup = new BgJobGroup(this, null, null, new BgJobGroupCallback() {});
                jobGroup.addJob( OpenAndroMapsUtil.createBgJobsFromAssetTheme(persistenceManager, getAssets()) );
                jobGroup.setConstructed(null);
            }
            ExtrasUtil.checkCreateMeta(persistenceManager, metaDataUtil, elevationProvider);
            for (TrackLog trackLog : metaDataUtil.loadMetaData()){
                trackStatisticFilter.checkFilter(trackLog);
                metaTrackLogs.put(trackLog.getNameKey(),trackLog);
            }
        }).start();

        // initialize handling of new points from TrackLoggerService
        new Thread(() -> {
            Log.i(LABEL, NameUtil.context()+"handle TrackLogPoints: started ");
            while (true){
                try {
                    PointModel pointModel = logPoints2process.take();
                    Log.i(LABEL, NameUtil.context() +" handle tlp="+pointModel);
                    if (pointModel != null){
                        if (recordingTrackLogObservable.getTrackLog() != null){
                            recordingTrackLogObservable.getTrackLog().addPoint(pointModel);
                            Log.v(LABEL, NameUtil.context()+ "handle TrackLogPoints: Processed "+pointModel);
                        }
                        if (logPoints2process.size() == 0){ // trigger lastPositionsObservable only for the latest point in the queue
                            lastPositionsObservable.handlePoint(pointModel);
                        }
                    }
                } catch (Exception e) {
                    Log.e(LABEL, NameUtil.context()+"handle TrackLogPoints: "+e.getMessage(),e);
                }
            }
        }).start();

        new Thread(() -> {
            long TIMEOUT = 10000;
            Log.i(LABEL, NameUtil.context()+"logcat supervision: start ");
            int cnt = 0;
            int escalationCnt = 0;
            long lastCheck = System.currentTimeMillis();
            while (true){
                try {
                    pLogcat.waitFor(TIMEOUT, TimeUnit.MILLISECONDS );
                    int ec = pLogcat.exitValue(); // normal execution will result in an IllegalStateException
                    Log.e(MGMapApplication.LABEL,NameUtil.context()+"  logcat supervision: logcat process terminated with exitCode "+ec+". Try to start again.");
                    startLogging(persistenceManager.getLogDir());
                    lastCheck = System.currentTimeMillis();
                } catch (Exception e) {
                    long now = System.currentTimeMillis();
                    if (prefGps.getValue() && ((now - lastCheck) > (TIMEOUT*1.5))){ // we might have detected an energy saving problem
                            Log.i(LABEL, NameUtil.context()+"Log supervision Timeout exceeded by factor 1.5; lastCheck="+lastCheck+" now="+now+" - is there an energy saving problem ?");
                            escalationCnt++;
                    } else {
                        escalationCnt = 0;
                    }
                    if (escalationCnt > 3){
                        Log.w(LABEL, NameUtil.context()+" try to notify user ...");
                        notificationUtil.notifyAlarm();
                    }
                    if (++cnt % 6 == 0){
                        Log.i(LABEL, NameUtil.context()+"logcat supervision: OK. (running "+(cnt/6)+" min)");
                    }
                    lastCheck = now;
                }
            }
        }).start();

    }

    @Override
    public void onTerminate() {
        Log.w(LABEL, NameUtil.context()+" MGMapViewer Application stop");
        try {
            NotificationManagerCompat.from(this).cancelAll();
            prefCache.cleanup();
        } catch (Exception e) {
            Log.e(LABEL, NameUtil.context(),e);
        }
        super.onTerminate();
    }

    public static class LastPositionsObservable extends Observable{
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
    public static class AvailableTrackLogsObservable extends Observable{
        TrackLogRef noRef = new TrackLogRef(null,-1);
        public TreeSet<TrackLog> availableTrackLogs = new TreeSet<>(Collections.reverseOrder());
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
        boolean addToMetaTrackLogs;

        public TrackLogObservable(boolean addToMetaTrackLogs){
            this.addToMetaTrackLogs = addToMetaTrackLogs;
        }

        public T getTrackLog(){
            return trackLog;
        }

        Observer proxyObserver = (observable, o) -> {
            setChanged();
            notifyObservers(o);
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

    public void startTrackLoggerService(Context context){
        Intent intent = new Intent(context, TrackLoggerService.class);
        this.startForegroundService(intent);
        Log.i(MGMapApplication.LABEL, NameUtil.context() + "prefGps="+prefGps.getValue());
        triggerGpsSupervisionWorker();
    }


    public void triggerGpsSupervisionWorker(){
        if (prefGps.getValue()){
            Log.i(MGMapApplication.LABEL, NameUtil.context() + "trigger OneTimeWorkRequest in 300s for GpsSupervisorWorker!");
            OneTimeWorkRequest oneTimeWorkRequest = new OneTimeWorkRequest.Builder(GpsSupervisorWorker.class)
                    .setInitialDelay(300, TimeUnit.SECONDS).build();
            String uniqueWokName = getApplicationContext().getString(R.string.unique_work_name);
            WorkManager.getInstance(this).enqueueUniqueWork(uniqueWokName, ExistingWorkPolicy.REPLACE, oneTimeWorkRequest);
        }
    }

    public void checkGpsStatus(){
        if (prefGps.getValue() && prefRestart.getValue()){ // if GPS is on ans restart flag is still set
            Intent intent = new Intent(this, MGMapActivity.class);
            this.startActivity(intent);
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

    public TestControl getTestControl() {
        return testControl;
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
            if (jobs.size() > 0){ // and if there are new jobs
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
        if (bgJobs.size() > 0){
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
        return ""+ totalBgJobs()+res;
    }

    public synchronized void addActiveJob(BgJob job){
        activeBgJobs.add(job);
    }
    public synchronized void removeActiveJob(BgJob job){
        activeBgJobs.remove(job);
    }

}
