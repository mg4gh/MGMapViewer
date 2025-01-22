package mg.mgmap.tc.gr_search;

import android.graphics.Point;
import android.os.SystemClock;
import android.view.View;
import android.widget.TextView;

import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.matcher.ViewMatchers;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
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
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class GroupSerachTests extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public GroupSerachTests(){
        mgLog.i("create");
        mgMapApplication.getSetup().wantSetup("SETUP_002", androidTestAssets);
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);

    @Before
    public void initMapPosition(){
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        initPos(mgMapActivity, new PointModelImpl(54.315814,13.351981),(byte) 15);
    }

    @Test(timeout = 40000)
    public void _01_search_graphhopper() {
        mgLog.i("started");
        SystemClock.sleep(1000);
        Assert.assertEquals("Graphhopper", mgMapApplication.getPrefCache().get(R.string.preference_choose_search_key, "").getValue());
        setCursorToCenterPos();
        addRegex(".*onClick mi_search.*");
        animateMenu(R.id.menu_search, R.id.mi_search);
        SystemClock.sleep(1000);

        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText("Hunne"));
        SystemClock.sleep(1000);
//        Assert.assertEquals ("", waitForView(TextView.class, R.id.search_result1).getText());
        Assert.assertEquals(currentActivity.findViewById(R.id.search_result1).getVisibility(), View.INVISIBLE);
        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText("n"));
        SystemClock.sleep(1000);
        Assert.assertTrue (waitForView(TextView.class, R.id.search_result1).getText().toString().contains("Hunnen"));
        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText("s"));
        SystemClock.sleep(1000);
        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText("t"));
        SystemClock.sleep(1000);
        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText("r"));
        SystemClock.sleep(1000);
        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText(" "));
        SystemClock.sleep(1000);
        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText("1"));
        SystemClock.sleep(1000);
        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText("3"));
        SystemClock.sleep(1000);
        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText(","));
        SystemClock.sleep(1000);
        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText("Garz"), ViewActions.pressImeActionButton());
        SystemClock.sleep(1000);
        animateToViewAndClick(R.id.search_result2);

        PointModel pmCenter = getPointModel4Point(getCenterPos().point());
        Assert.assertEquals(54.317866, pmCenter.getLat(), 0.00001);
        Assert.assertEquals(13.34888, pmCenter.getLon(), 0.00001);
        SystemClock.sleep(2000);

        mgLog.i("finished");
    }

    @Test(timeout = 40000)
    public void _02_reverse_search_graphhopper() {
        mgLog.i("started");
        SystemClock.sleep(1000);
        Assert.assertEquals("Graphhopper", mgMapApplication.getPrefCache().get(R.string.preference_choose_search_key, "").getValue());
        setCursorToCenterPos();
        addRegex(".*onClick mi_search.*");
        animateMenu(R.id.menu_search, R.id.mi_search);
        SystemClock.sleep(1000);

        Point pSearch = getPoint4PointModel(new PointModelImpl(54.317866, 13.34888));
        animateSwipeToPos(pSearch,pSearch); // long click pSearch

        Assert.assertTrue (waitForView(TextView.class, R.id.search_result1).getText().toString().matches(".*Hunnenstraße 13a.*Garz.*"));
        SystemClock.sleep(2000);

        mgLog.i("finished");
    }

    @Test(timeout = 40000)
    public void _03_search_nominatim() {
        mgLog.i("started");
        SystemClock.sleep(1000);
        Pref<String> prefSearchProvider = mgMapApplication.getPrefCache().get(R.string.preference_choose_search_key, "");
        prefSearchProvider.setValue("Nominatim");
//        Assert.assertEquals("Graphhopper", mgMapApplication.getPrefCache().get(R.string.preference_choose_search_key, "").getValue());
        setCursorToCenterPos();
        addRegex(".*onClick mi_search.*");
        animateMenu(R.id.menu_search, R.id.mi_search);
        SystemClock.sleep(1000);

        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText("Hunnenstr 13, Garz"));
        SystemClock.sleep(1000);
        Assert.assertEquals(currentActivity.findViewById(R.id.search_result1).getVisibility(), View.INVISIBLE);
//        Assert.assertEquals ("", waitForView(TextView.class, R.id.search_result1).getText());
        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.pressImeActionButton());
        SystemClock.sleep(1000);
        animateToViewAndClick(R.id.search_result1);

        PointModel pmCenter = getPointModel4Point(getCenterPos().point());
        Assert.assertEquals(54.317751, pmCenter.getLat(), 0.00001);
        Assert.assertEquals(13.349125, pmCenter.getLon(), 0.00001);
        SystemClock.sleep(2000);

        mgLog.i("finished");
    }

    @Test(timeout = 40000)
    public void _04_reverse_search_nominatim() {
        mgLog.i("started");
        SystemClock.sleep(1000);
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        MapPosition mp = new MapPosition(new LatLong(54.317,13.35), (byte) 17);
        mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);

        Pref<String> prefSearchProvider = mgMapApplication.getPrefCache().get(R.string.preference_choose_search_key, "");
        prefSearchProvider.setValue("Nominatim");
        setCursorToCenterPos();
        addRegex(".*onClick mi_search.*");
        animateMenu(R.id.menu_search, R.id.mi_search);
        SystemClock.sleep(1000);

        Point pSearch = getPoint4PointModel(new PointModelImpl(54.317866, 13.34888));
        animateSwipeToPos(pSearch,pSearch); // long click pSearch

        Assert.assertTrue (waitForView(TextView.class, R.id.search_result1).getText().toString().matches(".*Hunnenstraße.*Garz.*"));
        SystemClock.sleep(2000);

        mgLog.i("finished");
    }

    @Test(timeout = 40000)
    public void _05_search_POI() {
        mgLog.i("started");
        SystemClock.sleep(1000);
        Pref<String> prefSearchProvider = mgMapApplication.getPrefCache().get(R.string.preference_choose_search_key, "");
        prefSearchProvider.setValue("POI");
//        Assert.assertEquals("Graphhopper", mgMapApplication.getPrefCache().get(R.string.preference_choose_search_key, "").getValue());
        setCursorToCenterPos();
        addRegex(".*onClick mi_search.*");
        animateMenu(R.id.menu_search, R.id.mi_search);
        SystemClock.sleep(1000);

        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText("Hunnenstr Garz"));
        SystemClock.sleep(1000);
//        Assert.assertEquals ("", waitForView(TextView.class, R.id.search_result1).getText());
        Assert.assertEquals(currentActivity.findViewById(R.id.search_result1).getVisibility(), View.INVISIBLE);
        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText(""), ViewActions.pressImeActionButton());
        SystemClock.sleep(1000);
        animateToViewAndClick(R.id.search_result1);

        PointModel pmCenter = getPointModel4Point(getCenterPos().point());
        Assert.assertEquals(54.317257, pmCenter.getLat(), 0.00001);
        Assert.assertEquals(13.348735, pmCenter.getLon(), 0.00001);
        SystemClock.sleep(2000);

        mgLog.i("finished");
    }

    @Test(timeout = 40000)
    public void _06_reverse_search_POI() {
        mgLog.i("started");
        SystemClock.sleep(1000);
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        MapPosition mp = new MapPosition(new LatLong(54.317,13.35), (byte) 17);
        mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);

        Pref<String> prefSearchProvider = mgMapApplication.getPrefCache().get(R.string.preference_choose_search_key, "");
        prefSearchProvider.setValue("POI");
        setCursorToCenterPos();
        addRegex(".*onClick mi_search.*");
        animateMenu(R.id.menu_search, R.id.mi_search);
        SystemClock.sleep(1000);

        Point pSearch = getPoint4PointModel(new PointModelImpl(54.317257, 13.348735));
        animateSwipeToPos(pSearch,pSearch); // long click pSearch

        Assert.assertTrue (waitForView(TextView.class, R.id.search_result1).getText().toString().matches(".*Hunnenstraße.*Garz.*"));
        SystemClock.sleep(2000);

        mgLog.i("finished");
    }


    @Test(timeout = 40000)
    public void _07_GeoLatLong() {
        mgLog.i("started");
        SystemClock.sleep(1000);
        Pref<String> prefSearchProvider = mgMapApplication.getPrefCache().get(R.string.preference_choose_search_key, "");
        prefSearchProvider.setValue("GeoLatLong");
        setCursorToCenterPos();
        addRegex(".*onClick mi_search.*");
        animateMenu(R.id.menu_search, R.id.mi_search);
        SystemClock.sleep(1000);

        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText("54.317257, 13.348735"));
        SystemClock.sleep(1000);
        Assert.assertEquals ("Lat=54.317257, Long=13.348735", waitForView(TextView.class, R.id.search_result1).getText());
        Espresso.onView(ViewMatchers.withId(R.id.search_edit_text)).perform(ViewActions.typeText(""), ViewActions.pressImeActionButton());
        SystemClock.sleep(1000);
        animateToViewAndClick(R.id.search_result1);

        PointModel pmCenter = getPointModel4Point(getCenterPos().point());
        Assert.assertEquals(54.317257, pmCenter.getLat(), 0.00001);
        Assert.assertEquals(13.348735, pmCenter.getLon(), 0.00001);
        SystemClock.sleep(2000);

        mgLog.i("finished");
    }

    @Test(timeout = 40000)
    public void _08_reverse_search_GeoLatLong() {
        mgLog.i("started");
        SystemClock.sleep(1000);
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        MapPosition mp = new MapPosition(new LatLong(54.317,13.35), (byte) 17);
        mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);

        Pref<String> prefSearchProvider = mgMapApplication.getPrefCache().get(R.string.preference_choose_search_key, "");
        prefSearchProvider.setValue("GeoLatLong");
        setCursorToCenterPos();
        addRegex(".*onClick mi_search.*");
        animateMenu(R.id.menu_search, R.id.mi_search);
        SystemClock.sleep(1000);

        Point pSearch = getPoint4PointModel(new PointModelImpl(54.317257, 13.348735));
        animateSwipeToPos(pSearch,pSearch); // long click pSearch

        SystemClock.sleep(1000);
        Assert.assertTrue (waitForView(TextView.class, R.id.search_result1).getText().toString().matches(".*Lat=54.31725.*Long=13.34873.*"));
        SystemClock.sleep(2000);

        mgLog.i("finished");
    }

}
