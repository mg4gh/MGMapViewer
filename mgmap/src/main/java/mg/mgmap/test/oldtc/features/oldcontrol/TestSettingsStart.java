package mg.mgmap.test.oldtc.features.oldcontrol;

import android.graphics.PointF;
import android.util.Log;

import mg.mgmap.test.OldTestControl;
import mg.mgmap.test.OldTestcase;

public class TestSettingsStart extends OldTestcase {

    public TestSettingsStart(OldTestControl tc) {
        super(tc, Log.DEBUG);
    }

    @Override
    protected void setup() {
//        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,50),1), 1000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),1000), 1100);
        tc.timer.postDelayed(() -> tc.doClick(this), 3000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(21,97),200), 4000);
        tc.timer.postDelayed(() -> tc.doClick(this), 5000);
        tc.timer.postDelayed(() -> tc.doClick(this), 8000);
        tc.timer.postDelayed(rFinal, 10000);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*  TestSettingsStart");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex("SettingsActivity.onCreate.* open PreferenceFragment mg.mgmap.activity.settings.MainPreferenceScreen");
        addRegex("mg.mgmap.test.TestControl.onResume.* set TestView MGMapActivity");
    }
}