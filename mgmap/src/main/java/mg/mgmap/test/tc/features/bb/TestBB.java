package mg.mgmap.test.tc.features.bb;

import android.graphics.PointF;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.model.IMapViewPosition;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.test.TestControl;
import mg.mgmap.test.Testcase;

public class TestBB extends Testcase {

    public TestBB(TestControl tc) {
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
        },1000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,50),1), 1000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,70),2000), 1100);
//        tc.timer.postDelayed(() -> tc.doClick(this), 4000);
        tc.timer.postDelayed(() -> tc.swipeTo(this, new PointF(50,30),3000), 3500);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,70),2000), 7100);
        tc.timer.postDelayed(() -> tc.swipeTo(this, new PointF(50,30),200), 8500);

        tc.timer.postDelayed(rFinal, 10000);
    }

    @Override
    protected void addRegexs() {
        regexs.add(".*Testcase.start.* TestBB");
    }
}
