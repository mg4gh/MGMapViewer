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
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

import android.util.Log;

import org.mapsforge.core.util.Parameters;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;

import mg.mgmap.activity.mgmap.util.OpenAndroMapsUtil;
import mg.mgmap.service.bgjob.BgJobService;
import mg.mgmap.R;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLogRef;
import mg.mgmap.service.location.TrackLoggerService;
import mg.mgmap.test.TestControl;
import mg.mgmap.application.util.AltitudeProvider;
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

    private AltitudeProvider altitudeProvider;
    private GeoidProvider geoidProvider;
    private PersistenceManager persistenceManager;
    private MetaDataUtil metaDataUtil;
    private TestControl testControl;

    public final LastPositionsObservable lastPositionsObservable = new LastPositionsObservable();
    public final AvailableTrackLogsObservable availableTrackLogsObservable = new AvailableTrackLogsObservable();
    public final TrackLogObservable<RecordingTrackLog> recordingTrackLogObservable = new TrackLogObservable<>(true);
    public final TrackLogObservable<WriteableTrackLog> markerTrackLogObservable = new TrackLogObservable<>(false);
    public final TrackLogObservable<WriteableTrackLog> routeTrackLogObservable = new TrackLogObservable<>(true);
    public final TreeMap<String, TrackLog> metaTrackLogs = new TreeMap<>(Collections.reverseOrder());

    /** queue for new (unhandled) TrackLogPoint objects */
    public final ArrayBlockingQueue<PointModel> logPoints2process = new ArrayBlockingQueue<>(5000);

    private final ArrayList<BgJob> bgJobs = new ArrayList<>();

    PrefCache prefCache = null;
    Pref<Boolean> prefAppRestart = null; // property to distinguish ApplicationStart from ActivityRecreate
    Pref<Boolean> prefGps = null; // property to distinguish ApplicationStart from ActivityRecreate

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

        altitudeProvider = new AltitudeProvider(persistenceManager); // for hgt data handling
        geoidProvider = new GeoidProvider(this); // for difference between wgs84 and nmea altitude
        metaDataUtil = new MetaDataUtil(persistenceManager);
        testControl = new TestControl(this, prefCache);

        prefAppRestart = prefCache.get(R.string.MGMapApplication_pref_Restart, true);
        prefGps = prefCache.get(R.string.FSPosition_pref_GpsOn, false);
        prefAppRestart.setValue(true);
        prefGps.setValue(false);

        Parameters.LAYER_SCROLL_EVENT = true; // needed to support drag and drop of marker points

        // Recover state of RecordingTrackLog
        new Thread(){
            @Override
            public void run() {
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
                        startTrackLoggerService();

                        PointModel lastTlp = rtl.getCurrentSegment().getLastPoint();
                        if (lastTlp != null){
                            lastPositionsObservable.handlePoint(lastTlp);
                        }
                    }
                }
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" init finished!");
            }
        }.start();



        // initialize Theme and MetaData (as used from AvailableTrackLogs service and statistic)
        new Thread(){
            @Override
            public void run() {
                if (persistenceManager.getThemeNames().length == 0){
                    addBgJobs( OpenAndroMapsUtil.createBgJobsFromAssetTheme(persistenceManager, getAssets()) );
                }
                ExtrasUtil.checkCreateMeta(persistenceManager, metaDataUtil, altitudeProvider);
                for (TrackLog trackLog : metaDataUtil.loadMetaData()){
                    metaTrackLogs.put(trackLog.getNameKey(),trackLog);
                }
            }
        }.start();

        // initialize handling of new points from TrackLoggerService
        new Thread(){
            @Override
            public void run() {
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
            }
        }.start();

        new Thread(){
            @Override
            public void run() {
                long TIMEOUT = 10000;
                Log.i(LABEL, NameUtil.context()+"logcat supervision: start ");
                int cnt = 0;
                long lastCheck = System.currentTimeMillis();
                while (true){
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            pLogcat.waitFor(TIMEOUT, TimeUnit.MILLISECONDS );
                        } else {
                            Thread.sleep(TIMEOUT);
                        }
                        int ec = pLogcat.exitValue(); // normal execution will result in an IllegalStateException
                        Log.e(MGMapApplication.LABEL,NameUtil.context()+"  logcat supervision: logcat process terminated with exitCode "+ec+". Try to start again.");
                        startLogging(persistenceManager.getLogDir());
                        lastCheck = System.currentTimeMillis();
                    } catch (Exception e) {
                        long now = System.currentTimeMillis();
                        if ((now - lastCheck) > (TIMEOUT*1.5)){ // we might have detected an energy saving problem
                            Log.w(LABEL, NameUtil.context()+"Log supervision Timeout exceeded by factor 1.5; lastCheck="+lastCheck+" now="+now+" - is there an energy saving problem ?");
                            if (prefGps.getValue()){
                                Log.i(LABEL, NameUtil.context()+" try to trigger gps ...");
                                startTrackLoggerService();
                            }
                        }
                        if (++cnt % 6 == 0){
                            Log.i(LABEL, NameUtil.context()+"logcat supervision: OK. (running "+(cnt/6)+" min)");
                        }
                        lastCheck = now;
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

    public void startTrackLoggerService(){
        Intent intent = new Intent(this, TrackLoggerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            this.startForegroundService(intent);
        } else {
            this.startService(intent);
        }
    }

    public AltitudeProvider getAltitudeProvider() {
        return altitudeProvider;
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
}
