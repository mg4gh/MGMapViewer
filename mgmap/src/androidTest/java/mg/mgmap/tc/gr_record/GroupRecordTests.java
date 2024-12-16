package mg.mgmap.tc.gr_record;

import android.Manifest;
import android.graphics.Point;
import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.TestView;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class GroupRecordTests extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    @Rule
    public GrantPermissionRule permissionLocation = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);

    public GroupRecordTests(){
        mgLog.i("create");
        mgMapApplication.getSetup().wantSetup("SETUP_002", androidTestAssets);
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);

    @Before
    public void initMapPosition(){
        mgLog.i("init");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        initPos(mgMapActivity, new PointModelImpl(54.315814,13.351981),(byte) 16);
    }

    @Test(timeout = 120000)
    public void _01_center() {
        mgLog.i("started");
        SystemClock.sleep(5000);

        setCursorToCenterPos();

        addRegex(".*onClick mi_gps_toggle.*");
        animateMenu(R.id.menu_gps, R.id.mi_gps_toggle);

        addRegex(".*onClick mi_gps_center.*");
        animateMenu(R.id.menu_gps, R.id.mi_gps_center);

        SystemClock.sleep(1000);
        animateAndClick(54.3160, 13.3520, 11.1f, 60.1f, 48.6f, 55.5f);
        animateAndClick(54.3166, 13.3511, 11.1f, 66.1f, 48.6f, 55.5f);
        animateAndClick(54.3171, 13.3521, 11.1f, 72.1f, 48.6f, 55.5f);
        animateAndClick(54.3180, 13.3520, 11.1f, 73.1f, 48.6f, 55.5f);
        Assert.assertNull(mgMapApplication.recordingTrackLogObservable.getTrackLog());

        addRegex(".*onClick mi_gps_center.*");
        animateMenu(R.id.menu_gps, R.id.mi_gps_center);

        animateAndClick(54.3200, 13.3517, 11.1f, 78.1f, 48.6f, 55.5f);
        animateAndClick(54.3220, 13.3515, 11.1f, 83.1f, 48.6f, 55.5f);

        animateSwipeLatLong(54.3220, 13.3515, 54.3220, 13.3545);
        Point p = getPoint4PointModel(new PointModelImpl(54.3220, 13.3515));
        Assert.assertNotEquals(getCenterPos().point(), p);
        SystemClock.sleep(5000);
        p = getPoint4PointModel(new PointModelImpl(54.3220, 13.3515));
        Assert.assertNotEquals(getCenterPos().point(), p);
        SystemClock.sleep(3000);
        p = getPoint4PointModel(new PointModelImpl(54.3220, 13.3515));
        Assert.assertEquals(getCenterPos().point(), p);

        SystemClock.sleep(1000);

        animateSwipeLatLong(54.3220, 13.3515, 54.3220, 13.3545);
        p = getPoint4PointModel(new PointModelImpl(54.3220, 13.3515));
        Assert.assertNotEquals(getCenterPos().point(), p);
        SystemClock.sleep(4000);
        animateSwipeLatLong(54.3220, 13.3485, 54.3220, 13.3515);
        p = getPoint4PointModel(new PointModelImpl(54.3220, 13.3515));
        Assert.assertNotEquals(getCenterPos().point(), p);
        SystemClock.sleep(5000);
        p = getPoint4PointModel(new PointModelImpl(54.3220, 13.3515));
        Assert.assertNotEquals(getCenterPos().point(), p);
        SystemClock.sleep(3000);
        p = getPoint4PointModel(new PointModelImpl(54.3220, 13.3515));
        Assert.assertEquals(getCenterPos().point(), p);


        addRegex(".*onClick mi_gps_toggle.*");
        animateMenu(R.id.menu_gps, R.id.mi_gps_toggle);

        SystemClock.sleep(2000);
        mgLog.i("finished");
    }

    @Test(timeout = 60000)
    public void _02_gps() {
        mgLog.i("started");
        SystemClock.sleep(5000);

        setCursorToCenterPos();
        addRegex(".*onClick mi_gps_toggle.*");
        animateMenu(R.id.menu_gps, R.id.mi_gps_toggle);
        SystemClock.sleep(1000);
        animateAndClick(54.3160, 13.3520, 11.1f, 60.1f, 48.6f, 55.5f);
        animateAndClick(54.3166, 13.3511, 11.1f, 66.1f, 48.6f, 55.5f);
        animateAndClick(54.3171, 13.3521, 11.1f, 72.1f, 48.6f, 55.5f);
        animateAndClick(54.3180, 13.3520, 11.1f, 73.1f, 48.6f, 55.5f);
        Assert.assertNull(mgMapApplication.recordingTrackLogObservable.getTrackLog());
        addRegex(".*onClick mi_gps_center.*");
        animateToViewAndClick(R.id.menu_gps);
        animateToViewAndClick(R.id.mi_gps_center);
        addRegex(".*center:0.22 km");
        animateAndClick(54.3200, 13.3517, 11.1f, 78.1f, 48.6f, 55.5f);
        addRegex(".*center:0.45 km");
        animateAndClick(54.3220, 13.3515, 11.1f, 83.1f, 48.6f, 55.5f);

        SystemClock.sleep(1000);

        addRegex(".*onClick mi_gps_toggle.*");
        animateMenu(R.id.menu_gps, R.id.mi_gps_toggle);

        SystemClock.sleep(2000);
        mgLog.i("finished");
    }

    @Test(timeout = 60000)
    public void _03_trackRecord() {
        mgLog.i("started");
        SystemClock.sleep(5000);

        setCursorToCenterPos();
        addRegex(".*onClick mi_record_track.*");
        animateMenu(R.id.menu_gps, R.id.mi_record_track);
        SystemClock.sleep(1000);
        animateAndClick(54.3160, 13.3520, 11.1f, 60.1f, 48.6f, 55.5f);
        animateAndClick(54.3166, 13.3511, 11.1f, 66.1f, 48.6f, 55.5f);
        animateAndClick(54.3171, 13.3521, 11.1f, 72.1f, 48.6f, 55.5f);
        animateAndClick(54.3180, 13.3520, 11.1f, 73.1f, 48.6f, 55.5f);
        addRegex(".*onClick mi_gps_center.*");
        animateToViewAndClick(R.id.menu_gps);
        animateToViewAndClick(R.id.mi_gps_center);
        animateAndClick(54.3200, 13.3517, 11.1f, 78.1f, 48.6f, 55.5f);
        animateAndClick(54.3220, 13.3515, 11.1f, 83.1f, 48.6f, 55.5f);

        SystemClock.sleep(1000);
        addRegex(".*Test Statistic: .* duration=0:00 totalLength=721.18 gain=20.5 loss=0.0 minEle=11.5 maxEle=34.5 numPoints=6");
        mgLog.i("Test Statistic: " + mgMapApplication.recordingTrackLogObservable.getTrackLog().getTrackStatistic());

        animateMenu(R.id.menu_gps, R.id.mi_record_track);

        SystemClock.sleep(2000);
        mgLog.i("finished");
    }


    @Test(timeout = 90000)
    public void _04_segmentRecord() {
        mgLog.i("started");
        SystemClock.sleep(5000);

        setCursorToCenterPos();
        animateMenu(R.id.menu_gps, R.id.mi_record_segment);
        Assert.assertNull(mgMapApplication.recordingTrackLogObservable.getTrackLog());
        SystemClock.sleep(2000);
        addRegex(".*onClick mi_record_track.*");
        animateMenu(R.id.menu_gps, R.id.mi_record_track);

        Assert.assertNotNull(mgMapApplication.recordingTrackLogObservable.getTrackLog());
        SystemClock.sleep(1000);
        animateAndClick(54.3160, 13.3520, 11.1f, 60.1f, 48.6f, 55.5f);
        animateAndClick(54.3166, 13.3511, 11.1f, 66.1f, 48.6f, 55.5f);
        animateAndClick(54.3171, 13.3521, 11.1f, 72.1f, 48.6f, 55.5f);
        animateAndClick(54.3180, 13.3520, 11.2f, 73.1f, 48.7f, 55.6f);
        addRegex(".*onClick mi_record_segment.*");
        animateMenu(R.id.menu_gps, R.id.mi_record_segment);
        Assert.assertNotNull(mgMapApplication.recordingTrackLogObservable.getTrackLog());
        SystemClock.sleep(2000);
        addRegex(".*onClick mi_record_segment.*");
        animateMenu(R.id.menu_gps, R.id.mi_record_segment);
        animateAndClick(54.3200, 13.3517, 11.1f, 78.1f, 48.6f, 55.5f);
        animateAndClick(54.3220, 13.3515, 11.1f, 83.1f, 48.6f, 55.5f);

        SystemClock.sleep(1000);
        addRegex(".*Test Statistic: .* duration=0:00 totalLength=497.69 gain=12.9 loss=0.0 minEle=11.5 maxEle=34.5 numPoints=6");
        mgLog.i("Test Statistic: " + mgMapApplication.recordingTrackLogObservable.getTrackLog().getTrackStatistic());


        addRegex(".*onClick mi_record_track.*");
        animateMenu(R.id.menu_gps, R.id.mi_record_track);

        SystemClock.sleep(2000);
        mgLog.i("finished");
    }





    protected void animateAndClick(double lat, double lon, float acc, float wgs84ele, float geoidOffset, float wgs84eleAcc){
        TestView testView = waitForView(TestView.class, mg.mgmap.R.id.testview);
        PointModel pm = new PointModelImpl(lat, lon);
        Point p = getPoint4PointModel(pm);
        PointOfView pov = new PointOfView(p, testView);
        animateTo(pov);
        TrackLogPoint tlp = TrackLogPoint.createGpsLogPoint(System.currentTimeMillis(), lat, lon, acc, wgs84ele, geoidOffset, wgs84eleAcc);
        mgLog.i("tlp="+tlp);
        animateClick(pov, ()->{setCursorVisibility(false); mgMapApplication.logPoints2process.add(tlp);});
        p = getPoint4PointModel(pm); // due to center on new location the "pos" might be changed
        Assert.assertEquals(mgMapApplication.getPrefCache().get(R.string.FSPosition_pref_Center, true).getValue(), p.equals(getCenterPos().point()));
        setCursorPos(new PointOfView(p, testView));
        setCursorVisibility(true);
    }
}
