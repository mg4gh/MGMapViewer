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
public class GroupMultiTest extends AbstractTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public GroupMultiTest(MGMapApplication mgMapApplication, TestControl testControl) {
        super(mgMapApplication, testControl);
        durationLimit = 30000;
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



         // select menu multi
        Point clickPosGroupMulti = testControl.getViewClickPos("group_multi");
        MGLog.sd(clickPosGroupMulti);
        if (clickPosGroupMulti != null){
            animateTo(clickPosGroupMulti, 1000);
        }
        doClick();
        WaitUtil.doWait(TestControl.class, 5000); // wait at least 3s menu visible + 0,5s task menu blocked

        // click again menu multi
        doClick();
        // select menu item help
        animateTo(testControl.getViewClickPos("help"), 1000);
        doClick();
        WaitUtil.doWait(TestControl.class, 5000); // check that menu doesn't jump back

        animateTo(getCenterPosition(), 1000);
        doClick(); // nothing shell happen

        // now go to the help exit button and click it
        Point ptClickPosHelp1 = testControl.getViewClickPos("help1");
        if (ptClickPosHelp1 != null){
            animateTo(ptClickPosHelp1, 1000);
            doClick();
        }

        WaitUtil.doWait(TestControl.class, 1000);

        animateTo(getCenterPosition(), 1000);
        WaitUtil.doWait(TestControl.class, 1000);
        stop();
    }

    @Override
    protected void addRegexs() {
        addRegex(getName()+" start");

        addRegex("onClick group_multi");
        addRegex("context=MGMapActivity key=FSControl.qc_selector value=7");
        addRegex("context=MGMapActivity key=FSControl.qc_selector value=0");
        addRegex("setEnableMenu.* enable=false");
        addRegex("setEnableMenu.* enable=true");

        addRegex("onClick group_multi");
        addRegex("onClick help");
        addRegex("change help Visibility to true");

        addRegex("onClick help1");
        addRegex("change help Visibility to false");

        addRegex(getName()+" stop");
    }

}
