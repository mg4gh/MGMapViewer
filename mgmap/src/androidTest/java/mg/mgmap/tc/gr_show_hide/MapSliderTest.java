package mg.mgmap.tc.gr_show_hide;


import android.graphics.Point;
import android.os.SystemClock;


import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
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
import mg.mgmap.activity.mgmap.view.LabeledSlider;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.BaseTestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4.class)
public class MapSliderTest extends BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private MGMapActivity mgMapActivity;

    public MapSliderTest(){
        mgLog.i("create");
        mgMapApplication.getSetup().wantSetup("SETUP_002", androidTestAssets);
    }

    @Rule
    public ActivityScenarioRule<MGMapActivity> activityRule =new ActivityScenarioRule<>(MGMapActivity.class);

    @Before
    public void initMapPosition(){
        mgMapActivity = waitForActivity(MGMapActivity.class);
        MapPosition mp = new MapPosition(new LatLong(54.315814,13.351981), (byte) 15);
        mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
    }

    @Test(timeout = 20000)
    public void _01_mapSliderTest() {
        mgLog.i("started");
        setCursorToCenterPos();
        addRegex(".*onClick mi_alpha_layers.*");
        animateToViewAndClick(R.id.menu_show_hide);
        animateToViewAndClick(R.id.mi_alpha_layers);

        LabeledSlider lsl = waitForView(LabeledSlider.class, R.id.slider_map2);
        Point p0 = lsl.getThumbPos(0);
        animateTo(p0);
        addRegex(".*context=MGMapActivity key=alpha_MAPSFORGE: ruegen.map value=0.0");
        animateClick(p0);
        Assert.assertEquals(0.0f, mgMapActivity.getPrefCache().get("alpha_MAPSFORGE: ruegen.map",0f).getValue(), 0.001f);
        SystemClock.sleep(2000);
        addRegex(".*context=MGMapActivity key=alpha_MAPSFORGE: ruegen.map value=0.2");
        addRegex(".*context=MGMapActivity key=alpha_MAPSFORGE: ruegen.map value=0.3.");
        addRegex(".*context=MGMapActivity key=alpha_MAPSFORGE: ruegen.map value=0.8");
        animateSwipeToPos(lsl.getThumbPos(20),lsl.getThumbPos(80));
        Assert.assertEquals(0.8f, mgMapActivity.getPrefCache().get("alpha_MAPSFORGE: ruegen.map",0f).getValue(), 0.001f);

        SystemClock.sleep(5000);
        mgLog.i("finished");
    }

    @Test(timeout = 30000)
    public void _02_trackSliderTest() {
        mgLog.i("started");
        setCursorToCenterPos();

        addRegex(".*onClick mi_marker_edit.*");
        animateToViewAndClick(R.id.menu_marker);
        animateToViewAndClick(R.id.mi_marker_edit);

        animateToPosAndClick(54.323734, 13.359813);
        animateToPosAndClick(54.311409, 13.346893);


        addRegex(".*onClick mi_alpha_tracks.*");
        animateToViewAndClick(R.id.menu_show_hide);
        animateToViewAndClick(R.id.mi_alpha_tracks);

        LabeledSlider lsl = waitForView(LabeledSlider.class, R.id.slider_rotl);
        Point p0 = lsl.getThumbPos(0);
        animateTo(p0);
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.0");
        animateClick(p0);
        Assert.assertEquals(0.0f, mgMapActivity.getPrefCache().get("FSRouting.alphaRoTL",0f).getValue(), 0.001f);
        SystemClock.sleep(2000);
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.2");
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.3.");
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.5");
        animateSwipeToPos(lsl.getThumbPos(20),lsl.getThumbPos(50));
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.6");
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.7.");
        addRegex(".*context=MGMapActivity key=FSRouting.alphaRoTL value=0.8");
        animateSwipeToPos(lsl.getThumbPos(60),lsl.getThumbPos(80));
        Assert.assertEquals(0.8f, mgMapActivity.getPrefCache().get("FSRouting.alphaRoTL",0f).getValue(), 0.001f);

        SystemClock.sleep(5000);
        mgLog.i("finished");
    }



}
