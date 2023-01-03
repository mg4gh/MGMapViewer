package mg.mgmap.test.tc.init;

import android.annotation.SuppressLint;
import android.graphics.Point;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.settings.SettingsActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.WaitUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.test.AbstractTestCase;
import mg.mgmap.test.TestControl;

public class Init1 extends AbstractTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public Init1(MGMapApplication mgMapApplication) {
        super(mgMapApplication);
    }


    @SuppressLint("RestrictedApi")
    @Override
    public void run() {
        super.run();

        MGMapActivity mgMapActivity = testControl.getActivity(MGMapActivity.class);
        if (mgMapActivity == null) return; // runs in background - do nothing

        setCursorPosition(getCenterPosition());
        mgMapApplication.unregisterAlertDialogs(mgMapActivity);


        animateTo(testControl.getViewClickPos("group_task"), 1000);
        doClick();
        animateTo(testControl.getViewClickPos("download"), 1000);
        doClick();

        WaitUtil.doWait(TestControl.class, 1000);


        animateTo(new Point(700,650), 1000);

        AppCompatActivity settingsActivity =  testControl.getActivity(SettingsActivity.class);
        Fragment f = settingsActivity.getSupportFragmentManager().getFragments().get(0);
        if (f instanceof PreferenceFragmentCompat) {
            PreferenceFragmentCompat pfc = (PreferenceFragmentCompat) f;

            Preference p = pfc.findPreference(settingsActivity.getResources().getString(R.string.preferences_dl_maps_eu_key));
//            pfc.getListView().getAdapter().

            p.performClick();
//            View v = pfc.getListView().getChildAt(2);
//            int[] loc = new int[2];
//            v.getLocationOnScreen(loc);
//            Point pt = new Point(loc[0]+v.getWidth()/2, loc[1]+v.getHeight()/2);
//            animateTo(pt, 1000);
        }
        mgLog.d("and click");
//        doClick();
        mgLog.d("and wait");
        WaitUtil.doWait(TestControl.class, 1000);
        mgMapActivity.runOnUiThread(() -> {
            mgMapApplication.unregisterAlertDialogs(testControl.getActivity(SettingsActivity.class), false);
        });
        WaitUtil.doWait(TestControl.class, 4000);

    }
}
