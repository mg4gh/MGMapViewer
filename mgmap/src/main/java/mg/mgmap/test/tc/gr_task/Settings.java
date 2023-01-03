package mg.mgmap.test.tc.gr_task;

import android.graphics.Point;
import android.view.MotionEvent;

import androidx.fragment.app.DialogFragment;
import androidx.preference.ListPreferenceDialogFragmentCompat;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.settings.SettingsActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.WaitUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.AbstractTestCase;
import mg.mgmap.test.TestControl;

public class Settings extends AbstractTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public Settings(MGMapApplication mgMapApplication) {
        super(mgMapApplication);
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

        Point clickPosGroupMulti = testControl.getViewClickPos("group_task");
        MGLog.sd(clickPosGroupMulti);
        if (clickPosGroupMulti != null){
            animateTo(clickPosGroupMulti, 1000);
        }
        testControl.setCurrentCursorPos(clickPosGroupMulti);
        doClick();

        Point clickPosZoomIn = testControl.getViewClickPos("settings");
        mgLog.d(clickPosZoomIn);
        if (clickPosZoomIn != null){
            animateTo(clickPosZoomIn, 1000);
            doClick();
        }
        animateTo(getCenterPosition(), 1000);
        WaitUtil.doWait(TestControl.class, 3000);
        animateTo(new Point(700,650), 1000);
        WaitUtil.doWait(TestControl.class, 1000);
        doClick();
        WaitUtil.doWait(TestControl.class, 500);
        animateTo(getCenterPosition(), 1000);
        SettingsActivity sa = testControl.getActivity(SettingsActivity.class);
        testControl.handleDialog(sa, sa.getResources().getString(R.string.preference_choose_theme_key), "Elements.xml");

        WaitUtil.doWait(TestControl.class, 3000);
        stop();
    }

    @Override
    protected void addRegexs() {
        addRegex(getName()+" start");
        addRegex("context=MGMapActivity key=FSBeeline.ZoomLevel value=15");
        addRegex("key=FSControl.qc_selector value=7");
        addRegex("context=MGMapActivity key=FSBeeline.ZoomLevel value=16");
        addRegex(getName()+" stop");
    }

}
