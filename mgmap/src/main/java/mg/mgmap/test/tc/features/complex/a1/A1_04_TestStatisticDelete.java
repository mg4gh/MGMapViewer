package mg.mgmap.test.tc.features.complex.a1;

import android.graphics.PointF;
import android.util.Log;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.test.TestControl;
import mg.mgmap.test.Testcase;

public class A1_04_TestStatisticDelete extends Testcase {

    public A1_04_TestStatisticDelete(TestControl tc) {
        super(tc, Log.DEBUG);
    }

    @Override
    protected void setup() {
        timeout = 17000;
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),1000), 1100);
        tc.timer.postDelayed(() -> tc.doClick(this), 3000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(49,97),200), 4000);
        tc.timer.postDelayed(() -> tc.doClick(this), 5000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(30,5),600), 6500);
        tc.timer.postDelayed(() -> tc.doClick(this), 7500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(30,15),600), 8500);
        tc.timer.postDelayed(() -> tc.doClick(this), 9500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(83,97),600), 10500);
        tc.timer.postDelayed(() -> tc.doClick(this), 11500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(82,57),600), 12500);
        tc.timer.postDelayed(() -> tc.doClick(this), 13500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(94,97),600), 14500);
        tc.timer.postDelayed(() -> tc.doClick(this), 15500);
        tc.timer.postDelayed(() -> Log.i(MGMapApplication.LABEL, NameUtil.context()+" MetaTrackLogs.size="+tc.application.metaTrackLogs.size()), 16000);
        tc.timer.postDelayed(rFinal, 16500);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*TestStatisticDelete");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex("ActivityLifecycleCallbackAdapter.onActivityResumed.*  TrackStatisticActivity");
        addRegex("TrackStatisticActivity.reworkState.*_MarkerTrack,.*_MarkerRoute");
        addRegex("TrackStatisticActivity.* confirm delete for list .*_MarkerTrack,.*_MarkerRoute");
        addRegex("TestStatisticDelete.* MetaTrackLogs.size=2");
    }
}
