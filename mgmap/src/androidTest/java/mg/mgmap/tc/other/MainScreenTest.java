package mg.mgmap.tc.other;

import android.content.Context;
import android.graphics.Point;
import android.os.SystemClock;
import android.util.DisplayMetrics;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
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
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class MainScreenTest extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public MainScreenTest(){
        mgLog.i("create");
        MGMapApplication mgMapApplication = (MGMapApplication)InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        Context ctx = InstrumentationRegistry.getInstrumentation().getContext();
        mgMapApplication.getSetup().wantSetup("SETUP_003", ctx.getAssets());
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);

    @Test(timeout = 60000)
    public void _01_close_track() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);

        Pref<Boolean> prefBboxOn = mgMapActivity.getPrefCache().get(R.string.FSBB_qc_bboxOn, false);
        assert !prefBboxOn.getValue();

        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.422888,13.448283), (byte) 14);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
        });
        setCursorToCenterPos();
        SystemClock.sleep(1000);

        DisplayMetrics dm = mgMapApplication.getApplicationContext().getResources().getDisplayMetrics();
        double x1 = dm.widthPixels / 3.0;
        double x2 = x1 * 2;
        double y1 = (dm.heightPixels / 2.0) - (x1 / 2);
        double y2 = (dm.heightPixels / 2.0) + (x1 / 2);
        int ix1 = (int)x1;
        int ix2 = (int)x2;
        int iy1 = (int)y1;
        int iy2 = (int)y2;
        Point p1 = new Point(ix1, iy1);
        Point p2 = new Point(ix2, iy2);

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);
        assert prefBboxOn.getValue();

        animateSwipeToPos(p1,new Point(ix1-200,iy1-100));
        animateSwipeToPos(p2,new Point(ix2,iy2+500));

        addRegex(".*onClick mi_load_from_bb.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_load_from_bb);
        Assert.assertEquals(3, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size() );

        addRegex(".*onClick mi_bbox.*");
        animateToViewAndClick(R.id.menu_bb);
        animateToViewAndClick(R.id.mi_bbox);
        assert !prefBboxOn.getValue();

        Assert.assertEquals(3, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size());
        Assert.assertNull(mgMapApplication.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog());

        animateToPosAndClick(54.416004,13.438092);
        Assert.assertEquals(3, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size());
        Assert.assertNotNull(mgMapApplication.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog());
        Assert.assertEquals("20230220_180637_MarkerRoute", mgMapApplication.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog().getName());

        animateToPosAndClick(54.416353,13.437865);
        Assert.assertEquals(3, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size());
        Assert.assertNotNull(mgMapApplication.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog());
        Assert.assertEquals("20230207_060252_MarkerRoute", mgMapApplication.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog().getName());

        SystemClock.sleep(2000);
        animateTo(getCenterPos());

        SystemClock.sleep(5000);
        mgLog.i("finished");
    }


}
