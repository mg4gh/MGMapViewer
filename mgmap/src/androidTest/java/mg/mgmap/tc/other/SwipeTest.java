package mg.mgmap.tc.other;

import android.os.SystemClock;

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

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.generic.util.WaitUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;
import mg.mgmap.test.util.Mouse;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class SwipeTest extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());


    public SwipeTest(){
        mgLog.i("create");
        mgMapApplication.getSetup().wantSetup("SETUP_002", androidTestAssets);
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);


    @Test(timeout = 60000)
    public void _01_settings_test() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.315814,13.351981), (byte) 15);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        WaitUtil.doWait(this, 2000);

        setCursorToCenterPos();
        Mouse.swipeUpSlow();
        SystemClock.sleep(500);
        Mouse.swipeRightSlow();
        SystemClock.sleep(500);
        Mouse.swipeDownSlow();
        SystemClock.sleep(500);
        Mouse.swipeLeftSlow();
        SystemClock.sleep(500);

        Mouse.swipeUp();
        SystemClock.sleep(2500);
        Mouse.swipeRight();
        SystemClock.sleep(2500);
        Mouse.swipeDown();
        SystemClock.sleep(2500);
        Mouse.swipeLeft();
        SystemClock.sleep(2500);

        WaitUtil.doWait(this, 5000);
        mgLog.i("finished");
    }



}
