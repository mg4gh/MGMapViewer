package mg.mgmap.test.tc.features.complex.a1;

import android.graphics.PointF;
import android.util.Log;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.model.IMapViewPosition;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.test.TestControl;
import mg.mgmap.test.Testcase;

public class A1_03_TestBB extends Testcase {

    public A1_03_TestBB(TestControl tc) {
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
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,96),600), 900);
        tc.timer.postDelayed(() -> tc.doClick(this), 2000);
        tc.timer.postDelayed(() -> tc.doClick(this), 3000);
        tc.timer.postDelayed(() -> tc.animateTo(this, new PointF(50,50),600), 4000);
        tc.timer.postDelayed(() -> tc.swipeTo(this, new PointF(50,50),1000), 6500);
        tc.timer.postDelayed(() -> Log.i(MGMapApplication.LABEL, NameUtil.context()+" AvailableTrackLogs.size="+tc.application.availableTrackLogsObservable.availableTrackLogs.size()), 8000);

        tc.timer.postDelayed(rFinal, 8500);
    }

    @Override
    protected void addRegexs() {
        regexs.add(".*Testcase.start.*TestBB");
        addRegex("TestBB.* AvailableTrackLogs.size=2");
    }
}
