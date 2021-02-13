package mg.mgmap.test.tc.features.control;

import android.graphics.PointF;
import android.util.Log;

import mg.mgmap.test.Testcase;
import mg.mgmap.test.TestControl;

public class TestHelpGroup1 extends Testcase {

    public TestHelpGroup1(TestControl tc) {
        super(tc, Log.DEBUG);
    }

    @Override
    protected void setup() {
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,50),1), 1000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),2000), 1100);
        tc.timer.postDelayed(() -> tc.doClick(this), 3502);
        tc.timer.postDelayed(() -> tc.doClick(this), 5000);
        tc.timer.postDelayed(() -> tc.doClick(this), 9000);
        tc.timer.postDelayed(rFinal, 14000);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*  TestHelpGroup1");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex("FSControl.*  change help Visibility to true");
        addRegex("FSControl.*  change help Visibility to false");
        addRegex("key=FSControl.qc_selector value=0");
    }
}
