package mg.mgmap.tc.init;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static org.hamcrest.core.AllOf.allOf;

import android.content.Intent;
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
import mg.mgmap.generic.util.WaitUtil;
import mg.mgmap.generic.util.basic.MGLog;
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
        waitForActivity(MGMapActivity.class);

        setCursorToCenterPos();
        addRegex(".*onClick HintInitialMapDownload_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        addRegex(".*onClick menu_task.*");
        animateToViewAndClick(R.id.menu_task);
        addRegex(".*onClick mi_download.*");
        animateToViewAndClick(R.id.mi_download);
        AppCompatActivity settingsActivity = waitForActivity(SettingsActivity.class);
        addRegex(".*onPreferenceClick key=DldMapsDEKey.*");
        animateToPrefAndClick(R.string.preferences_dl_maps_de_key);

        SystemClock.sleep(2000); // now you see the browser
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("mf-v4-map://ftp.gwdg.de/pub/misc/openstreetmap/openandromaps/mapsV5/germany/Berlin.zip"));
        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        settingsActivity.startActivity(intent);

        waitForActivity(MGMapActivity.class);
        setCursorVisibility(true);
        SystemClock.sleep(2000);
        addRegex(".*onClick bgJobGroupConfirm_Download_map_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        SystemClock.sleep(2000);
        addRegex(".*onClick bgJobGroupResult_Download_map_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        waitForActivity(SettingsActivity.class);
        setCursorVisibility(true);
        SystemClock.sleep(1000);
        addRegex(".*onClick HintMapLayerAssignment_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        addRegex(".*onClick.*key=SelectMap2.*");
        animateToPrefAndClick(R.string.Layers_pref_chooseMap2_key);

        addRegex(".*recreate MGMapActivity due to key=SelectMap2 value=MAPSFORGE: Berlin_oam.osm.map.*");
        onView(allOf(withText("MAPSFORGE: Berlin_oam.osm.map"))).perform(click());

        SystemClock.sleep(2000);
        pressBack();

        waitForActivity(MGMapActivity.class);
        setCursorVisibility(true);
        SystemClock.sleep(2000);
        addRegex(".*onClick bgJobGroupConfirm_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);
        SystemClock.sleep(2000);
        addRegex(".*onClick bgJobGroupResult_Download_height_data_btPositive.*");
        animateToViewAndClick(R.id.bt_dialog_positive);

        animateTo(getCenterPos());

        WaitUtil.doWait(this, 5000);
        mgLog.i("finished");
    }




}
