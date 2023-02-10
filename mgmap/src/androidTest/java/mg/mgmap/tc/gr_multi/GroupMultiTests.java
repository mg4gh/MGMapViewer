package mg.mgmap.tc.gr_multi;

import android.graphics.Point;
import android.os.SystemClock;
import android.view.View;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;


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
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class GroupMultiTests extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static boolean forceSetup = true;

    public GroupMultiTests(){
        mgLog.i("create");
        mgMapApplication.getSetup().wantSetup("SETUP_002", androidTestAssets, forceSetup);
        forceSetup = false;
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);

    @Before
    public void initMapPosition(){
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        MapPosition mp = new MapPosition(new LatLong(54.315814,13.351981), (byte) 15);
        mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
    }

    @Test(timeout = 20000)
    public void _01_help1_wait_close() {
        mgLog.i("started");
        setCursorToCenterPos();
        addRegex(".*onClick mi_help_multi.*");
        animateToViewAndClick(R.id.menu_multi);
        animateToViewAndClick(R.id.mi_help_multi);
        SystemClock.sleep(4000);
        addRegex(".*onClick help1_close.*");
        animateToViewAndClick(R.id.help1_close);
        SystemClock.sleep(1000);
        mgLog.i("finished");
    }

    @Test(timeout = 20000)
    public void _01_help2() {
        mgLog.i("started");
        setCursorToCenterPos();
        addRegex(".*onClick mi_help_multi.*");
        animateToViewAndClick(R.id.menu_multi);
        animateToViewAndClick(R.id.mi_help_multi);

        animateToViewAndClick(R.id.testview); // nothing happens
        addRegex(".*onClick mi_multi_empty1.*");
        animateToViewAndClick(R.id.mi_multi_empty1);
        SystemClock.sleep(1000);
        mgLog.i("finished");
    }


    @Test(timeout = 20000)
    public void _02_exit() {
        mgLog.i("started");
        setCursorToCenterPos();
        addRegex(".*onClick mi_exit.*");
        animateToViewAndClick(R.id.menu_multi);
        animateToViewAndClick(R.id.mi_exit);
        SystemClock.sleep(1000);
        mgLog.i("finished");
    }

    @Test(timeout = 20000)
    public void _03_empty() {
        mgLog.i("started");
        setCursorToCenterPos();
        addRegex(".*onClick mi_multi_empty1.*");
        addRegex(".*context=MGMapActivity key=FSControl.qc_selector value=0.*");
        animateToViewAndClick(R.id.menu_multi);
        Point p = animateToViewAndClick(R.id.mi_multi_empty1);
        addRegex(".*onClick menu_marker.*"); // next click hits marker menu - this proves that previous click on R.id.mi_multi_empty1 directly closed the menu_multi
        animateClick(p);
        SystemClock.sleep(1000);
        mgLog.i("finished");
    }

    @Test(timeout = 20000)
    public void _04_fullscreen() {
        mgLog.i("started");
        setCursorToCenterPos();
        int[] loc1 = new int[2], loc2 = new int[2], loc3 = new int[2];
        View vMenuMulti = waitForView(ExtendedTextView.class, R.id.menu_multi);
        vMenuMulti.getLocationOnScreen(loc1);
        animateToViewAndClick(R.id.menu_multi);
        addRegex(".*onClick mi_fullscreen.*");
        animateToViewAndClick(R.id.mi_fullscreen);
        SystemClock.sleep(1000);
        vMenuMulti.getLocationOnScreen(loc2);
        animateToViewAndClick(R.id.menu_multi);
        addRegex(".*onClick mi_fullscreen.*");
        animateToViewAndClick(R.id.mi_fullscreen);
        vMenuMulti.getLocationOnScreen(loc3);
        mgLog.i(loc1[0]+","+loc1[1]+" "+loc2[0]+","+loc2[1]+" "+loc3[0]+","+loc3[1]);
        assert ((loc1[0] == loc2[0]) && (loc1[0] == loc3[0]));
        assert ((loc1[1] > loc2[1]) && (loc1[1] == loc3[1]));
        animateTo(getCenterPos());
        SystemClock.sleep(1000);
        mgLog.i("finished");
    }

    @Test(timeout = 20000)
    public void _05_zoomIn() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.315814,13.351981), (byte) 15);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        SystemClock.sleep(2000);

        setCursorToCenterPos();
        addRegex(".*onClick mi_zoom_in.*");
        animateToViewAndClick(R.id.menu_multi);
        animateToViewAndClick(R.id.mi_zoom_in);
        animateTo(getCenterPos());
        SystemClock.sleep(5000);
        mgLog.i("finished");
    }



    @Test(timeout = 20000)
    public void _06_zoomOut() {
        mgLog.i("started");
        waitForActivity(MGMapActivity.class);
        setCursorToCenterPos();
        addRegex(".*onClick mi_zoom_out.*");
        animateToViewAndClick(R.id.menu_multi);
        animateToViewAndClick(R.id.mi_zoom_out);
        animateTo(getCenterPos());
        SystemClock.sleep(2000);
        mgLog.i("finished");
    }


    @Test(timeout = 20000)
    public void _07_home() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.315814,13.351981), (byte) 15);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        SystemClock.sleep(2000);

        setCursorToCenterPos();
        addRegex(".*onClick mi_home.*");
        animateToViewAndClick(R.id.menu_multi);
        animateToViewAndClick(R.id.mi_home);
        SystemClock.sleep(1000);
        mgLog.i("finished");
    }

}
