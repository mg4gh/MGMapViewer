package mg.mgmap.tc.gr_task;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

import android.os.SystemClock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

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
import mg.mgmap.activity.settings.SettingsActivity;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;
import mg.mgmap.test.util.PreferenceUtil;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class SettingTest extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());


    public SettingTest(){
        mgLog.i("create");
        mgMapApplication.getSetup().wantSetup("SETUP_002", androidTestAssets);
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);


    @Test(timeout = 20000)
    public void _01_settings_test() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.315814,13.351981), (byte) 15);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        SystemClock.sleep(2000);

        setCursorToCenterPos();
        addRegex(".*onClick mi_settings.*");
        animateToViewAndClick(R.id.menu_task);
        animateToViewAndClick(R.id.mi_settings);
        AppCompatActivity settingsActivity = waitForActivity(SettingsActivity.class);
        animateTo(getCenterPos());

        onView(withId(androidx.preference.R.id.recycler_view))
                .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.preference_choose_theme_title)), ViewActions.click()));

        onView(allOf(withText("Elements.xml"))).perform(click());
        SystemClock.sleep(2000);
        PreferenceUtil.setPreference(settingsActivity, R.string.preference_choose_theme_key, "Elevate.xml");

        SystemClock.sleep(2000);
        Espresso.pressBack();
        SystemClock.sleep(2000);
        mgLog.i("finished");
    }



}
