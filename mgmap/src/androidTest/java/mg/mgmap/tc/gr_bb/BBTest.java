package mg.mgmap.tc.gr_bb;

import android.content.Context;
import android.graphics.Point;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.ExtendedTextView;
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

    @Test(timeout = 60000)
    public void _01_load_from_bb() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);

        Pref<Boolean> prefBboxOn = mgMapActivity.getPrefCache().get(R.string.FSBB_qc_bboxOn, false);
        assert !prefBboxOn.getValue();

        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.422888,13.448283), (byte) 14);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        setCursorToCenterPos();
        SystemClock.sleep(1000);

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);
        assert prefBboxOn.getValue();

        addRegex(".*onClick mi_load_from_bb.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_load_from_bb);
        Assert.assertEquals(1, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size() );

        DisplayMetrics dm = mgMapApplication.getApplicationContext().getResources().getDisplayMetrics();
        double x1 = dm.widthPixels / 3.0;
        double x2 = x1 * 2;
        double y1 = (dm.heightPixels / 2.0) - (x1 / 2);
        double y2 = (dm.heightPixels / 2.0) + (x1 / 2);
        int ix1 = (int)x1;
        int ix2 = (int)x2;
        int iy1 = (int)y1;
        int iy2 = (int)y2;
        Point p1 = new Point(ix1, iy1);
        Point p2 = new Point(ix2, iy2);
        animateSwipeToPos(p1,new Point(ix1-100,iy1-100));
        animateSwipeToPos(p2,new Point(ix2,iy2+300));

        animateTo(getCenterPos());
        animateSwipeToPos(currentPos,currentPos); // animate longClick
        Assert.assertEquals(2, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size() );
        assert !prefBboxOn.getValue();

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);
        assert prefBboxOn.getValue();

        animateSwipeToPos(p1,new Point(ix1-200,iy1-100));
        animateSwipeToPos(p2,new Point(ix2,iy2+500));

        addRegex(".*onClick mi_load_from_bb.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_load_from_bb);
        Assert.assertEquals(3, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size() );

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);
        assert !prefBboxOn.getValue();

        animateTo(getCenterPos());

        SystemClock.sleep(2000);
        mgLog.i("finished");
    }

    @Test(timeout = 120000)
    public void _02_load_and_delete_with_tilestore() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);

        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.422888,13.448283), (byte) 14);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        setCursorToCenterPos();
        SystemClock.sleep(1000);

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        addRegex(".*onClick mi_load_remain.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_load_remain);

        addRegex(".*onClick bgJobGroupConfirm_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        addRegex(".*successCounter=2.  errorCounter=0  jobCounter=2..*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        // switch bbox off
        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        //scroll down
        Point pCenter = getCenterPos();
        animateSwipeToPos(pCenter, new Point(pCenter.x, pCenter.y+400));


        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        // load partly (only remaining)
        addRegex(".*onClick mi_load_remain.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_load_remain);
        addRegex(".*onClick bgJobGroupConfirm_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        addRegex(".*successCounter=1.  errorCounter=0  jobCounter=1..*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        // reload all
        addRegex(".*onClick mi_load_all.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_load_all);
        addRegex(".*onClick bgJobGroupConfirm_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        addRegex(".*successCounter=2.  errorCounter=0  jobCounter=2..*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        // switch bbox off
        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        addRegex(".*onClick mi_zoom_out.*");
        animateToViewAndClick(R.id.menu_multi);
        animateToViewAndClick(R.id.mi_zoom_out);
        animateToViewAndClick(R.id.mi_zoom_out);
        SystemClock.sleep(3500);

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        // delete all
        addRegex(".*onClick mi_delete_all.*");
        addRegex(".*Drop .. tiles in 3 jobs.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_delete_all);
        addRegex(".*onClick bgJobGroupConfirm_Drop_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        addRegex(".*successCounter=3  errorCounter=0  jobCounter=3.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Drop_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        // switch bbox off
        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);


        addRegex(".*onClick mi_zoom_in.*");
        animateToViewAndClick(R.id.menu_multi);
        animateToViewAndClick(R.id.mi_zoom_in);
        animateToViewAndClick(R.id.mi_zoom_in);
        SystemClock.sleep(3500);

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        // and now load again to verify that delete was done ...
        addRegex(".*onClick mi_load_remain.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_load_remain);
        addRegex(".*onClick bgJobGroupConfirm_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        addRegex(".*successCounter=1.  errorCounter=0  jobCounter=1..*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Load_Tiles_for_\"OpenCycleMap\"_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        SystemClock.sleep(2000);
        mgLog.i("finished");
    }

    @Test(timeout = 20000)
    public void _03_tilestore_and_hgt() {
        mgLog.i("started");
        MGMapActivity mgMapActivity1 = waitForActivity(MGMapActivity.class);
        SystemClock.sleep(1000);
        addRegex(".*recreate MGMapActivity due to key=SelectMap4 value=MAPGRID: hgt*");
        mgMapActivity1.getPrefCache().get(R.string.Layers_pref_chooseMap4_key, "").setValue("MAPGRID: hgt");
        SystemClock.sleep(1000);
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.422888, 13.448283), (byte) 7);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        setCursorToCenterPos();

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        animateToViewAndClick(R.id.menu_bb);
        Assert.assertFalse(waitForView(ExtendedTextView.class, R.id.mi_load_remain).isEnabled());
        Assert.assertFalse(waitForView(ExtendedTextView.class, R.id.mi_load_all).isEnabled());
        Assert.assertFalse(waitForView(ExtendedTextView.class, R.id.mi_delete_all).isEnabled());

        SystemClock.sleep(1000);

    }


    @Test(timeout = 120000)
    public void _04_load_and_delete_with_hgt() {
        mgLog.i("started");
        MGMapActivity mgMapActivity1 = waitForActivity(MGMapActivity.class);

        addRegex(".*recreate MGMapActivity due to key=SelectMap3 value=MAPGRID: hgt*");
        mgMapActivity1.getPrefCache().get(R.string.Layers_pref_chooseMap3_key, "").setValue("MAPGRID: hgt");
        SystemClock.sleep(1000);
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.3,13.448283), (byte) 7);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        setCursorToCenterPos();
        SystemClock.sleep(1000);

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        addRegex(".*onClick mi_load_remain.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_load_remain);

        addRegex(".*onClick bgJobGroupConfirm_Download_height_data_btPositive.*");
        addRegex(".*successCounter=2  errorCounter=0  jobCounter=2.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        // switch bbox off
        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        //scroll down
        Point pCenter = getCenterPos();
        animateSwipeToPos(pCenter, new Point(pCenter.x, pCenter.y+100));


        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        // load partly (only remaining)
        addRegex(".*onClick mi_load_remain.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_load_remain);
        addRegex(".*onClick bgJobGroupConfirm_Download_height_data_btPositive.*");
        addRegex(".*successCounter=3  errorCounter=0  jobCounter=3.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        // reload all
        addRegex(".*onClick mi_load_all.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_load_all);
        addRegex(".*onClick bgJobGroupConfirm_Download_height_data_btPositive.*");
        addRegex(".*successCounter=6  errorCounter=0  jobCounter=6.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        // switch bbox off
        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        addRegex(".*onClick mi_zoom_out.*");
        animateToViewAndClick(R.id.menu_multi);
        animateToViewAndClick(R.id.mi_zoom_out);
        animateToViewAndClick(R.id.mi_zoom_out);
        SystemClock.sleep(3500);

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        // delete all
        addRegex(".*onClick mi_delete_all.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_delete_all);
        addRegex(".*onClick bgJobGroupConfirm_Drop_hgt_files_btPositive.*");
        addRegex(".*successCounter=6  errorCounter=0  jobCounter=6.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Drop_hgt_files_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        // switch bbox off
        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);


        addRegex(".*onClick mi_zoom_in.*");
        animateToViewAndClick(R.id.menu_multi);
        animateToViewAndClick(R.id.mi_zoom_in);
        animateToViewAndClick(R.id.mi_zoom_in);
        SystemClock.sleep(3500);

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);

        // and now load again to verify that delete was done ...
        addRegex(".*onClick mi_load_remain.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_load_remain);
        addRegex(".*onClick bgJobGroupConfirm_Download_height_data_btPositive.*");
        addRegex(".*successCounter=6  errorCounter=0  jobCounter=6.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        addRegex(".*onClick bgJobGroupResult_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        SystemClock.sleep(2000);
        mgLog.i("finished");
    }





}
