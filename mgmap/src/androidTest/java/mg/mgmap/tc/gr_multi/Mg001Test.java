package mg.mgmap.tc.gr_multi;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;


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
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class Mg001Test extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public Mg001Test(){
        mgLog.i("create");
        MGMapApplication mgMapApplication = (MGMapApplication)InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        Context ctx = InstrumentationRegistry.getInstrumentation().getContext();
        mgMapApplication.getSetup().wantSetup("testgroup001", ctx.getAssets());
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);

    @Test(timeout = 50000)
    public void _01_zoomIn() {
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
    public void _02_zoomOut() {
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

}
