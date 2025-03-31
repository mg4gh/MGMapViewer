package mg.mgmap.tc.gr_bb;

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

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.features.bb.FSBB;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class BBTest extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public BBTest(){
        mgLog.i("create");
        MGMapApplication mgMapApplication = (MGMapApplication)InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        Context ctx = InstrumentationRegistry.getInstrumentation().getContext();
        mgMapApplication.getSetup().wantSetup("SETUP_003", ctx.getAssets());
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);

    @Test(timeout = 120000)
    public void _01_load_from_bb() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);

        Pref<Boolean> prefBboxOn = mgMapActivity.getPrefCache().get(R.string.FSBB_qc_bboxOn, false);
        assert !prefBboxOn.getValue();

        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.422888,13.448283),(byte) 14));
        setCursorToCenterPos();
        SystemClock.sleep(1000);

        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);
        assert prefBboxOn.getValue();
        PointModelImpl p1_1 = new PointModelImpl(54.427888,13.44128);
        PointModelImpl p2_1 = new PointModelImpl(54.421888,13.45528);
        FSBB fsbb = mgMapActivity.getFS(FSBB.class);
        while ((fsbb.getP1() == null) || (fsbb.getP2() == null)) SystemClock.sleep(50);
        assert( fsbb.getP1() != null);
        assert( fsbb.getP2() != null);
        animateSwipeLatLong(fsbb.getP1(), p1_1);
        animateSwipeLatLong(fsbb.getP2(), p2_1);


        addRegex(".*onClick mi_load_from_bb.*");
        animateMenu(R.id.menu_bb, R.id.mi_load_from_bb);
        Assert.assertEquals(1, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size() );

        PointModelImpl p1_2 = new PointModelImpl(54.427888,13.43528);
        PointModelImpl p2_2 = new PointModelImpl(54.418888,13.45528);
        animateSwipeLatLong(p1_1, p1_2);
        animateSwipeLatLong(p2_1, p2_2);

        animateTo(getCenterPos());
        animateSwipeToPos(currentPos,currentPos); // animate longClick
        Assert.assertEquals(2, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size() );
        assert !prefBboxOn.getValue();

        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);
        assert prefBboxOn.getValue();
        while ((fsbb.getP1() == null) || (fsbb.getP2() == null)) SystemClock.sleep(50);

        PointModelImpl p1_3 = new PointModelImpl(54.427888,13.43528);
        PointModelImpl p2_3 = new PointModelImpl(54.41,13.45528);
        animateSwipeLatLong(fsbb.getP1(), p1_3);
        animateSwipeLatLong(fsbb.getP2(), p2_3);

        addRegex(".*onClick mi_load_from_bb.*");
        animateMenu(R.id.menu_bb, R.id.mi_load_from_bb);
        Assert.assertEquals(3, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size() );

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);
        assert !prefBboxOn.getValue();

        animateTo(getCenterPos());

        SystemClock.sleep(2000);
        mgLog.i("finished");
    }

    @Test(timeout = 180000)
    public void _02_load_and_delete_with_tilestore() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);

        PointModelImpl p1_1 = new PointModelImpl(54.427888,13.44128);
        PointModelImpl p2_1 = new PointModelImpl(54.421888,13.45528);
        BBox bBox =  new BBox().extend(p1_1).extend(p2_1);
        mgMapActivity.runOnUiThread(() -> {
            mgMapActivity.getMapViewUtility().setCenter(bBox.getCenter());
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setZoomLevel((byte) 14);
        });
        setCursorToCenterPos();
        SystemClock.sleep(3000);

        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);
        FSBB fsbb = mgMapActivity.getFS(FSBB.class);
        while ((fsbb.getP1() == null) || (fsbb.getP2() == null)) SystemClock.sleep(50);
        assert( fsbb.getP1() != null);
        assert( fsbb.getP2() != null);
        animateSwipeLatLong(fsbb.getP1(), p1_1);
        animateSwipeLatLong(fsbb.getP2(), p2_1);

        addRegex(".*onClick mi_load_remain.*");
        animateMenu(R.id.menu_bb, R.id.mi_load_remain);

        addRegex(".*onClick bgJobGroupConfirm_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        addRegex(".*successCounter=2.  errorCounter=0  jobCounter=2..*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        // switch bbox off
        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);

        //scroll down
        PointModel pc1 = new PointModelImpl(bBox.getCenter());
        PointModel pc2 = new PointModelImpl(pc1.getLat()-0.008, pc1.getLon());
        animateSwipeLatLong(pc1, pc2);


        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);

        // load partly (only remaining)
        addRegex(".*onClick mi_load_remain.*");
        animateMenu(R.id.menu_bb, R.id.mi_load_remain);
        addRegex(".*onClick bgJobGroupConfirm_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        addRegex(".*successCounter=1.  errorCounter=0  jobCounter=1..*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        // reload all
        addRegex(".*onClick mi_load_all.*");
        animateMenu(R.id.menu_bb, R.id.mi_load_all);
        addRegex(".*onClick bgJobGroupConfirm_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        addRegex(".*successCounter=2.  errorCounter=0  jobCounter=2..*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        // switch bbox off
        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);

        addRegex(".*onClick mi_zoom_out.*");
        animateMenu(R.id.menu_multi, R.id.mi_zoom_out);
        animateToViewAndClick(R.id.mi_zoom_out);
        SystemClock.sleep(3500);

        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);

        // delete all
        addRegex(".*onClick mi_delete_all.*");
        addRegex(".*Drop .. tiles in 3 jobs.*");
        animateMenu(R.id.menu_bb, R.id.mi_delete_all);
        addRegex(".*onClick bgJobGroupConfirm_Drop_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        addRegex(".*successCounter=3  errorCounter=0  jobCounter=3.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Drop_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        // switch bbox off
        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);


        addRegex(".*onClick mi_zoom_in.*");
        animateMenu(R.id.menu_multi, R.id.mi_zoom_in);
        animateToViewAndClick(R.id.mi_zoom_in);
        SystemClock.sleep(3500);

        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);

        // and now load again to verify that delete was done ...
        addRegex(".*onClick mi_load_remain.*");
        animateMenu(R.id.menu_bb, R.id.mi_load_remain);
        addRegex(".*onClick bgJobGroupConfirm_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        addRegex(".*successCounter=1.  errorCounter=0  jobCounter=1..*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        SystemClock.sleep(2000);
        mgLog.i("finished");
    }

    @Test(timeout = 60000)
    public void _03_tilestore_and_hgt() {
        mgLog.i("started");
        MGMapActivity mgMapActivity1 = waitForActivity(MGMapActivity.class);
        SystemClock.sleep(1000);
        addRegex(".*recreate.*due to key=SelectMap4 value=MAPGRID: hgt*");
        mgMapActivity1.getPrefCache().get(R.string.Layers_pref_chooseMap4_key, "").setValue("MAPGRID: hgt");
        SystemClock.sleep(5000);
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.8,13.4),(byte) 7));
        setCursorToCenterPos();

        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);

        mgMapApplication.getPrefCache().get(R.string.FSBB_pref_bb_action_layer, 0).setValue("MAPGRID: hgt".hashCode());
        animateMenu(R.id.menu_bb, R.id.mi_load_remain);
        addRegex(".*onClick btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupConfirm_Download_height_data_btPositive.*");
        addRegex(".*successCounter=5  errorCounter=0  jobCounter=5.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        SystemClock.sleep(1000);
        addRegex(".*onClick bgJobGroupResult_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        SystemClock.sleep(1000);

    }


    @Test(timeout = 180000)
    public void _04_load_and_delete_with_hgt() {
        mgLog.i("started");
        MGMapActivity mgMapActivity1 = waitForActivity(MGMapActivity.class);

        addRegex(".*recreate.*due to key=SelectMap3 value=MAPGRID: hgt*");
        mgMapActivity1.getPrefCache().get(R.string.Layers_pref_chooseMap3_key, "").setValue("MAPGRID: hgt");
        SystemClock.sleep(1000);
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.4,13.448283),(byte) 7));
        setCursorToCenterPos();
        SystemClock.sleep(1000);

        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);
        PointModelImpl p1_1 = new PointModelImpl(54.9,12.7);
        PointModelImpl p2_1 = new PointModelImpl(54.1,14.3);
        FSBB fsbb = mgMapActivity.getFS(FSBB.class);
        while ((fsbb.getP1() == null) || (fsbb.getP2() == null)) SystemClock.sleep(50);
        assert( fsbb.getP1() != null);
        assert( fsbb.getP2() != null);
        animateSwipeLatLong(fsbb.getP1(), p1_1);
        animateSwipeLatLong(fsbb.getP2(), p2_1);

        addRegex(".*onClick mi_load_remain.*");
        animateMenu(R.id.menu_bb, R.id.mi_load_remain);

        addRegex(".*onClick bgJobGroupConfirm_Download_height_data_btPositive.*");
        addRegex(".*successCounter=2  errorCounter=0  jobCounter=2.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        // extend bb to load more with load remain
        PointModelImpl p1_2 = new PointModelImpl(55.3,12.7);
        PointModelImpl p2_2 = new PointModelImpl(54.1,14.3);
        animateSwipeLatLong(p1_1, p1_2);
        animateSwipeLatLong(p2_1, p2_2);

        // load partly (only remaining)
        addRegex(".*onClick mi_load_remain.*");
        animateMenu(R.id.menu_bb, R.id.mi_load_remain);
        addRegex(".*onClick bgJobGroupConfirm_Download_height_data_btPositive.*");
        addRegex(".*successCounter=3  errorCounter=0  jobCounter=3.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        PointModelImpl p1_2a = new PointModelImpl(55.2,12.7);
        PointModelImpl p2_2a = new PointModelImpl(54.2,14.3);
        animateSwipeLatLong(p1_2, p1_2a);
        animateSwipeLatLong(p2_2, p2_2a);


        // reload all
        addRegex(".*onClick mi_load_all.*");
        animateMenu(R.id.menu_bb, R.id.mi_load_all);
        addRegex(".*onClick bgJobGroupConfirm_Download_height_data_btPositive.*");
        addRegex(".*successCounter=6  errorCounter=0  jobCounter=6.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        // extend bb for delete all
        PointModelImpl p1_3 = new PointModelImpl(56.1,11.8);
        PointModelImpl p2_3 = new PointModelImpl(53.8,15.2);
        animateSwipeLatLong(p1_2a, p1_3);
        animateSwipeLatLong(p2_2a, p2_3);

        // delete all
        addRegex(".*onClick mi_delete_all.*");
        animateMenu(R.id.menu_bb, R.id.mi_delete_all);
        addRegex(".*onClick bgJobGroupConfirm_Drop_hgt_files_btPositive.*");
        addRegex(".*successCounter=6  errorCounter=0  jobCounter=6.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Drop_hgt_files_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        // reduce bb for load remain
        animateSwipeLatLong(p1_3, p1_2);
        animateSwipeLatLong(p2_3, p2_2);

        // and now load again to verify that delete was done ...
        addRegex(".*onClick mi_load_remain.*");
        animateMenu(R.id.menu_bb, R.id.mi_load_remain);
        addRegex(".*onClick bgJobGroupConfirm_Download_height_data_btPositive.*");
        addRegex(".*successCounter=6  errorCounter=0  jobCounter=6.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        SystemClock.sleep(2000);
        mgLog.i("finished");
    }





}
