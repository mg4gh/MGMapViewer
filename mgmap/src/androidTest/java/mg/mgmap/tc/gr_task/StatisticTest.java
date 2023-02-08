package mg.mgmap.tc.gr_task;

import android.os.SystemClock;

import androidx.test.espresso.matcher.ViewMatchers;
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
import mg.mgmap.activity.statistic.TrackStatisticActivity;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;
import mg.mgmap.test.util.Mouse;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class StatisticTest extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());


    public StatisticTest() {
        mgLog.i("create");
        mgMapApplication.getSetup().wantSetup("testgroup002", androidTestAssets);
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule = new ActivityScenarioRule<>(MGMapActivity.class);

    @Test(timeout = 35000)
    public void _01_statistic_test() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.315814, 13.351981), (byte) 15);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        SystemClock.sleep(2000);

        setCursorToCenterPos();
        addRegex(".*onClick mi_statistic.*");
        animateToViewAndClick(R.id.menu_task);
        animateToViewAndClick(R.id.mi_statistic);
        waitForActivity(TrackStatisticActivity.class);
        animateTo(getCenterPos());

//        onView(withId(R.id.trackStatisticEntries))
//                .perform(RecyclerViewActions.actionOnItem(hasDescendant(TrackStatisticMatcher.matchTrack(".*20220712_104717.*")), ViewActions.click()));
//        TestViewAction tva = new TestViewAction((v)->animateToViewAndClick(v));

//        onView(withId(R.id.trackStatisticEntries))
//                .perform(RecyclerViewActions.scrollTo(TrackStatisticMatcher.matchTrack(".*20220712_104649.*")));

        Mouse.swipeUpSlow();
        Mouse.swipeUpSlow();

        animateToStatAndClick(".*20220712_104649.*");
        animateToStatAndClick(".*20221012_141638.*");
        animateToStatAndClick(".*20221029_122839.*");
//
//        onView(withId(R.id.trackStatisticEntries))
//                .perform(RecyclerViewActions.actionOnItem(TrackStatisticMatcher.matchTrack(".*20220712_104717.*"), new ViewCallbackAction((v)->trackStatisticView=v)));
//        animateToViewAndClick(trackStatisticView);

//
//        onView(allOf(withText("Elements.xml"))).perform(click());
//        SystemClock.sleep(2000);
////        PreferenceUtil.setPreference(settingsActivity, R.string.preference_choose_theme_key, "Elevate.xml");

        ViewMatchers.withId(1);

        SystemClock.sleep(5000);
        mgLog.i("finished");

    }
}