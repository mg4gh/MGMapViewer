package mg.mgmap.test.oldtc.features.oldcontrol;

import android.graphics.PointF;

import mg.mgmap.test.OldTestControl;
import mg.mgmap.test.OldTestcase;

public class TestTriggerStatistic extends OldTestcase {

    public TestTriggerStatistic(OldTestControl tc) {
        super(tc);
    }

    @Override
    protected void setup() {
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,50),1), 1000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),2000), 1100);
        tc.timer.postDelayed(() -> tc.doClick(this), 3500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,97),1000), 4500);
        tc.timer.postDelayed(() -> tc.doClick(this), 6000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(93,97),1000), 9000);
        tc.timer.postDelayed(() -> tc.doClick(this), 11000);
        tc.timer.postDelayed(rFinal, 13000);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*  TestTriggerStatistic");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex("onActivityResumed.*  TrackStatisticActivity");
        addRegex("onActivityResumed.*  MGMapActivity");
    }
}
