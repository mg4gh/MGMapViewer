package mg.mgmap.test.tc.features.control;

import android.graphics.PointF;
import android.util.Log;

import mg.mgmap.R;
import mg.mgmap.test.TestControl;
import mg.mgmap.test.Testcase;

public class TestFuSettingsGL extends Testcase {

    public TestFuSettingsGL(TestControl tc) {
        super(tc, Log.DEBUG);
    }

    @Override
    protected void setup() {
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),1000), 1100);
        tc.timer.postDelayed(() -> tc.doClick(this), 3000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(35,97),200), 4000);
        tc.timer.postDelayed(() -> tc.doClick(this), 5000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(20,42),600), 6500);
        tc.timer.postDelayed(() -> tc.doClick(this), 7500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(25,97),1000), 8500);
        tc.timer.postDelayed(() -> tc.doClick(this), 10000);
        tc.timer.postDelayed(rFinal, 12000);
        tc.timer.postDelayed(() -> tc.getPref(R.string.FSATL_pref_stlGl, false).setValue(false), 14000);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*  TestFuSettingsGL");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex("SettingsActivity.onCreate.* open PreferenceFragment mg.mgmap.activity.settings.FurtherPreferenceScreen");
        addRegex("PrefCache.onSharedPreferenceChanged.* key=FSATL.stlGl value=true");
        addRegex("mg.mgmap.test.TestControl.onResume.* set TestView MGMapActivity");
    }
}
