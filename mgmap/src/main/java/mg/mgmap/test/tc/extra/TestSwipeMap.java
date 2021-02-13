package mg.mgmap.test.tc.extra;

import android.graphics.PointF;

import mg.mgmap.test.TestControl;
import mg.mgmap.test.Testcase;

public class TestSwipeMap extends Testcase {

    public TestSwipeMap(TestControl tc) {
        super(tc);
    }

    @Override
    protected void setup() {
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,50),1), 1000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,70),2000), 1100);
//        tc.timer.postDelayed(() -> tc.doClick(this), 4000);
        tc.timer.postDelayed(() -> tc.swipeTo(this, new PointF(50,30),3000), 3500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,70),2000), 7100);
        tc.timer.postDelayed(() -> tc.swipeTo(this, new PointF(50,30),500), 8500);

        tc.timer.postDelayed(rFinal, 10000);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*  TestSwipeMap");
    }
}
