package mg.mgmap.tc.gr_show_hide;


import android.graphics.Point;
import android.os.SystemClock;


import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.view.LabeledSlider;
import mg.mgmap.activity.statistic.TrackStatisticActivity;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class ShowHideTest extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private MGMapActivity mgMapActivity;

    public ShowHideTest(){
        mgLog.i("create");
        mgMapApplication.getSetup().wantSetup("SETUP_002", androidTestAssets);
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);

    @Before
    public void initMapPosition(){
        mgMapActivity = waitForActivity(MGMapActivity.class);
        initPos(mgMapActivity, new PointModelImpl(54.315814,13.351981),(byte) 15);
    }

    @After
    public void cleanup(){
        mgMapActivity = null;
    }

    @Test(timeout = 50000)
    public void _01_mapSliderTest() {
        mgLog.i("started");
        setCursorToCenterPos();
        addRegex(".*onClick mi_alpha_layers.*");
        animateToViewAndClick(R.id.menu_show_hide);
        waitForPref(prefMenuInflated, true);
        assert(  mgMapActivity.findViewById(R.id.mi_alpha_layers).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_alpha_tracks).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_stl).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_atl).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_all).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_mtl).isEnabled() );
        animateToViewAndClick(R.id.mi_alpha_layers);

        LabeledSlider lsl = waitForView(LabeledSlider.class, R.id.slider_map2);
        Point p0 = lsl.getThumbPos(0);
        PointOfView pov = new PointOfView(p0, lsl);
        animateTo(pov);
        addRegex(".*context=MGMapActivity key=alpha_MAPSFORGE: ruegen.map value=0.0");
        animateClick(pov);
        Assert.assertEquals(0.0f, mgMapActivity.getPrefCache().get("alpha_MAPSFORGE: ruegen.map",0f).getValue(), 0.01f);
        SystemClock.sleep(2000);
        addRegex(".*context=MGMapActivity key=alpha_MAPSFORGE: ruegen.map value=0.2.?");
        addRegex(".*context=MGMapActivity key=alpha_MAPSFORGE: ruegen.map value=0.3.?");
        addRegex(".*context=MGMapActivity key=alpha_MAPSFORGE: ruegen.map value=0.8.?");
        animateSwipeToPos(lsl.getThumbPos(20),lsl.getThumbPos(81));
        Assert.assertEquals(0.8f, mgMapActivity.getPrefCache().get("alpha_MAPSFORGE: ruegen.map",0f).getValue(), 0.01f);

        SystemClock.sleep(5000);
        mgLog.i("finished");
    }

    @Test(timeout = 90000)
    public void _02_trackSliderTest() {
        mgLog.i("started");
        setCursorToCenterPos();

        addRegex(".*onClick mi_marker_edit.*");
        animateMenu(R.id.menu_marker, R.id.mi_marker_edit);

        animateToPosAndClick(54.323734, 13.359813);
        animateToPosAndClick(54.311409, 13.346893);


        addRegex(".*onClick mi_alpha_tracks.*");
        animateToViewAndClick(R.id.menu_show_hide);
        waitForPref(prefMenuInflated, true);
        assert(  mgMapActivity.findViewById(R.id.mi_alpha_layers).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_alpha_tracks).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_stl).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_atl).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_hide_all).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_hide_mtl).isEnabled() );
        animateToViewAndClick(R.id.mi_alpha_tracks);

        LabeledSlider lslRotl = waitForView(LabeledSlider.class, R.id.slider_rotl);
        Point p0 = lslRotl.getThumbPos(0);
        PointOfView pov = new PointOfView(p0, lslRotl);
        animateTo(pov);
        animateClick(pov);
        Assert.assertEquals(0.0f, mgMapActivity.getPrefCache().get("FSRouting.alphaRoTL",0f).getValue(), 0.001f);
        SystemClock.sleep(2000);
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.2.");
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.3.");
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.5.");
        animateSwipeToPos(lslRotl.getThumbPos(20),lslRotl.getThumbPos(51));
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.6.");
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.7.");
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.9.");
        animateSwipeToPos(lslRotl.getThumbPos(60),lslRotl.getThumbPos(100));
        Assert.assertEquals(1f, mgMapActivity.getPrefCache().get("FSRouting.alphaRoTL",0f).getValue(), 0.01f);

        LabeledSlider lslMtl = waitForView(LabeledSlider.class, R.id.slider_mtl);
        Assert.assertEquals(0.0f, mgMapActivity.getPrefCache().get("FSMarker.alphaMTL",0f).getValue(), 0.01f);
        SystemClock.sleep(2000);
        addRegex(".*context=MGMapActivity key=FSMarker.alphaMTL value=0.5.");
        addRegex(".*context=MGMapActivity key=FSMarker.alphaMTL value=0.7.");
        animateSwipeToPos(lslMtl.getThumbPos(50),lslMtl.getThumbPos(100));
        Assert.assertEquals(1f, mgMapActivity.getPrefCache().get("FSMarker.alphaMTL",0f).getValue(), 0.01f);
        addRegex(".*context=MGMapActivity key=FSMarker.alphaMTL value=0.4.");
        addRegex(".*context=MGMapActivity key=FSMarker.alphaMTL value=0.2.");
        animateSwipeToPos(lslMtl.getThumbPos(50),lslMtl.getThumbPos(20));
        animateSwipeToPos(lslMtl.getThumbPos(10),lslMtl.getThumbPos(0));
        Assert.assertEquals(0f, mgMapActivity.getPrefCache().get("FSMarker.alphaMTL",0f).getValue(), 0.01f);

        SystemClock.sleep(5000);
        mgLog.i("finished");
    }

    @Test(timeout = 60000)
    public void _03_hideStlAtlTest() {
        mgLog.i("started");
        setCursorToCenterPos();

        SystemClock.sleep(3000); // wait some time to make sure that init meta files is finished

        addRegex(".*onClick mi_statistic.*");
        animateMenu(R.id.menu_task, R.id.mi_statistic);
        waitForActivity(TrackStatisticActivity.class);
        waitForPref(prefMetaLoading, false); // make sure that there is no interference with end of meta loading
        SystemClock.sleep(100);

        animateToStatAndClick(".*20221029_122839.*");
        animateToStatAndClick(".*20221025_095831.*");
        animateToStatAndClick(".*20221018_104204.*");

        animateToViewAndClick(R.id.stat_mi_show);
        waitForActivity(MGMapActivity.class);
        assert (mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size() == 3);

        addRegex(".*onClick mi_hide_stl.*");
        animateToViewAndClick(R.id.menu_show_hide);
        waitForPref(prefMenuInflated, true);
        assert(  mgMapActivity.findViewById(R.id.mi_alpha_layers).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_alpha_tracks).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_hide_stl).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_hide_atl).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_hide_all).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_mtl).isEnabled() );
        animateToViewAndClick(R.id.mi_hide_stl);

        SystemClock.sleep(1000);

        addRegex(".*onClick mi_hide_atl.*");
        animateToViewAndClick(R.id.menu_show_hide);
        waitForPref(prefMenuInflated, true);
        assert(  mgMapActivity.findViewById(R.id.mi_alpha_layers).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_alpha_tracks).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_stl).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_hide_atl).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_hide_all).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_mtl).isEnabled() );
        animateToViewAndClick(R.id.mi_hide_atl);


        SystemClock.sleep(3000);
        mgLog.i("finished");
    }

    @Test(timeout = 60000)
    public void _04_hideAllTest() {
        mgLog.i("started");
        setCursorToCenterPos();

        SystemClock.sleep(3000); // wait some time to make sure that init meta files is finished

        addRegex(".*onClick mi_statistic.*");
        animateMenu(R.id.menu_task, R.id.mi_statistic);
        waitForActivity(TrackStatisticActivity.class);
        waitForPref(prefMetaLoading, false); // make sure that there is no interference with end of meta loading
        SystemClock.sleep(100);

        animateToStatAndClick(".*20221029_122839.*");
        animateToStatAndClick(".*20221025_095831.*");
        animateToStatAndClick(".*20221018_104204.*");

        animateToViewAndClick(R.id.stat_mi_show);
        waitForActivity(MGMapActivity.class);
        assert (mgMapApplication.availableTrackLogsObservable.availableTrackLogs.size() == 3);

        addRegex(".*onClick mi_hide_all.*");
        animateToViewAndClick(R.id.menu_show_hide);
        waitForPref(prefMenuInflated, true);
        assert(  mgMapActivity.findViewById(R.id.mi_alpha_layers).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_alpha_tracks).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_hide_stl).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_hide_atl).isEnabled() );
        assert(  mgMapActivity.findViewById(R.id.mi_hide_all).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_mtl).isEnabled() );
        animateToViewAndClick(R.id.mi_hide_all);

        SystemClock.sleep(1000);

        animateToViewAndClick(R.id.menu_show_hide);
        waitForPref(prefMenuInflated, true);
        assert(  mgMapActivity.findViewById(R.id.mi_alpha_layers).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_alpha_tracks).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_stl).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_atl).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_all).isEnabled() );
        assert( !mgMapActivity.findViewById(R.id.mi_hide_mtl).isEnabled() );

        SystemClock.sleep(3000);
        mgLog.i("finished");
    }

    @Test(timeout = 40000)
    public void _05_hideMtlTest() {
        mgLog.i("started");
        setCursorToCenterPos();

        addRegex(".*onClick mi_marker_edit.*");
        animateMenu(R.id.menu_marker, R.id.mi_marker_edit);

        animateToPosAndClick(54.323734, 13.359813);
        animateToPosAndClick(54.311409, 13.346893);


        addRegex(".*onClick mi_hide_mtl.*");
        animateToViewAndClick(R.id.menu_show_hide);
        waitForPref(prefMenuInflated, true);
        assert (mgMapActivity.findViewById(R.id.mi_alpha_layers).isEnabled());
        assert (mgMapActivity.findViewById(R.id.mi_alpha_tracks).isEnabled());
        assert (!mgMapActivity.findViewById(R.id.mi_hide_stl).isEnabled());
        assert (!mgMapActivity.findViewById(R.id.mi_hide_atl).isEnabled());
        assert (mgMapActivity.findViewById(R.id.mi_hide_all).isEnabled());
        assert (mgMapActivity.findViewById(R.id.mi_hide_mtl).isEnabled());
        animateToViewAndClick(R.id.mi_hide_mtl);

        SystemClock.sleep(1000);
        Assert.assertNull(mgMapApplication.markerTrackLogObservable.getTrackLog() );

    }


}
