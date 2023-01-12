package mg.mgmap.test;

import android.graphics.Point;

public class ScreenSwiper implements Runnable{

    Point from;
    Point to;
    int duration;

    public ScreenSwiper(Point from, Point to, int duration){
        this.from = from;
        this.to = to;
        this.duration = duration;
    }

    @Override
    public void run() {
        try {
            Runtime.getRuntime().exec("/system/bin/input swipe "+from.x+" "+from.y+" "+to.x+" "+to.y+" "+duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
