package mg.mgmap.test.tc;

import android.graphics.Point;
import android.util.Log;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.test.AbstractTestCase;

public class T1 extends AbstractTestCase {

    public T1(MGMapApplication mgMapApplication) {
        super(mgMapApplication);
    }

    @Override
    public void start() {
        super.start();
        durationLimit = 30000;
    }

    public void run(){
        try {
            testControl.setCurrentCursorPos(new Point(1000,1000));
            testControl.setCursorVisibility(true);
            Thread.sleep(3000);

            MGMapActivity mgMapActivity = testControl.getActivity(MGMapActivity.class);
            if (mgMapActivity == null) return; // runs in background - do nothing

            mgMapActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MapPosition mp = new MapPosition(new LatLong(54.315814,13.351981), (byte) 15);
                    mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
                }
            });
            Thread.sleep(3000);
            PointModel pm1 = new PointModelImpl(54.315814,13.351981);
            Point p1 = getPoint4PointModel(pm1);
            PointModel pm2 = getPointModel4Point(p1);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" YYYYY "+ pm1+" "+p1 + pm2);

            Point p10 = new Point(1000,1000);
            PointModel pm10 = getPointModel4Point(p10);
            Point p11 = new Point(720,1480);
            setMapViewPosition(getMapView(), new MapPosition(new LatLong(pm10.getLat(), pm10.getLon()), (byte) 15));
            setCursorPosition(p11);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" YYYYY "+ pm1+" "+p1 + pm2);

            Thread.sleep(5000);

            Point clickPosGroupMulti = testControl.getViewClickPos("group_multi");
            Log.d(MGMapApplication.LABEL, NameUtil.context()+clickPosGroupMulti);
            testControl.setCurrentCursorPos(clickPosGroupMulti);
            doClick();

            Thread.sleep(1000);
            Point clickPosZoomOut = testControl.getViewClickPos("zoom_out");
            Log.d(MGMapApplication.LABEL, NameUtil.context()+clickPosZoomOut);
            if (clickPosZoomOut != null){
                animateTo(clickPosZoomOut, 1000);
                Thread.sleep(1000);
                doClick();
            }
            Thread.sleep(2000);
            animateTo(new Point(1000,2000), 1000);
            Thread.sleep(3000);
            swipeTo(new Point(1000,1000), 2000);

            stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
