package mg.mgmap.test.tc.gr_multi;

import android.graphics.Point;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.WaitUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.AbstractTestCase;
import mg.mgmap.test.TestControl;

@SuppressWarnings("unused")
public class FullscreenTest extends AbstractTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public FullscreenTest(MGMapApplication mgMapApplication, TestControl testControl) {
        super(mgMapApplication, testControl);
    }

    public void run(){
        MGMapActivity mgMapActivity = testControl.getActivity(MGMapActivity.class);
        if (mgMapActivity == null) return; // runs in background - do nothing

        // start test, set defined conditions
        mgMapActivity.runOnUiThread(() -> {
            MapPosition mp = new MapPosition(new LatLong(54.315814,13.351981), (byte) 15);
            mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
            setCursorPosition(getCenterPosition());
        });
        WaitUtil.doWait(TestControl.class, 2000);

        // document current fullscreen state (expect true)
        Point ptScreenSize = getPercentPosition(100,100);
        Point ptTestViewSize = testControl.currentTestView.getPxSize();
        mgLog.d("prefFullscreen="+mgMapApplication.getPrefCache().get(R.string.FSControl_qcFullscreenOn, true).getValue());
        mgLog.d(ptScreenSize+" "+ptTestViewSize+" result1="+ptTestViewSize.equals(ptScreenSize));

        // select menu multi
        Point clickPosGroupMulti = testControl.getViewClickPos("group_multi");
        MGLog.sd(clickPosGroupMulti);
        if (clickPosGroupMulti != null){
            animateTo(clickPosGroupMulti, 1000);
        }
        doClick();

        // ... and action fullscreen
        Point clickPosFullscreen = testControl.getViewClickPos("fullscreen");
        mgLog.d(clickPosFullscreen);
        if (clickPosFullscreen != null){
            animateTo(clickPosFullscreen, 1000);
            doClick();
        }

        WaitUtil.doWait(TestControl.class, 1000);

        // document new fullscreen state (expect false)
        ptTestViewSize = testControl.currentTestView.getPxSize();
        mgLog.d("prefFullscreen="+mgMapApplication.getPrefCache().get(R.string.FSControl_qcFullscreenOn, true).getValue());
        mgLog.d(ptScreenSize+" "+ptTestViewSize+" result2="+ptTestViewSize.equals(ptScreenSize));

        // again menu multi
        Point clickPosGroupMulti2 = testControl.getViewClickPos("group_multi");
        assert clickPosGroupMulti != null;
        mgLog.d(clickPosGroupMulti+" "+clickPosGroupMulti2+" result3="+clickPosGroupMulti.equals(clickPosGroupMulti2));
        if (clickPosGroupMulti2 != null){
            animateTo(clickPosGroupMulti2, 1000);
        }
        testControl.setCurrentCursorPos(clickPosGroupMulti2);
        doClick();
        // and action fullscreen
        clickPosFullscreen = testControl.getViewClickPos("fullscreen");
        mgLog.d(clickPosFullscreen);
        if (clickPosFullscreen != null){
            animateTo(clickPosFullscreen, 1000);
            doClick();
        }
        mgLog.d("prefFullscreen="+mgMapApplication.getPrefCache().get(R.string.FSControl_qcFullscreenOn, true).getValue());
        WaitUtil.doWait(TestControl.class, 1000);

        animateTo(getCenterPosition(), 1000);
        WaitUtil.doWait(TestControl.class, 1000);
        stop();
    }

    @Override
    protected void addRegexs() {
        addRegex(getName()+" start");
        addRegex("FullscreenTest.java.*prefFullscreen=true");
        addRegex("FullscreenTest.java.*result1=true");
        addRegex("onClick group_multi");
        addRegex("onClick fullscreen");

        addRegex("FullscreenTest.java.*prefFullscreen=false");
        addRegex("FullscreenTest.java.*result2=true");
        addRegex("FullscreenTest.java.*result3=false");
        addRegex("onClick group_multi");
        addRegex("onClick fullscreen");

        addRegex("FullscreenTest.java.*prefFullscreen=true");
        addRegex(getName()+" stop");
    }

}
