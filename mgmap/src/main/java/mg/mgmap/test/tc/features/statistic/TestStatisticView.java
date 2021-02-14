package mg.mgmap.test.tc.features.statistic;

import android.graphics.PointF;
import android.util.Log;

import mg.mgmap.R;
import mg.mgmap.test.TestControl;
import mg.mgmap.test.Testcase;

public class TestStatisticView extends Testcase {

    public TestStatisticView(TestControl tc) {
        super(tc, Log.DEBUG);
    }

    @Override
    protected void setup() {
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),1000), 1100);
        tc.timer.postDelayed(() -> tc.doClick(this), 3000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(49,97),200), 4000);
        tc.timer.postDelayed(() -> tc.doClick(this), 5000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(30,5),600), 6500);
        tc.timer.postDelayed(() -> tc.doClick(this), 7500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(40,97),1000), 8500);
        tc.timer.postDelayed(() -> tc.doClick(this), 10000);
        tc.timer.postDelayed(rFinal, 12000);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*  TestStatisticView");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex("ActivityLifecycleCallbackAdapter.onActivityResumed.*  TrackStatisticActivity");
        addRegex("TrackStatisticActivity.reworkState.*  .20201028_103630_GPS");
        addRegex("mg.mgmap.test.TestControl.onResume.* set TestView MGMapActivity");
        addRegex("PrefCache.onSharedPreferenceChanged.*  key=FSATL.STL_visibility value=true");
    }
}
