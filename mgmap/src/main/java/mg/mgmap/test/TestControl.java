package mg.mgmap.test;

import android.animation.ObjectAnimator;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;

public class TestControl {


    private static final float MIN_X_PERCENT = 1f;
    private static final float MAX_X_PERCENT = 99f;
    private static final float MIN_Y_PERCENT = 1f;
    private static final float MAX_Y_PERCENT = 99f;

    TestView testView = null;
    PointF pPos = new PointF(); // percent position (of TestView) - useful for different screen sizes and resolutions, also for fullscreen on/off
    boolean cursorVisibility = false; // remember cursorVisibility
    Testcase currentTestcase = null; // remember current testcase

    public final Handler timer = new Handler(); // timer for tests
    private final Handler suTimer = new Handler(); // separate timer for supervision
    public MGMapApplication application;
    PrefCache prefCache; // access to shared preferences via applications prefCache (lifecycle of TestControl corresponds to application)
    Pref<Boolean> prefTestMode; // preference for testing mode (on/off)

    public TestControl(MGMapApplication application, PrefCache prefCache){
        this.application = application;
        this.prefCache = prefCache;


        prefCache.get(R.string.preference_testTrigger_key, false).addObserver((o, arg) -> { // observer to detect that test should start
            if (prefTestMode.getValue()){                   // test run is triggered
                new Thread(){                               // do this in background
                    @Override
                    public void run() {
                        LogMatcher.cleanup();               // logcat -c to clean old results that might confuse LogMatcher
                        prepareTestSetup();
                        try {
                            Thread.sleep(2500);       // give the logcat some time to finish
                        } catch (InterruptedException e) {
                            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
                        }
                        ArrayList<Testcase> testcases = new ArrayList<>();
                        String testSet = prefCache.get(R.string.preference_testSet_key, "").getValue();
                        new TestControlComposer(application).compose(TestControl.this, testSet, testcases); // fill the list of testcases according to the value of testSet
                        executeTestCases(testcases);       // finally process the testcases
                    }
                }.start();
            }
        });
        prefTestMode = prefCache.get(R.string.preference_testMode_key, false);
    }


    private void prepareTestSetup() { //drop files that are not on the reference list (create defined environment for start of test)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return; // setup doesn't work
        try{
            PersistenceManager pm = application.getPersistenceManager();
            File base = pm.getBaseDir();
            File filelist = new File(base, "filelist.txt");
            if (filelist.exists()) {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(filelist)));
                String line;
                ArrayList<String> refEntries = new ArrayList<>();
                while ((line = in.readLine()) != null) {
                    refEntries.add(line);
                }

                List<Path> actEntries = new ArrayList<>();
                Stream<Path> stream =
                        Files.find(pm.getAppDir().toPath(), 100,
                                (path, basicFileAttributes) -> {
                                    File file = path.toFile();
                                    return !file.isDirectory();
                                });
                actEntries = stream.collect(Collectors.toList());

                ArrayList<File> deleteCandidates = new ArrayList<>();
                ArrayList<TrackLog> deleteTLCandidates = new ArrayList<>();
                for (Path path : actEntries) {
                    String absolute = path.toString().replaceFirst(".*files/MGMapViewer/", "");
                    boolean found = refEntries.remove(absolute);
                    if (!found) {
                        deleteCandidates.add(new File(pm.getAppDir(), absolute));
                        if (absolute.endsWith(".gpx")) {
                            for (String nameKey : application.metaTrackLogs.keySet()) {
                                TrackLog trackLog = application.metaTrackLogs.get(nameKey);
                                String name1 = trackLog.getName();
                                String name2 = absolute.replaceFirst("\\.gpx", "").replaceFirst(".*\\/", "");
//                                Log.i(MGMapApplication.LABEL, NameUtil.context()+"AAAAA1 name: "+name1);
//                                Log.i(MGMapApplication.LABEL, NameUtil.context()+"AAAAA2 name: "+absolute.replaceFirst("\\.gpx","").replaceFirst(".*\\/",""));
                                if (name1.equals(name2)) {
                                    deleteTLCandidates.add(trackLog);
                                }
                            }
                        }
                    }
                }
                Log.w(MGMapApplication.LABEL, NameUtil.context() + " Create defined starting position for tests: missing files: " + refEntries.size() + " unexpected files " + deleteCandidates.size());
                if (refEntries.size() > 0) {
                    for (String s : refEntries) {
                        Log.i(MGMapApplication.LABEL, NameUtil.context() + " missing file: " + s);
                    }
                }
                if (deleteCandidates.size() > 0) {
                    for (File file : deleteCandidates) {
                        Log.i(MGMapApplication.LABEL, NameUtil.context() + " Unexpected file " + file.getAbsolutePath() + " (" + file.exists() + ")");
                        pm.deleteFile(file);
                    }
                }
                if (deleteTLCandidates.size() > 0) {
                    for (TrackLog trackLog : deleteTLCandidates) {
                        Log.i(MGMapApplication.LABEL, NameUtil.context() + " Unexpected trackLog: nameKey=" + trackLog.getNameKey());
                        application.metaTrackLogs.remove(trackLog.getNameKey());
                        application.availableTrackLogsObservable.availableTrackLogs.remove(trackLog);
                    }
                }
            }
            prefCache.get(R.string.FSATL_pref_hideAll, false).toggle();
            // (eventuell auch andere Properties auf sinnvollen Stand setzen)
            prefCache.get(R.string.Layers_pref_chooseMap1_key, "").setValue("none");
            prefCache.get(R.string.Layers_pref_chooseMap2_key, "").setValue("MAPSFORGE: baden-wuerttemberg_oam.osm.map");
            prefCache.get(R.string.Layers_pref_chooseMap3_key, "").setValue("none");
            prefCache.get(R.string.Layers_pref_chooseMap4_key, "").setValue("none");
            prefCache.get(R.string.Layers_pref_chooseMap5_key, "").setValue("none");
            prefCache.get(R.string.preference_choose_theme_key, "").setValue("Elevate.xml");
            prefCache.get(R.string.preference_choose_search_key, "").setValue("Graphhopper");
            prefCache.get(R.string.FSATL_pref_stlGl, false).setValue(false);
            prefCache.get(R.string.FSRouting_pref_RouteGL, false).setValue(false);

        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
    }

    private void executeTestCases(ArrayList<Testcase> testcases){
        boolean finished = false;
        StringBuilder sb = new StringBuilder();
        Runnable rStop = null;
        while (!finished){
            try {
                Thread.sleep(500);
                if (currentTestcase == null){                                      // currently no testcase running
                    if (testcases.size() > 0){                                     // is there any more testcase -> yes
                        Log.i(MGMapApplication.LABEL, NameUtil.context());
                        currentTestcase = testcases.remove(0);               // take testcase
                        currentTestcase.start();                                   // ... and start it
                        final Testcase theTestcase = currentTestcase;
                        rStop = () -> {
                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+ theTestcase.name);
                            if (theTestcase.isRunning()){
                                theTestcase.stop();
                                Log.i(MGMapApplication.LABEL, NameUtil.context()+" timeout - testcase stopped "+ theTestcase.name);
                            }
                        };
                        suTimer.postDelayed(rStop,  currentTestcase.getTimeout()); // setup testcase time supervision
                    } else {                                                // is there any more testcase -> no
                        finished = true;                                    // ==> finished work
                    }
                } else {                                                    // currently testcase is running
//                    Log.v(MGMapApplication.LABEL, NameUtil.context()+" "+currentTestcase.name+" "+currentTestcase.isRunning()+" ");
                    if (!currentTestcase.isRunning()){                             // but yet is finished
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+ currentTestcase.name+" finished ");
                        currentTestcase.logResult(sb);                             // log the result immediately, add summary info to given StringBuffer sb
                        currentTestcase = null;
                        assert rStop != null;
                        suTimer.removeCallbacks(rStop);                     // remove supervision timer
                    }
                }
            } catch (Exception e) {
                Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
            }
        }
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" ********************* Test Result ********************"+sb.toString());
    }

    public void setCursorVisibility(boolean visibility) {
        cursorVisibility = visibility;
        TestView tv = testView;
        if (tv != null){
            tv.setVisibility(visibility?View.VISIBLE:View.INVISIBLE, tv.cursor);
        }
    }
    public void setClickVisibility(boolean visibility) {
        TestView tv = testView;
        if (tv != null){
            tv.setVisibility(visibility?View.VISIBLE:View.INVISIBLE, tv.click);
        }
    }

    public void onResume(TestView testView) {
        if (prefTestMode.getValue()){
            if (this.testView == null){
                this.testView = testView;
                setPos(pPos);                                               // there is a new TestView registered, bring cursor and click  ImageView to correct position
                setCursorVisibility(cursorVisibility);                      // and show it (if visibility is given)
                Log.i(MGMapApplication.LABEL, NameUtil.context() +"set TestView "+testView.getContext().getClass().getSimpleName());
            }
        }
    }
    public void onPause(TestView testView){
        if (this.testView == testView){
            this.testView = null;
            Log.i(MGMapApplication.LABEL, NameUtil.context() +"set TestView null");
        }
    }

    public void onTestViewLayout(TestView tv){
        if (tv == testView){
            setPos(pPos);
        }
    }

    public void setPos(PointF pPos){
        this.pPos.set(pPos);
        cursor2Pos();
        click2Pos();
    }

    public void cursor2Pos(){
        TestView tv = testView;
        if (tv != null){
            Point to = new Point();
            tv.percent2pos(pPos, to);
            tv.cursor.setX(to.x);
            tv.cursor.setY(to.y);
        }
    }
    public void click2Pos(){
        TestView tv = testView;
        if (tv != null){
            Point to = new Point();
            tv.percent2pos(pPos, to);
            tv.click.setX(to.x- tv.click.getWidth()/2.0f);
            tv.click.setY(to.y- tv.click.getHeight()/2.0f);
        }
    }


    public void animateTo(Testcase testcase, PointF pPosNew, int duration){
        TestView tv = testView;
        if ((tv != null) && (currentTestcase == testcase)){
            Point from = new Point();
            Point toCursor = new Point();
            Point toClick = new Point();
            limitPPos(pPosNew);
            tv.percent2pos(pPos, from);  // convert old position to pixel
            tv.percent2pos(pPosNew, toCursor); // convert new position to pixel
            tv.clickOffset(toCursor, toClick); // convert new position to pixel

            Log.i(MGMapApplication.LABEL, NameUtil.context()+" form:"+pPos+" to:"+pPosNew
                    +" pxFrom:"+from+" pxTo:"+toCursor);
            {
                ObjectAnimator animation = ObjectAnimator.ofFloat(tv.cursor, "translationX", toCursor.x);
                animation.setDuration(duration);
                animation.start();
                ObjectAnimator animation2 = ObjectAnimator.ofFloat(tv.cursor, "translationY", toCursor.y);
                animation2.setDuration(duration);
                animation2.start();
            }
            {
                ObjectAnimator animation = ObjectAnimator.ofFloat(tv.click, "translationX", toClick.x);
                animation.setDuration(duration);
                animation.start();
                ObjectAnimator animation2 = ObjectAnimator.ofFloat(tv.click, "translationY", toClick.y);
                animation2.setDuration(duration);
                animation2.start();
            }

            pPos.set(pPosNew);  // take the new position
//            click2Pos();        // ... and bring click ImageView already to target position
        }
    }

    // execute a test click
    public void doClick(Testcase testcase){
        final TestView tv = testView;
        if ((tv != null) && (currentTestcase == testcase)){
            Point to = new Point();
            Point toRaw = new Point();

            setClickVisibility(true);
            tv.percent2pos(pPos, to);
            tv.percent2rawPos(pPos, toRaw);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" to="+to+" toRaw="+toRaw);
            timer.postDelayed(new ScreenClicker(toRaw), 200);   // use the "raw" position, since this is relative to the screen, not to the TestView
                                                                    // do the click after 200ms + some time to execute this command
            ObjectAnimator animation = ObjectAnimator.ofFloat(tv.click, "scaleX", 2);
            animation.setDuration(500);                             // ... run the click animation
            animation.start();                                      // but start this immediately
            ObjectAnimator animation2 = ObjectAnimator.ofFloat(tv.click, "scaleY", 2);
            animation2.setDuration(500);                            // same for Y direction
            animation2.start();
            timer.postDelayed(() -> {                               // finally after 600ms the click animation steps
                setClickVisibility(false);                          // ImageIcon will disappear
                tv.click.setScaleX(1);                              // and the scale will be reset to normal size (1)
                tv.click.setScaleY(1);
            },600);
        }
    }

    public void swipeTo(Testcase testcase, PointF pPosNew, int duration) {
        TestView tv = testView;
        if ((tv != null) && (currentTestcase == testcase)){
            Point fromRaw = new Point();
            Point toRaw = new Point();
            limitPPos(pPosNew);
            tv.percent2rawPos(pPos, fromRaw);
            tv.percent2rawPos(pPosNew, toRaw);

            Log.i(MGMapApplication.LABEL, NameUtil.context()+" fromRaw="+fromRaw+" toRaw="+toRaw);
            tv.click.setScaleX(1.5f);                              // and the scale will be reset to normal size (1)
            tv.click.setScaleY(1.5f);
            setClickVisibility(true);
            timer.postDelayed(new ScreenSwiper(fromRaw, toRaw, duration), 1);
            timer.postDelayed(() -> animateTo(testcase, pPosNew, duration), 100);
            timer.postDelayed(() -> {
                setClickVisibility(false);                          // ImageIcon will disappear
                tv.click.setScaleX(1);                              // and the scale will be reset to normal size (1)
                tv.click.setScaleY(1);
            },duration + 200);

        }
    }

    private void limitPPos(PointF pPosNew){
        pPosNew.x = Math.min(Math.max(MIN_X_PERCENT, pPosNew.x), MAX_X_PERCENT);
        pPosNew.y = Math.min(Math.max(MIN_Y_PERCENT, pPosNew.y), MAX_Y_PERCENT);
    }

    public <T> Pref<T> getPref(int keyId, T defaultValue){
        return prefCache.get(keyId, defaultValue);
    }

}
