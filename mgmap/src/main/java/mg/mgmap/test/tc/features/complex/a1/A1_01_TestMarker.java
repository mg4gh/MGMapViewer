package mg.mgmap.test.tc.features.complex.a1;

import android.graphics.PointF;
import android.util.Log;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.model.IMapViewPosition;

import java.util.Locale;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.test.TestControl;
import mg.mgmap.test.Testcase;

public class A1_01_TestMarker extends Testcase {

    public A1_01_TestMarker(TestControl tc) {
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
//        tc.timer.postDelayed(() -> tc.swipeTo(this, new PointF(26,19),2000), 11000);
        Formatter f = new Formatter(Formatter.FormatType.FORMAT_DISTANCE);
        tc.timer.postDelayed(() -> Log.i(MGMapApplication.LABEL, NameUtil.context()
                +" d="+f.format(tc.application.routeTrackLogObservable.getTrackLog().getTrackStatistic().getTotalLength())), 11500);

        tc.timer.postDelayed(() -> tc.getPref(R.string.FSATL_pref_hideAll, false).toggle(), 12000);
        tc.timer.postDelayed(rFinal, 12500);
    }

    @Override
    protected void addRegexs() {
        regexs.add(".*Testcase.start.*TestMarker");
        regexs.add(".*TestMarker.*d= 0.95 km");
    }
}
