package mg.mgmap.tc.gr_marker;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.lang.invoke.MethodHandles;

//import leakcanary.AppWatcher;
//import leakcanary.DetectLeaksAfterTestSuccess;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogStatistic;
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
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.422888,13.448283),(byte) 14));
        setCursorToCenterPos();
        SystemClock.sleep(2000);

        addRegex(".*onClick mi_marker_edit.*");
        animateMenu(R.id.menu_marker, R.id.mi_marker_edit);

        animateToPosAndClick(54.420327,13.437579);
        animateToPosAndClick(54.415861,13.447284);
        animateToPosAndClick(54.418657,13.456617);
        animateToPosAndClick(54.424050,13.454930);
        animateToPosAndClick(54.421417,13.444976);
        animateToPosAndClick(54.423379,13.433771);
        SystemClock.sleep(1000);


        TrackLogStatistic stat = mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic();
        mgLog.i("Test Statistic: " + stat);
        Assert.assertEquals(4316,stat.getTotalLength(), 5);
        Assert.assertEquals(67.3,stat.getGain(), 2);
        Assert.assertEquals(91.5,stat.getLoss(), 2);
        Assert.assertEquals(14.1,stat.getMinEle(), 1);
        Assert.assertEquals(81.2,stat.getMaxEle(), 1);
        Assert.assertEquals(125,stat.getNumPoints());


        SystemClock.sleep(2000);
        animateSwipeLatLong(54.4240,13.454900, 54.421981, 13.450780);
        SystemClock.sleep(500);
        stat = mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic();
        mgLog.i("Test Statistic: " + stat);
        Assert.assertEquals(3959,stat.getTotalLength(), 5);
        Assert.assertEquals(67.3,stat.getGain(), 2);
        Assert.assertEquals(91.5,stat.getLoss(), 2);
        Assert.assertEquals(14.1,stat.getMinEle(), 1);
        Assert.assertEquals(81.2,stat.getMaxEle(), 1);
        Assert.assertEquals(118,stat.getNumPoints());

        SystemClock.sleep(2000);
        animateTo(getCenterPos());
        SystemClock.sleep(2000);
        mgLog.i("finished");
    }

//    @Test()
    public void _02B_marker() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.422888, 13.448283), (byte) 14));
        setCursorToCenterPos();
        SystemClock.sleep(2000);

        addRegex(".*onClick mi_marker_edit.*");
        animateMenu(R.id.menu_marker, R.id.mi_marker_edit);

        animateToPosAndClick(54.415861,13.447284);
        SystemClock.sleep(1000);

        for (int i=0; i<100; i++){
            animateToPosAndClick(54.420327,13.437579);
            SystemClock.sleep(1000);
            mgLog.i("Test Statistic 1: ("+i+") " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic());
            Assert.assertEquals(1046.72, mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic().getTotalLength(), 0.01);
            Assert.assertEquals(2, mgMapApplication.markerTrackLogObservable.getTrackLog().getTrackStatistic().getNumPoints());


            animateSwipeLatLong(54.420327,13.437579, 54.425327,13.437579);
            SystemClock.sleep(1000);
            mgLog.i("Test Statistic 2: ("+i+") " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic());
            Assert.assertEquals(1796.13, mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic().getTotalLength(), 0.01);
            Assert.assertEquals(2, mgMapApplication.markerTrackLogObservable.getTrackLog().getTrackStatistic().getNumPoints());

            animateToPosAndClick(54.425327,13.437579);
        }
    }

    @Test(timeout = 80000)
    public void _02_marker() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
//        AppWatcher.INSTANCE.getObjectWatcher().expectWeaklyReachable(mgMapActivity, "mgMapActivityObject");
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.422888,13.448283),(byte) 14));
        setCursorToCenterPos();
        SystemClock.sleep(2000);

        addRegex(".*onClick mi_marker_edit.*");
        animateMenu(R.id.menu_marker, R.id.mi_marker_edit);

        animateToPosAndClick(54.420327,13.437579);
        animateToPosAndClick(54.415861,13.447284);
        animateToPosAndClick(54.418657,13.456617);
        SystemClock.sleep(1000);
        mgLog.i("Test Statistic 1: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic() );
        Assert.assertEquals(1920,mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic().getTotalLength(), 2);
        Assert.assertEquals(3,mgMapApplication.markerTrackLogObservable.getTrackLog().getTrackStatistic().getNumPoints());

        animateToPosAndClick(54.416,13.447);
        SystemClock.sleep(1000);
        mgLog.i("Test Statistic 2: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic() );
        Assert.assertEquals(1577,mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic().getTotalLength(), 2);
        Assert.assertEquals(2,mgMapApplication.markerTrackLogObservable.getTrackLog().getTrackStatistic().getNumPoints());

        animateSwipeLatLong(54.421981, 13.450780, 54.416,13.447);
        SystemClock.sleep(1000);
        mgLog.i("Test Statistic 3: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic() );
        Assert.assertEquals(1920,mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic().getTotalLength(), 2);
        Assert.assertEquals(3,mgMapApplication.markerTrackLogObservable.getTrackLog().getTrackStatistic().getNumPoints());

        animateSwipeLatLong(54.420327,13.437579, 54.425327,13.437579);
        SystemClock.sleep(1000);
        mgLog.i("Test Statistic 4: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic() );
        Assert.assertEquals(2669,mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic().getTotalLength(), 2);
        Assert.assertEquals(3,mgMapApplication.markerTrackLogObservable.getTrackLog().getTrackStatistic().getNumPoints());

        animateToPosAndClick( 54.425327,13.437579);
        SystemClock.sleep(1000);
        mgLog.i("Test Statistic 5: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic() );
        Assert.assertEquals(896,mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic().getTotalLength(), 2);
        Assert.assertEquals(2,mgMapApplication.markerTrackLogObservable.getTrackLog().getTrackStatistic().getNumPoints());

        animateToPosAndClick( 54.425327,13.437579);
        SystemClock.sleep(1000);
        mgLog.i("Test Statistic 6: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic() );
        Assert.assertEquals(2656,mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic().getTotalLength(), 2);
        Assert.assertEquals(3,mgMapApplication.markerTrackLogObservable.getTrackLog().getTrackStatistic().getNumPoints());

        animateSwipeLatLong(54.425327,13.437579, 54.420327,13.437579);
        SystemClock.sleep(1000);
        mgLog.i("Test Statistic 7: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic() );
        Assert.assertEquals(2473,mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic().getTotalLength(), 3);
        Assert.assertEquals(3,mgMapApplication.markerTrackLogObservable.getTrackLog().getTrackStatistic().getNumPoints());

        animateToPosAndClick( 54.420327,13.437579);
        SystemClock.sleep(1000);
        mgLog.i("Test Statistic 8: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic() );
        Assert.assertEquals(896,mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic().getTotalLength(), 2);
        Assert.assertEquals(2,mgMapApplication.markerTrackLogObservable.getTrackLog().getTrackStatistic().getNumPoints());

        animateToPosAndClick( 54.416044,13.453317);
        SystemClock.sleep(1000);
        mgLog.i("Test Statistic 9: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic() );
        Assert.assertEquals(896,mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic().getTotalLength(), 2);
        Assert.assertEquals(3,mgMapApplication.markerTrackLogObservable.getTrackLog().getTrackStatistic().getNumPoints());

        SystemClock.sleep(2000);
        animateTo(getCenterPos());
        SystemClock.sleep(2000);
        mgLog.i("finished");
    }


    @Test(timeout = 50000)
    public void _03_snap_14() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
//        AppWatcher.INSTANCE.getObjectWatcher().expectWeaklyReachable(mgMapActivity, "mgMapActivityObject");
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.354733,13.274653),(byte) 14));
        setCursorToCenterPos();
        SystemClock.sleep(2000);

        addRegex(".*onClick mi_marker_edit.*");
        animateMenu(R.id.menu_marker, R.id.mi_marker_edit);

        addRegex(".*optimize Pos Lat=54.3551.., Lon=13.2729.. to Lat=54.3547.., Lon=13.2729... .*");
        addRegex(".*optimize Pos Lat=54.3556.., Lon=13.2759.. to Lat=54.3547.., Lon=13.2759.., .*");
        addRegex(".*optimize Pos Lat=54.3560.., Lon=13.2789.. to Lat=54.3547.., Lon=13.2789.., .*");
        addRegex(".*optimize Pos Lat=54.3565.., Lon=13.2819.. no approach.*");

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
    public void _04_snap_15() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
//        AppWatcher.INSTANCE.getObjectWatcher().expectWeaklyReachable(mgMapActivity, "mgMapActivityObject");
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.354733,13.274653),(byte) 15));
        setCursorToCenterPos();
        SystemClock.sleep(2000);

        addRegex(".*onClick mi_marker_edit.*");
        animateMenu(R.id.menu_marker, R.id.mi_marker_edit);

        addRegex(".*optimize Pos Lat=54.3550.., Lon=13.2729.. to Lat=54.3547.., Lon=13.2729.., .*");
        addRegex(".*optimize Pos Lat=54.3552.., Lon=13.2759.. to Lat=54.3547.., Lon=13.2759.., .*");
        addRegex(".*optimize Pos Lat=54.3555.., Lon=13.2789.. to Lat=54.3547.., Lon=13.2789.., .*");
        addRegex(".*optimize Pos Lat=54.3558.., Lon=13.2819.. no approach.*");

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
    public void _05_snap_16() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
//        AppWatcher.INSTANCE.getObjectWatcher().expectWeaklyReachable(mgMapActivity, "mgMapActivityObject");
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.354733,13.274653),(byte) 16));
        setCursorToCenterPos();
        SystemClock.sleep(2000);

        addRegex(".*onClick mi_marker_edit.*");
        animateMenu(R.id.menu_marker, R.id.mi_marker_edit);

        addRegex(".*optimize Pos Lat=54.3548.., Lon=13.2729.. to Lat=54.3547.., Lon=13.2729.., .*");
        addRegex(".*optimize Pos Lat=54.3550.., Lon=13.2744.. to Lat=54.3547.., Lon=13.2744.., .*");
        addRegex(".*optimize Pos Lat=54.3551.., Lon=13.2759.. to Lat=54.3547.., Lon=13.2759.., .*");
        addRegex(".*optimize Pos Lat=54.3552.., Lon=13.2774.. no approach.*");

        double disty  = PointModelUtil.latitudeDistance(15.0);
        double distx = 0.0015;
        for (int i=0; i<4; i++){
            animateToPosAndClick(54.354742+(i+1)*disty, 13.272980+(i)*distx);
        }

        SystemClock.sleep(1000);
        animateTo(getCenterPos());
        SystemClock.sleep(2000);
        mgLog.i("finished");
    }



}
