package mg.mgmap.tc.gr_search;

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

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class HelpTests extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public HelpTests(){
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
    public void _01_help1_wait_close() {
        mgLog.i("started");
        setCursorToCenterPos();
        addRegex(".*onClick mi_search_help.*");
        animateMenu(R.id.menu_search, R.id.mi_search_help);
        SystemClock.sleep(4000);
        addRegex(".*onClick help1_close.*");
        animateToViewAndClick(R.id.help1_close);
        SystemClock.sleep(1000);
        mgLog.i("finished");
    }

    @Test(timeout = 30000)
    public void _01_help2() {
        mgLog.i("started");
        setCursorToCenterPos();
        addRegex(".*onClick mi_search_help.*");
        animateMenu(R.id.menu_search, R.id.mi_search_help);

        animateToViewAndClick(R.id.testview); // nothing happens
        addRegex(".*onClick mi_search_empty2.*");
        animateToViewAndClick(R.id.mi_search_empty2);
        SystemClock.sleep(1000);
        mgLog.i("finished");
    }


    @Test(timeout = 30000)
    public void _03_empty() {
        mgLog.i("started");
        setCursorToCenterPos();
        addRegex(".*onClick mi_search_empty2.*");
        addRegex(".*context=MGMapActivity key=FSControl.qc_selector value=0.*");
        animateMenu(R.id.menu_search, R.id.mi_search_empty2);
        addRegex(".*onClick menu_gps.*"); // next click hits gps menu - this proves that previous click on R.id.mi_search_empty2 directly closed the menu_search
        PointOfView povGps = getClickPos(R.id.menu_gps);
        animateTo(povGps, 100);
        assert(povGps.view().getVisibility() == View.VISIBLE);
        animateClick(povGps);
        SystemClock.sleep(1000);
        mgLog.i("finished");
    }

}
