package mg.mgmap.tc.other;

import android.content.Context;
import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.features.bb.FSBB;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModelImpl;
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

        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.422888,13.448283),(byte) 14));
        setCursorToCenterPos();
        SystemClock.sleep(1000);

        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);
        assert prefBboxOn.getValue();

        PointModelImpl p1_3 = new PointModelImpl(54.427888,13.43528);
        PointModelImpl p2_3 = new PointModelImpl(54.408888,13.45528);
        FSBB fsbb = mgMapActivity.getFS(FSBB.class);
        while ((fsbb.getP1() == null) || (fsbb.getP2() == null)) SystemClock.sleep(50);
        animateSwipeLatLong(fsbb.getP1(), p1_3);
        animateSwipeLatLong(fsbb.getP2(), p2_3);

        addRegex(".*onClick mi_load_from_bb.*");
        animateMenu(R.id.menu_bb, R.id.mi_load_from_bb);
        Assert.assertEquals(3, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size() );

        addRegex(".*onClick mi_bbox.*");
        animateMenu(R.id.menu_bb, R.id.mi_bbox);
        assert !prefBboxOn.getValue();

        Assert.assertEquals(3, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size());
        Assert.assertNull(mgMapApplication.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog());

        animateToPosAndClick(54.416004,13.438092);
        Assert.assertEquals(3, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size());
        Assert.assertNotNull(mgMapApplication.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog());
        Assert.assertEquals("20230220_180637_MarkerRoute", mgMapApplication.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog().getName());

        animateToPosAndClick(54.416353,13.438265);
        Assert.assertEquals(3, mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size());
        Assert.assertNotNull(mgMapApplication.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog());
        Assert.assertEquals("20230207_060252_MarkerRoute", mgMapApplication.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog().getName());

        SystemClock.sleep(2000);
        animateTo(getCenterPos());

        SystemClock.sleep(5000);
        mgLog.i("finished");
    }


}
