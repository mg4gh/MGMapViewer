package mg.mgmap.test.tc.extra;

import android.graphics.PointF;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.model.IMapViewPosition;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.test.TestControl;
import mg.mgmap.test.Testcase;

public class TestMarker1 extends Testcase {

    public TestMarker1(TestControl tc) {
        super(tc);
    }

    @Override
    protected void setup() {

        tc.timer.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getTestView().getContext() instanceof MGMapActivity){
                    MGMapActivity activity = (MGMapActivity)getTestView().getContext();
                    IMapViewPosition im = activity.getMapsforgeMapView().getModel().mapViewPosition;
                    im.setZoomLevel((byte)15);
                    im.setCenter(new LatLong(49.432901, 8.779158));
                }
            }
        },200);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(35,97),600), 900);
        tc.timer.postDelayed(() -> tc.doClick(this), 2000);
        tc.timer.postDelayed(() -> tc.doClick(this), 3000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(42,47),600), 4000);
        tc.timer.postDelayed(() -> tc.doClick(this), 5500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(27,30),600), 6500);
        tc.timer.postDelayed(() -> tc.doClick(this), 7500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(26,19),600), 8500);
        tc.timer.postDelayed(() -> tc.doClick(this), 9500);
        tc.timer.postDelayed(() -> tc.swipeTo(this, new PointF(26,19),2000), 11000);
        tc.timer.postDelayed(() -> tc.getPref(R.string.FSATL_pref_hideAll, false).toggle(), 14000);
        tc.timer.postDelayed(rFinal, 14500);
    }

    @Override
    protected void addRegexs() {
        regexs.add(".*Testcase.start.*  TestMapUtil");
    }
}
