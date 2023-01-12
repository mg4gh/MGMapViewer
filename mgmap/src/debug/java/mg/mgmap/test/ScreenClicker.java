package mg.mgmap.test;

import android.graphics.Point;

public class ScreenClicker implements Runnable{

    Point p;

    public ScreenClicker(Point p){
        this.p = p;
    }

    @Override
    public void run() {
        try {
            Runtime.getRuntime().exec("/system/bin/input tap "+p.x+" "+p.y);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
