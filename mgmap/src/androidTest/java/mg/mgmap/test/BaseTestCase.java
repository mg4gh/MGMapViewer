package mg.mgmap.test;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.activity.settings.SettingsActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.test.util.ActivitySupervision;
import mg.mgmap.test.util.LogMatcher;
import mg.mgmap.R;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.TestView;
import mg.mgmap.test.util.Mouse;
import mg.mgmap.test.util.PreferenceUtil;

@SuppressWarnings({"UnusedReturnValue", "unused", "SameParameterValue"})
public class BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    protected static Handler timer = new Handler(Looper.getMainLooper());


    boolean running = false;
    LogMatcher lm = null;
    protected MGLog.Level level = MGLog.Level.DEBUG;
    private ArrayList<String> regexs = null;
    private ArrayList<String> matches = null;

    protected MGMapApplication mgMapApplication;
    protected AssetManager androidTestAssets;
    protected ActivitySupervision activitySupervision;


    protected Activity currentActivity;
    protected Point currentPos = new Point(0,0); // current cursor position on screen

    public BaseTestCase(){
        mgLog.i(this.getClass().getName() + "." + ((name!=null)?name.getMethodName():"?") + " start");
        mgMapApplication = (MGMapApplication) InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        androidTestAssets = InstrumentationRegistry.getInstrumentation().getContext().getAssets();
        activitySupervision = new ActivitySupervision(mgMapApplication);
    }

    @Rule
    public TestName name = new TestName();

    @Before
    public void start() {
        running = true;
        lm = new LogMatcher(level);
        regexs = new ArrayList<>();
        matches = new ArrayList<>();
        lm.startMatch(regexs, matches);
        mgLog.i(this.getClass().getName() + "." + name.getMethodName() + " start");
    }

    @After
    public void after() {
        if (running) {
            int lmLines = lm.stopMatch();
            running = false;
            mgLog.i(this.getClass().getName() + "." + name.getMethodName() + " stop ("+lmLines+")");
        }
        Assert.assertTrue(lm.getResult());
    }

    protected void addRegex(String regex) {
        regexs.add(regex);
    }

    protected <T> T waitForView(Class<T> clazz, int viewId, long timeout) {
        View view;
        while (((view = currentActivity.findViewById(viewId)) == null) || (view.getVisibility() != View.VISIBLE)
                || (view.getWidth() == 0) || (view.getHeight() == 0)) {
            SystemClock.sleep(100);
        }
        if (timeout > 0) {
            SystemClock.sleep(timeout);
        }
        //noinspection unchecked
        return (T)view;
    }

    protected  <T> T waitForView(Class<T> clazz, int viewId) {
        return waitForView(clazz, viewId, 0);
    }

    protected  <T extends Activity> T waitForActivity(Class<T> clazz) {
        T activity = null;
        boolean found = false;
        while (!found){
            activity = activitySupervision.getActivity(clazz);
            if (activity != null){
                found = (activitySupervision.activityResumedToPaused.contains(clazz.getSimpleName()));
            }
            SystemClock.sleep(100);
        }
        currentActivity = activity;
        setCursorPos(currentPos);
        return activity;
    }

    protected Point animateToViewAndClick(int viewId){
        Point pos = animateTo(getClickPos(viewId),1000);
        timer.postDelayed(()->new Thread(()->Mouse.click(currentPos)).start(), 200);
//        timer.postDelayed(()-> new Thread(() -> onView(withId(viewId)).check(matches(isDisplayed())).perform(ViewActions.click())).start() , 200);
        animateClick(pos);
        return pos;
    }

    protected Point animateToPrefAndClick(int keyId){
        Point pos = PreferenceUtil.getPreferenceCenter(waitForActivity(SettingsActivity.class),keyId);
        assert (pos != null);
        animateTo(pos, 1000);
        timer.postDelayed(()->new Thread(()->Mouse.click(currentPos)).start(), 200);
        animateClick(pos);
        return pos;
    }

    protected Point getClickPos(int viewId){
        View v = waitForView(View.class, viewId);
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        return new Point(loc[0]+v.getWidth()/2,loc[1]+v.getHeight()/2);
    }

    protected Point getCenterPos(){
        return getClickPos(mg.mgmap.R.id.testview);
    }

    protected Point setCursorPos(Point pos ){
        TestView testView = waitForView(TestView.class, mg.mgmap.R.id.testview);
        testView.setCursorPosition(pos);
        currentPos = pos;
        return pos;
    }

    protected Point setCursorToCenterPos(){
        return setCursorPos(getCenterPos());
    }

    protected Point animateTo(Point pos){
        return animateTo(pos, 1000);
    }

    /**
     * @param pos position on screen
     * @param duration duration of animation
     */
    protected Point animateTo(Point pos, int duration){
        TestView testView = waitForView(TestView.class, mg.mgmap.R.id.testview);

        setCursorVisibility(true);
        int[] tvLoc = new int[2];
        testView.getLocationOnScreen(tvLoc);
        Point tvPos = new Point(pos.x - tvLoc[0], pos.y - tvLoc[1]);
        mgLog.i("to "+pos+" tvPos="+tvPos+" "+testView.getActivity().getClass());

        testView.getActivity().runOnUiThread(() -> {
            {
                ObjectAnimator animation = ObjectAnimator.ofFloat(testView.getCursor(), "translationX", pos.x-tvLoc[0]);
                animation.setDuration(duration);
                animation.start();
                ObjectAnimator animation2 = ObjectAnimator.ofFloat(testView.getCursor(), "translationY", pos.y-tvLoc[1]);
                animation2.setDuration(duration);
                animation2.start();
            }
            {
                ObjectAnimator animation = ObjectAnimator.ofFloat(testView.getClick(), "translationX", pos.x-tvLoc[0]);
                animation.setDuration(duration);
                animation.start();
                ObjectAnimator animation2 = ObjectAnimator.ofFloat(testView.getClick(), "translationY", pos.y-tvLoc[1]);
                animation2.setDuration(duration);
                animation2.start();
            }

        });
        SystemClock.sleep(duration+200);
        currentPos = pos;
        return pos;
    }

    /**
     * @param pos position on screen
     */
    protected Point animateClick(Point pos){
        TestView testView = waitForView(TestView.class, mg.mgmap.R.id.testview);
        mgLog.i("pos "+pos);
        testView.setClickPosition(pos);
        setClickVisibility(true);
        testView.getActivity().runOnUiThread(() -> {
            ObjectAnimator animation = ObjectAnimator.ofFloat(testView.getClick(), "scaleX", 2);
            animation.setDuration(500);                             // ... run the click animation
            animation.start();                                      // but start this immediately
            ObjectAnimator animation2 = ObjectAnimator.ofFloat(testView.getClick(), "scaleY", 2);
            animation2.setDuration(500);                            // same for Y direction
            animation2.start();
        });
        timer.postDelayed(() -> {                               // finally after 600ms the click animation steps
            setClickVisibility(false);                          // ImageIcon will disappear
            testView.getClick().setScaleX(1);                              // and the scale will be reset to normal size (1)
            testView.getClick().setScaleY(1);
        },600);
        SystemClock.sleep(800);
        return pos;
    }

    protected void setCursorVisibility(boolean cursorVisibility){
        TestView testView = waitForView(TestView.class, mg.mgmap.R.id.testview);
        testView.setVisibility(cursorVisibility? View.VISIBLE:View.INVISIBLE, testView.getCursor());
    }
    protected void setClickVisibility(boolean clickVisibility){
        TestView testView = waitForView(TestView.class, R.id.testview);
        testView.setVisibility(clickVisibility? View.VISIBLE:View.INVISIBLE, testView.getClick());
    }


}