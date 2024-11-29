package mg.mgmap.tc.gr_task;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
//import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
//import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.core.AllOf.allOf;

import android.graphics.Point;
import android.os.SystemClock;

import androidx.appcompat.app.AppCompatActivity;
import androidx.test.espresso.Espresso;
//import androidx.test.espresso.action.ViewActions;
//import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mapsforge.core.model.Dimension;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.settings.SettingsActivity;
import mg.mgmap.generic.model.PointModelImpl;
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


    @Test(timeout = 60000)
    public void _01_settings_layers() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.315814,13.351981),(byte) 15));
        SystemClock.sleep(2000);

        setCursorToCenterPos();
        addRegex(".*onClick mi_settings.*");
        animateToViewAndClick(R.id.menu_task);
        animateToViewAndClick(R.id.mi_settings);

// keep this sample code in comment, just in case it will be needed later ... for whatever reason
//        onView(withId(androidx.preference.R.id.recycler_view))
//                .perform(RecyclerViewActions.actionOnItem(hasDescendant(withText(R.string.preference_choose_theme_title)), ViewActions.click()));
        addRegex(".*onClick.*key=SelectMap3.*");
        animateToPrefAndClick(R.string.Layers_pref_chooseMap3_key);

        onView(allOf(withText("MAPGRID: hgt"))).perform(click());
        Assert.assertEquals("MAPGRID: hgt", mgMapApplication.getSharedPreferences().getString(mgMapApplication.getResources().getString(R.string.Layers_pref_chooseMap3_key),""));
        SystemClock.sleep(2000);

        Espresso.pressBack();
        SystemClock.sleep(2000);
        mgLog.i("finished");
    }



    @Test(timeout = 60000)
    public void _02_settings_theme() {
        mgLog.i("started");
        String elevatexy="Elevate5.5";
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.315814,13.351981),(byte) 15));
        SystemClock.sleep(2000);
        Dimension dim = mgMapActivity.getMapsforgeMapView().getDimension();

        setCursorToCenterPos();
        addRegex(".*onClick mi_settings.*");
        animateToViewAndClick(R.id.menu_task);
        animateToViewAndClick(R.id.mi_settings);
        AppCompatActivity settingsActivity = waitForActivity(SettingsActivity.class);

        animateSwipeToPos(new Point(dim.width/2,(dim.height*4)/5), new Point(dim.width/2,dim.height/5));
        SystemClock.sleep(2000);

        Assert.assertEquals(elevatexy+"/Elevate.xml", mgMapApplication.getSharedPreferences().getString(mgMapApplication.getResources().getString(R.string.preference_choose_theme_key),""));

        animateToPrefAndClick(R.string.preference_choose_theme_key);
        onView(allOf(withText(elevatexy+"/Elements.xml"))).perform(click());
        Assert.assertEquals(elevatexy+"/Elements.xml", mgMapApplication.getSharedPreferences().getString(mgMapApplication.getResources().getString(R.string.preference_choose_theme_key),""));
        SystemClock.sleep(2000);

        PreferenceUtil.setPreference(settingsActivity, R.string.preference_choose_theme_key, elevatexy+"/Elevate.xml");
        SystemClock.sleep(200);
        Assert.assertEquals(elevatexy+"/Elevate.xml", mgMapApplication.getSharedPreferences().getString(mgMapApplication.getResources().getString(R.string.preference_choose_theme_key),""));
        SystemClock.sleep(2000);

        Espresso.pressBack();
        SystemClock.sleep(2000);
        mgLog.i("finished");
    }


    @Test(timeout = 60000)
    public void _03_settings_search() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.315814,13.351981),(byte) 15));
        SystemClock.sleep(2000);

        setCursorToCenterPos();
        addRegex(".*onClick mi_settings.*");
        animateToViewAndClick(R.id.menu_task);
        animateToViewAndClick(R.id.mi_settings);
        AppCompatActivity settingsActivity = waitForActivity(SettingsActivity.class);

        Assert.assertEquals("Graphhopper", mgMapApplication.getSharedPreferences().getString(mgMapApplication.getResources().getString(R.string.preference_choose_search_key),""));


        animateToPrefAndClick(R.string.preference_choose_search_key);
        onView(allOf(withText("POI"))).perform(click());
        Assert.assertEquals("POI", mgMapApplication.getSharedPreferences().getString(mgMapApplication.getResources().getString(R.string.preference_choose_search_key),""));
        SystemClock.sleep(1000);

        PreferenceUtil.setPreference(settingsActivity, R.string.preference_choose_search_key, "Nominatim");
        SystemClock.sleep(200);
        Assert.assertEquals("Nominatim", mgMapApplication.getSharedPreferences().getString(mgMapApplication.getResources().getString(R.string.preference_choose_search_key),""));
        SystemClock.sleep(1000);

        animateToPrefAndClick(R.string.preference_choose_search_key);
        onView(allOf(withText("GeoLatLong"))).perform(click());
        Assert.assertEquals("GeoLatLong", mgMapApplication.getSharedPreferences().getString(mgMapApplication.getResources().getString(R.string.preference_choose_search_key),""));
        SystemClock.sleep(2000);

        Espresso.pressBack();
        SystemClock.sleep(2000);
        mgLog.i("finished");
    }



}
