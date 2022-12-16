package mg.mgmap.test.oldtc.features.oldcontrol;

import android.graphics.PointF;
import android.util.Log;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.test.OldTestControl;
import mg.mgmap.test.OldTestcase;
import mg.mgmap.generic.util.basic.NameUtil;

public class TestThemeSettings extends OldTestcase {

    public TestThemeSettings(OldTestControl tc) {
        super(tc);
    }

    @Override
    protected void setup() {
        timeout = 16000;
        tc.timer.postDelayed(() -> Log.i(MGMapApplication.LABEL, NameUtil.context()+" ("+name+")"),1000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,50),1), 1000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),2000), 1100);
        tc.timer.postDelayed(() -> tc.doClick(this), 3500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(93,97),1000), 4500);
        tc.timer.postDelayed(() -> tc.doClick(this), 6000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,48),1000), 7500);
        tc.timer.postDelayed(() -> tc.doClick(this), 9000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(20,97),1000), 12000);
        tc.timer.postDelayed(() -> tc.doClick(this), 13500);
        tc.timer.postDelayed(rFinal, 15000);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*  TestThemeSettings");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex("onActivityResumed.*  ThemeSettings");
        addRegex("MapViewerBase.*  recreate MGMapActivity due to key=PrefThemeChanged");
        addRegex("mg.mgmap.test.TestControl.onResume.* set TestView MGMapActivity");
    }
}
