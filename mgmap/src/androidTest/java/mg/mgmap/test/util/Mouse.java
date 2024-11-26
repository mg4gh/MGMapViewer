package mg.mgmap.test.util;

import android.app.Instrumentation;
import android.graphics.Point;
import android.os.SystemClock;
import android.view.MotionEvent;

import androidx.test.platform.app.InstrumentationRegistry;

import mg.mgmap.generic.util.basic.MGLog;

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
        MGLog.si("clicked "+pos);
    }

    public static void swipe(float startX, float startY, float endX, float endY,  long delay) {
        swipe(startX, startY, endX, endY, delay, 2000, null);
    }

    public static void swipe(float startX, float startY, float endX, float endY,  long delay, long finalDelay, SwipeCB cb) {
        long downTime = SystemClock.uptimeMillis();
        Instrumentation inst = InstrumentationRegistry.getInstrumentation();

        MotionEvent event = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, startX, startY, 0);
        inst.sendPointerSync(event);

        // if mouse motion is too slow, android doesn't do it -> so add some fast motion at the begin
        // But if start==end, then this should simulate a long click - so don't add some fast motion at the begin
        if ((startX != endX) || (startY != endY)){
            event = MotionEvent.obtain(downTime, downTime+(1), MotionEvent.ACTION_MOVE, startX -100, startY -100, 0);
            inst.sendPointerSync(event);
            event = MotionEvent.obtain(downTime, downTime+(2), MotionEvent.ACTION_MOVE, startX , startY , 0);
            inst.sendPointerSync(event);
        }
        while (downTime + delay > SystemClock.uptimeMillis()){
            long stepStartTime = SystemClock.uptimeMillis();
            float progress = (float)(stepStartTime - downTime) / delay; // should be 0..1
            progress = Math.max(0, Math.min(1, progress)); // make sure progress in 0..1
            float x = startX + ((endX - startX) * progress);
            float y = startY + ((endY - startY) * progress);
            event = MotionEvent.obtain(downTime, stepStartTime, MotionEvent.ACTION_MOVE, x, y, 0);
            // try to make the last step big enough - this should ensure that the last MouseMotion ends really at the target
            if ( (endX - x)*(endX - x) + (endY - y)*(endY - y) > 500){ // ... so don't send event, if too close to the target
                inst.sendPointerSync(event);
            }
            if (cb != null) cb.swipePos((int) event.getX(), (int) event.getY());
        }
        event = MotionEvent.obtain(downTime, downTime+delay, MotionEvent.ACTION_MOVE, endX, endY, 0);
        inst.sendPointerSync(event);
        if (cb != null) cb.swipePos((int)event.getX(), (int)event.getY());
        SystemClock.sleep(1000); // rather wait long to prevent fling effect
        event = MotionEvent.obtain(downTime, downTime+delay+1000, MotionEvent.ACTION_UP, endX, endY, 0);
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
