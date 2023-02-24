package mg.mgmap.tc.gr_marker;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;

import java.lang.invoke.MethodHandles;

//import leakcanary.AppWatcher;
//import leakcanary.DetectLeaksAfterTestSuccess;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class MarkerTest extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public MarkerTest(){
        mgLog.i("create");
        MGMapApplication mgMapApplication = (MGMapApplication)InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        Context ctx = InstrumentationRegistry.getInstrumentation().getContext();
        mgMapApplication.getSetup().wantSetup("SETUP_002", ctx.getAssets());
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);
//    @Rule
//    public DetectLeaksAfterTestSuccess l = new DetectLeaksAfterTestSuccess();

    @Test(timeout = 50000)
    public void _01_marker() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
//        AppWatcher.INSTANCE.getObjectWatcher().expectWeaklyReachable(mgMapActivity, "mgMapActivityObject");
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.422888,13.448283), (byte) 14);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        setCursorToCenterPos();
        SystemClock.sleep(2000);

        addRegex(".*onClick mi_marker_edit.*");
        animateToViewAndClick(R.id.menu_marker);
        animateToViewAndClick(R.id.mi_marker_edit);

        animateToPosAndClick(54.420327,13.437579);
        animateToPosAndClick(54.415861,13.447284);
        animateToPosAndClick(54.418657,13.456617);
        animateToPosAndClick(54.424050,13.454930);
        animateToPosAndClick(54.421417,13.444976);
        animateToPosAndClick(54.423379,13.433771);

        addRegex(".*Test Statistic: .* totalLength=4315.88 gain=70.9 loss=79.8 minEle=11.1 maxEle=92.1 numPoints=129");
        mgLog.i("Test Statistic: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic());

        SystemClock.sleep(5000);
        animateSwipeLatLong(54.4240,13.454900, 54.421981, 13.450780);
        SystemClock.sleep(500);
        addRegex(".*Test Statistic: .* totalLength=3959.18 gain=70.9 loss=79.8 minEle=11.1 maxEle=92.1 numPoints=122");
        mgLog.i("Test Statistic: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic());

        SystemClock.sleep(5000);
        animateTo(getCenterPos());
        SystemClock.sleep(5000);
        mgLog.i("finished");
    }

    @Test(timeout = 50000)
    public void _02_snap_14() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
//        AppWatcher.INSTANCE.getObjectWatcher().expectWeaklyReachable(mgMapActivity, "mgMapActivityObject");
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.354733,13.274653), (byte) 14);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        setCursorToCenterPos();
        SystemClock.sleep(2000);

        addRegex(".*onClick mi_marker_edit.*");
        animateToViewAndClick(R.id.menu_marker);
        animateToViewAndClick(R.id.mi_marker_edit);


        addRegex(".*optimize Pos Lat=54.355188, Lon=13.272999 to Lat=54.354733, Lon=13.272998, Ele=6.0m dist=50.7m.*");
        addRegex(".*optimize Pos Lat=54.355642, Lon=13.275995 to Lat=54.354730, Lon=13.275992, Ele=3.4m dist=101.5m.*");
        addRegex(".*optimize Pos Lat=54.356097, Lon=13.278991 to Lat=54.354727, Lon=13.278987, Ele=3.0m dist=152.5m.*");
        addRegex(".*optimize Pos Lat=54.356534, Lon=13.281988 no approach.*");

        double disty  = PointModelUtil.latitudeDistance(50.0);
        double distx = 0.003;
        for (int i=0; i<4; i++){
            animateToPosAndClick(54.354732+(i+1)*disty, 13.273+(i)*distx);
        }

        SystemClock.sleep(1000);
        animateTo(getCenterPos());
        SystemClock.sleep(2000);
        mgLog.i("finished");
    }

    @Test(timeout = 50000)
    public void _03_snap_15() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
//        AppWatcher.INSTANCE.getObjectWatcher().expectWeaklyReachable(mgMapActivity, "mgMapActivityObject");
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.354733,13.274653), (byte) 15);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        setCursorToCenterPos();
        SystemClock.sleep(2000);

        addRegex(".*onClick mi_marker_edit.*");
        animateToViewAndClick(R.id.menu_marker);
        animateToViewAndClick(R.id.mi_marker_edit);


        addRegex(".*optimize Pos Lat=54.355006, Lon=13.272999 to Lat=54.354733, Lon=13.272998, Ele=6.0m dist=30.4m.*");
        addRegex(".*optimize Pos Lat=54.355279, Lon=13.275995 to Lat=54.354730, Lon=13.275993, Ele=3.4m dist=61.1m.*");
        addRegex(".*optimize Pos Lat=54.355542, Lon=13.278991 to Lat=54.354727, Lon=13.278989, Ele=3.0m dist=90.7m.*");
        addRegex(".*optimize Pos Lat=54.355815, Lon=13.281988 no approach.*");

        double disty  = PointModelUtil.latitudeDistance(30.0);
        double distx = 0.003;
        for (int i=0; i<4; i++){
            animateToPosAndClick(54.354732+(i+1)*disty, 13.273+(i)*distx);
        }

        SystemClock.sleep(1000);
        animateTo(getCenterPos());
        SystemClock.sleep(2000);
        mgLog.i("finished");
    }

    @Test(timeout = 50000)
    public void _04_snap_16() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
//        AppWatcher.INSTANCE.getObjectWatcher().expectWeaklyReachable(mgMapActivity, "mgMapActivityObject");
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.354733,13.274653), (byte) 16);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        setCursorToCenterPos();
        SystemClock.sleep(2000);

        addRegex(".*onClick mi_marker_edit.*");
        animateToViewAndClick(R.id.menu_marker);
        animateToViewAndClick(R.id.mi_marker_edit);


        addRegex(".*optimize Pos Lat=54.354869, Lon=13.272999 to Lat=54.354733, Lon=13.272999, Ele=6.0m dist=15.1m.*");
        addRegex(".*optimize Pos Lat=54.355001, Lon=13.274497 to Lat=54.354732, Lon=13.274496, Ele=5.0m dist=29.9m.*");
        addRegex(".*optimize Pos Lat=54.355138, Lon=13.275995 to Lat=54.354730, Lon=13.275994, Ele=3.4m dist=45.4m.*");
        addRegex(".*optimize Pos Lat=54.355274, Lon=13.277493 no approach.*");

        double disty  = PointModelUtil.latitudeDistance(15.0);
        double distx = 0.0015;
        for (int i=0; i<4; i++){
            animateToPosAndClick(54.354732+(i+1)*disty, 13.273+(i)*distx);
        }

        SystemClock.sleep(1000);
        animateTo(getCenterPos());
        SystemClock.sleep(2000);
        mgLog.i("finished");
    }



}
