package mg.mgmap.tc.gr_marker;

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
public class MarkerTest extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public MarkerTest(){
        mgLog.i("create");
        MGMapApplication mgMapApplication = (MGMapApplication)InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        Context ctx = InstrumentationRegistry.getInstrumentation().getContext();
        mgMapApplication.getSetup().wantSetup("SETUP_002", ctx.getAssets());
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);

    @Test(timeout = 50000)
    public void _01_marker() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.422888,13.448283), (byte) 14);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        setCursorToCenterPos();
        SystemClock.sleep(2000);

        addRegex(".*onClick mi_marker_edit.*");
        animateToViewAndClick(R.id.menu_marker);
        animateToViewAndClick(R.id.mi_marker_edit);

        animateToPosAndClick(54.420327,13.437579);
        animateToPosAndClick(54.415861,13.447284);
        animateToPosAndClick(54.418657,13.456617);
        animateToPosAndClick(54.424050,13.454930);
        animateToPosAndClick(54.421417,13.444976);
        animateToPosAndClick(54.423379,13.433771);

        addRegex(".*Test Statistic: .* totalLength=4315.88 gain=70.9 loss=79.8 minEle=11.1 maxEle=92.1 numPoints=129");
        mgLog.i("Test Statistic: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic());

        SystemClock.sleep(5000);
        animateSwipeLatLong(54.4240,13.454900, 54.421981, 13.450780);
        SystemClock.sleep(500);
        addRegex(".*Test Statistic: .* totalLength=3959.18 gain=70.9 loss=79.8 minEle=11.1 maxEle=92.1 numPoints=122");
        mgLog.i("Test Statistic: " + mgMapApplication.routeTrackLogObservable.getTrackLog().getTrackStatistic());

        SystemClock.sleep(5000);
        animateTo(getCenterPos());
        SystemClock.sleep(5000);
        mgLog.i("finished");
    }




}
