package mg.mgmap.test.oldtc.features.complex.a1;

import android.graphics.PointF;
import android.util.Log;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.test.OldTestControl;
import mg.mgmap.test.OldTestcase;

public class A1_02_TestStatisticSave extends OldTestcase {

    public A1_02_TestStatisticSave(OldTestControl tc) {
        super(tc, Log.DEBUG);
    }

    @Override
    protected void setup(int delay) {
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(7,97),1000), delay+1100);
        tc.timer.postDelayed(() -> tc.doClick(this), delay+3000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(49,97),200), delay+4000);
        tc.timer.postDelayed(() -> tc.doClick(this), delay+5000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(30,5),600), delay+6500);
        tc.timer.postDelayed(() -> tc.doClick(this), delay+7500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(30,15),600), delay+8500);
        tc.timer.postDelayed(() -> tc.doClick(this), delay+9500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(72,97),600), delay+10500);
        tc.timer.postDelayed(() -> tc.doClick(this), delay+11500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(94,97),600), delay+12500);
        tc.timer.postDelayed(() -> tc.doClick(this), delay+13500);
        tc.timer.postDelayed(() -> Log.i(MGMapApplication.LABEL, NameUtil.context()+" MetaTrackLogs.size="+tc.application.metaTrackLogs.size()), delay+13500);
        tc.timer.postDelayed(rFinal, delay+14500);
    }

    @Override
    protected void addRegexs() {
        addRegex("Testcase.start.*TestStatisticSave");
        addRegex("key=FSControl.qc_selector value=1");
        addRegex("ActivityLifecycleCallbackAdapter.onActivityResumed.*  TrackStatisticActivity");
        addRegex("TrackStatisticActivity.reworkState.*_MarkerTrack,.*_MarkerRoute");
        addRegex("TrackStatisticActivity.* save .*_MarkerTrack");
        addRegex("TrackStatisticActivity.* save .*_MarkerRoute");
        addRegex("TestStatisticSave.* MetaTrackLogs.size=4");
    }
}
