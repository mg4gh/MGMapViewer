package mg.mgmap.tc.gr_task;

import android.graphics.Point;
import android.os.SystemClock;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.statistic.TrackStatisticActivity;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class StatisticTest extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public StatisticTest() {
        mgLog.i("create");
        mgMapApplication.getSetup().wantSetup("SETUP_002", androidTestAssets);
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule = new ActivityScenarioRule<>(MGMapActivity.class);

    @Test(timeout = 60000)
    public void _01_statistic_test() {
        mgLog.i("started");
        MGMapActivity mgMapActivity = waitForActivity(MGMapActivity.class);
        mgMapActivity.runOnUiThread(() -> initPos(mgMapActivity, new PointModelImpl(54.315814,13.351981),(byte) 15));
        SystemClock.sleep(2000);
        Pref<Boolean> metaLoading = mgMapApplication.getPrefCache().get(R.string.MGMapApplication_pref_MetaData_loading, true);
        int cnt =0;
        while (metaLoading.getValue()){
            SystemClock.sleep(200);
            mgLog.i("wait for finish of metadata loading, cnt="+ cnt++);
        }

        setCursorToCenterPos();
        addRegex(".*onClick mi_statistic.*");
        animateMenu(R.id.menu_task, R.id.mi_statistic);
        waitForActivity(TrackStatisticActivity.class);
        waitForPref(prefMetaLoading, false); // make sure that there is no interference with end of meta loading
        SystemClock.sleep(100);

        animateSwipeToPos(new Point(500,1000), new Point(500,300));
        animateSwipeToPos(new Point(500,1000), new Point(500,300));

        animateToStatAndClick(".*20220712_104649.*");
        animateToStatAndClick(".*20221012_141638.*");
        animateToStatAndClick(".*20221029_122839.*");

        SystemClock.sleep(5000);
        mgLog.i("finished");

    }
}