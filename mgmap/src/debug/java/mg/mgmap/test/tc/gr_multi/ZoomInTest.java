package mg.mgmap.test.tc.gr_multi;

import android.graphics.Point;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;

import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.WaitUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.AbstractTestCase;
import mg.mgmap.test.TestControl;

@SuppressWarnings("unused")
public class ZoomInTest extends AbstractTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public ZoomInTest(MGMapApplication mgMapApplication, TestControl testControl) {
        super(mgMapApplication, testControl);
    }

    public void run(){
        MGMapActivity mgMapActivity = testControl.getActivity(MGMapActivity.class);
        if (mgMapActivity == null) return; // runs in background - do nothing

        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.315814,13.351981), (byte) 14);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
            setCursorPosition(getCenterPosition());
        });
        WaitUtil.doWait(TestControl.class, 500);
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.315814,13.351981), (byte) 15);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
            setCursorPosition(getCenterPosition());
        });
        WaitUtil.doWait(TestControl.class, 2000);

        Point clickPosGroupMulti = testControl.getViewClickPos("group_multi");
        MGLog.sd(clickPosGroupMulti);
        if (clickPosGroupMulti != null){
            animateTo(clickPosGroupMulti, 1000);
        }
        testControl.setCurrentCursorPos(clickPosGroupMulti);
        doClick();

        Point clickPosZoomIn = testControl.getViewClickPos("zoom_in");
        mgLog.d(clickPosZoomIn);
        if (clickPosZoomIn != null){
            animateTo(clickPosZoomIn, 1000);
            doClick();
        }
        animateTo(getCenterPosition(), 1000);
        WaitUtil.doWait(TestControl.class, 1000);
        stop();
    }

    @Override
    protected void addRegexs() {
        addRegex(getName()+" start");
        addRegex("key=FSBeeline.ZoomLevel value=15");
        addRegex("onClick group_multi");
        addRegex("onClick zoom_in");
        addRegex("key=FSBeeline.ZoomLevel value=16");
        addRegex(getName()+" stop");
    }

}
