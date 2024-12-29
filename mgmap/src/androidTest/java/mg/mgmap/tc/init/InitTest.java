package mg.mgmap.tc.init;

import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.SystemClock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.settings.SettingsActivity;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.WaitUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.TestView;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class InitTest extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public InitTest(){
        mgLog.i("create");
        mgMapApplication.getSetup().wantSetup("SETUP_001", androidTestAssets);
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);

    @Test(timeout = 300000)
    public void _01_initFromScratch() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);

        mgMapActivity.getMapViewUtility().setCenter(new PointModelImpl(49.4057, 8.6789));
        mgMapActivity.getMapViewUtility().setZoomLevel((byte)5);

        setCursorToCenterPos();
        addRegex(".*onClick HintInitialMapDownload_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        addRegex(".*onClick menu_task.*");
        addRegex(".*onClick mi_download.*");
        animateMenu(R.id.menu_task, R.id.mi_download);

        AppCompatActivity settingsActivity = waitForActivity(SettingsActivity.class);
        addRegex(".*onPreferenceClick key=DldMapsDEKey.*");
        animateToPrefAndClick(R.string.preferences_dl_maps_de_key);

        TestView testView = waitForView(TestView.class, mg.mgmap.R.id.testview);
        SystemClock.sleep(10000); // now you see the browser
        PointOfView povBerlin = new PointOfView(new Point(750,980), testView);
        animateTo(povBerlin);
        SystemClock.sleep(2000); // now you see the browser
        animateClick(povBerlin, false); // just do like click
        Intent intent = new Intent(settingsActivity, MGMapActivity.class);
        intent.setData(Uri.parse("mf-v4-map://ftp.gwdg.de/pub/misc/openstreetmap/openandromaps/mapsV5/germany/Berlin.zip"));
        settingsActivity.startActivity(intent);

        waitForActivity(MGMapActivity.class);
        setCursorVisibility(true);
        SystemClock.sleep(2000);
        addRegex(".*onClick bgJobGroupConfirm_Download_map_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        SystemClock.sleep(2000);
        addRegex(".*onClick bgJobGroupResult_Download_map_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        SystemClock.sleep(2000);

        waitForActivity(MGMapActivity.class);
        addRegex(".*onClick bgJobGroupConfirm_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        SystemClock.sleep(2000);
        addRegex(".*onClick bgJobGroupResult_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        animateSwipeToPos(getPoint4PointModel(new PointModelImpl(52.514087, 13.440732)), getCenterPos().point());
        animateMenu(R.id.menu_multi, R.id.mi_zoom_in);
        for (int i=0; i<5; i++)
            animateToViewAndClick(R.id.mi_zoom_in);
        for (int i=0; i<5; i++)
            animateToViewAndClick(R.id.mi_zoom_in);


        WaitUtil.doWait(this, 5000);
        mgLog.i("finished");
    }




}
