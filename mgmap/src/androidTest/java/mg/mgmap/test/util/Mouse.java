package mg.mgmap.test.util;

import android.app.Instrumentation;
import android.graphics.Point;
import android.os.SystemClock;
import android.view.MotionEvent;

import androidx.test.platform.app.InstrumentationRegistry;

public class Mouse {

    public interface SwipeCB{
        void swipePos(int x, int y);
    }

    public static void click(Point pos){
        long now = SystemClock.uptimeMillis();
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();

        MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, pos.x, pos.y, 0);
        inst.sendPointerSync(event);
        event = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, pos.x, pos.y, 0);
        inst.sendPointerSync(event);
    }

    public static void swipe(float startX, float startY, float endX, float endY,  long delay) {
        swipe(startX, startY, endX, endY, delay, 2000, null);
    }

    public static void swipe(float startX, float startY, float endX, float endY,  long delay, long finalDelay, SwipeCB cb) {
        int steps = (int)(delay/20);
        long downTime = SystemClock.uptimeMillis();
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();

        MotionEvent event = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0);
        inst.sendPointerSync(event);

        for (int i=0; i<steps; i++){
            event = MotionEvent.obtain(downTime, downTime+(i*delay/steps), MotionEvent.ACTION_MOVE, startX + ((endX - startX)*i)/steps, startY + ((endY - startY)*i)/steps, 0);
            inst.sendPointerSync(event);
            if (cb != null) cb.swipePos((int)event.getX(), (int)event.getY());
        }
        event = MotionEvent.obtain(downTime, downTime+delay, MotionEvent.ACTION_UP, endX, endY, 0);
        inst.sendPointerSync(event);
        SystemClock.sleep(finalDelay); //The wait is important to scroll
    }

    public static void swipeUp(){
        swipe(500,1000,500,300, 40);
    }
    public static void swipeDown(){
        swipe(500,300,500,1000, 40);
    }
    public static void swipeRight(){
        swipe(100,500,600,500, 40);
    }
    public static void swipeLeft(){
        swipe(600,500,100,500, 40);
    }

    public static void swipeUpSlow(){
        swipe(500,1000,500,300, 1000);
    }
    public static void swipeDownSlow(){
        swipe(500,300,500,1000, 1000);
    }
    public static void swipeRightSlow(){
        swipe(100,500,600,500, 1000);
    }
    public static void swipeLeftSlow(){
        swipe(600,500,100,500, 1000);
    }


}
