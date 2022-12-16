package mg.mgmap.test.oldtc;

import android.graphics.Point;
import android.util.Log;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.test.TestDataRegistry;

public class T1 {

    MGMapApplication mgMapApplication;

    public T1(MGMapApplication mgMapApplication) {
        this.mgMapApplication = mgMapApplication;
    }

    public void run(){
        try {
            TestDataRegistry testDataRegistry = mgMapApplication.getTestDataRegistry();
            testDataRegistry.setCurrentCursorPos(new Point(1000,1000));
            testDataRegistry.setCursorVisibility(true);
            Thread.sleep(3000);

            MGMapActivity mgMapActivity = testDataRegistry.getActivity(MGMapActivity.class);
            if (mgMapActivity == null) return; // runs in background - do nothing

            mgMapActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MapPosition mp = new MapPosition(new LatLong(54.315814,13.351981), (byte) 15);
                    mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setMapPosition(mp);
                }
            });

            Thread.sleep(5000);

            Point clickPosGroupMulti = testDataRegistry.getViewClickPos("group_multi");
            Log.d(MGMapApplication.LABEL, NameUtil.context()+clickPosGroupMulti);
            testDataRegistry.setCurrentCursorPos(clickPosGroupMulti);
            testDataRegistry.doClick();

            Thread.sleep(1000);
            Point clickPosZoomOut = testDataRegistry.getViewClickPos("zoom_out");
            Log.d(MGMapApplication.LABEL, NameUtil.context()+clickPosZoomOut);
            testDataRegistry.animateTo(clickPosZoomOut, 1000);
            Thread.sleep(1000);
            testDataRegistry.doClick();

            Thread.sleep(2000);
            testDataRegistry.animateTo(new Point(1000,2000), 1000);
            Thread.sleep(3000);
            testDataRegistry.swipeTo(new Point(1000,1000), 2000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
