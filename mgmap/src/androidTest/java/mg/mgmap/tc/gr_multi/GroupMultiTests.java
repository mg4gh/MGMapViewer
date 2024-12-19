package mg.mgmap.tc.gr_multi;

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
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class GroupMultiTests extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public GroupMultiTests(){
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

    @Test(timeout = 30000)
    public void _04_fullscreen() {
        mgLog.i("started");
        setCursorToCenterPos();
        int[] loc1 = new int[2], loc2 = new int[2], loc3 = new int[2];
        View vMenuMulti = waitForView(ExtendedTextView.class, R.id.menu_multi);
        vMenuMulti.getLocationOnScreen(loc1);
        addRegex(".*onClick mi_fullscreen.*");
        animateMenu(R.id.menu_multi, R.id.mi_fullscreen);

        SystemClock.sleep(1000);
        vMenuMulti.getLocationOnScreen(loc2);
        addRegex(".*onClick mi_fullscreen.*");
        animateMenu(R.id.menu_multi, R.id.mi_fullscreen);
        vMenuMulti.getLocationOnScreen(loc3);
        mgLog.i(loc1[0]+","+loc1[1]+" "+loc2[0]+","+loc2[1]+" "+loc3[0]+","+loc3[1]);
        assert ((loc1[0] == loc2[0]) && (loc1[0] == loc3[0]));
        assert ((loc1[1] > loc2[1]) && (loc1[1] == loc3[1]));
        animateTo(getCenterPos());
        SystemClock.sleep(1000);
        mgLog.i("finished");
    }

    @Test(timeout = 30000)
    public void _05_zoomIn() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.315814,13.351981),(byte) 15));
        SystemClock.sleep(2000);

        setCursorToCenterPos();
        addRegex(".*onClick mi_zoom_in.*");
        animateMenu(R.id.menu_multi, R.id.mi_zoom_in);
        animateTo(getCenterPos());
        SystemClock.sleep(5000);
        mgLog.i("finished");
    }



    @Test(timeout = 30000)
    public void _06_zoomOut() {
        mgLog.i("started");
        waitForActivity(MGMapActivity.class);
        setCursorToCenterPos();
        addRegex(".*onClick mi_zoom_out.*");
        animateMenu(R.id.menu_multi, R.id.mi_zoom_out);
        animateTo(getCenterPos());
        SystemClock.sleep(2000);
        mgLog.i("finished");
    }


    @Test(timeout = 30000)
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
        animateMenu(R.id.menu_multi, R.id.mi_home);
        SystemClock.sleep(1000);
        mgLog.i("finished");
    }

}
