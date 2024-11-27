package mg.mgmap.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.settings.SettingsActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.application.MGMapApplicationHelper;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.test.util.ActivitySupervision;
import mg.mgmap.test.util.LogMatcher;
import mg.mgmap.R;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.TestView;
import mg.mgmap.test.util.Mouse;
import mg.mgmap.test.util.PreferenceUtil;
import mg.mgmap.test.util.TrackStatisticMatcher;
import mg.mgmap.test.util.ViewCallbackAction;

@SuppressWarnings({"UnusedReturnValue", "unused", "SameParameterValue"})
public class BaseTestCase {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    protected static Handler timer = new Handler(Looper.getMainLooper());

    protected static int TIMEOUT_ANIMATION = 800;

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
        SystemClock.sleep(1000);
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
        activitySupervision.clear();

        MGMapApplicationHelper.cleanup(mgMapApplication);
        mgMapApplication = null;
        androidTestAssets = null;
        activitySupervision = null;
        currentActivity = null;
        currentPos = null;
    }

    protected void initPos(MGMapActivity mgMapActivity, PointModel pm, byte zoom){
        mgMapActivity.getMapViewUtility().setCenter(pm);
        mgMapActivity.getMapsforgeMapView().getModel().mapViewPosition.setZoomLevel(zoom);
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
        SystemClock.sleep(1000);
        currentActivity = activity;
        setCursorPos(currentPos);
        return activity;
    }

    protected Point animateToPosAndClick(double latitude, double longitude){
        Point pos = animateTo(getPoint4PointModel(new PointModelImpl(latitude,longitude)),TIMEOUT_ANIMATION);
        animateClick(pos);
        return pos;
    }

    protected void animateSwipeLatLong(double latitudeStart, double longitudeStart, double latitudeEnd, double longitudeEnd){
        animateSwipeLatLong(new PointModelImpl(latitudeStart,longitudeStart),new PointModelImpl(latitudeEnd,longitudeEnd));
    }
    protected void animateSwipeLatLong(PointModel pmStart, PointModel pmEnd){
        // wait if a fing action is in progress
        PointModel pmCenterOld;
        PointModel pmCenter = activitySupervision.getActivity(MGMapActivity.class).getMapViewUtility().getCenter();
        do {
            SystemClock.sleep(50);
            pmCenterOld = pmCenter;
            pmCenter = activitySupervision.getActivity(MGMapActivity.class).getMapViewUtility().getCenter();
        } while (!pmCenterOld.equals(pmCenter));

        Point start = getPoint4PointModel(pmStart);
        Point end = getPoint4PointModel(pmEnd);
        animateSwipeToPos(start, end);
    }

    protected Point animateSwipeToPos(Point start, Point end){
        animateTo(start);
        SystemClock.sleep(300);
        Point p = new Point();
        setClickVisibility(true);
        Mouse.swipe(start.x, start.y, end.x, end.y, 2*TIMEOUT_ANIMATION, 0, (x,y)->{p.x=x;p.y=y;setCursorPos(p);setClickPos(p);});
        setClickVisibility(false);
        return end;
    }


    protected Point animateToViewAndClick(int viewId){
        mgLog.i("to "+currentActivity.getResources().getResourceName(viewId));
        Point pos = animateTo(getClickPos(viewId),TIMEOUT_ANIMATION);
        animateClick(pos);
        return pos;
    }
    protected Point animateToViewAndClick(View view){
        Point pos = animateTo(getClickPos(view),TIMEOUT_ANIMATION);
        animateClick(pos);
        return pos;
    }

    protected Point animateToPrefAndClick(int keyId){
        Point pos = PreferenceUtil.getPreferenceCenter(waitForActivity(SettingsActivity.class),keyId);
        assert (pos != null);
        animateTo(pos, TIMEOUT_ANIMATION);
        animateClick(pos);
        return pos;
    }

    protected Point animateToStatAndClick(String nameMatch){
        AtomicReference<View> viewRef = new AtomicReference<>();
        onView(withId(R.id.trackStatisticEntries))
                .perform(RecyclerViewActions.actionOnItem(TrackStatisticMatcher.matchTrack(nameMatch), new ViewCallbackAction(viewRef::set)));
        return animateToViewAndClick(viewRef.get());
    }

    protected Point getClickPos(int viewId){
        View v = waitForView(View.class, viewId);
        SystemClock.sleep(50);
        return getClickPos(v);
    }
    protected Point getClickPos(View v){
        int[] loc = new int[2];
        v.getLocationOnScreen(loc);
        return new Point(loc[0]+v.getWidth()/2,loc[1]+v.getHeight()/2);
    }

    protected Point getCenterPos(){
        return getClickPos(mg.mgmap.R.id.testview);
    }

    protected Point setCursorPos(Point pos ){
        mgLog.d(pos+" "+ getCenterPos());
        TestView testView = waitForView(TestView.class, mg.mgmap.R.id.testview);
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            testView.setCursorPosition(pos);
        } else {
            currentActivity.runOnUiThread(()->testView.setCursorPosition(pos));
        }
        currentPos = pos;
        return pos;
    }

    protected Point setClickPos(Point pos){
        TestView testView = waitForView(TestView.class, mg.mgmap.R.id.testview);
        mgLog.i("pos "+pos);
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            testView.setClickPosition(pos);
        } else {
            currentActivity.runOnUiThread(()->testView.setClickPosition(pos));
        }
        return pos;
    }

    protected Point setCursorToCenterPos(){
        return setCursorPos(getCenterPos());
    }

    protected Point animateTo(Point pos){
        return animateTo(pos, TIMEOUT_ANIMATION);
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

    protected Point animateClick(Point pos, Runnable r) {
        timer.postDelayed(()->new Thread(r).start(), 300);
        return animateClick(pos);
    }

        /**
         * @param pos position on screen
         */
    protected Point animateClick(Point pos){
        TestView testView = waitForView(TestView.class, mg.mgmap.R.id.testview);
        mgLog.i("pos "+pos);
        timer.postDelayed(()->new Thread(()->Mouse.click(pos)).start(), 200);
        testView.getActivity().runOnUiThread(() -> {
            testView.setClickPosition(pos);
            setClickVisibility(true);
            ObjectAnimator animation = ObjectAnimator.ofFloat(testView.getClick(), "scaleX", 2);
            animation.setDuration(500);                             // ... run the click animation
            animation.start();                                      // but start this immediately
            ObjectAnimator animation2 = ObjectAnimator.ofFloat(testView.getClick(), "scaleY", 2);
            animation2.setDuration(500);                            // same for Y direction
            animation2.start();
        });
        timer.postDelayed(() -> {                               // finally after 600ms the click animation steps
            testView.getActivity().runOnUiThread(() -> {
                setClickVisibility(false);                          // ImageIcon will disappear
                testView.getClick().setScaleX(1);                              // and the scale will be reset to normal size (1)
                testView.getClick().setScaleY(1);
            });
        },600);
        SystemClock.sleep(TIMEOUT_ANIMATION);
        mgLog.i("pos "+pos);
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

    public Point getPoint4PointModel(PointModel pm) {
        return activitySupervision.getActivity(MGMapActivity.class).getMapViewUtility().getPoint4PointModel(pm);
    }
    public PointModel getPointModel4Point(Point p) {
        return activitySupervision.getActivity(MGMapActivity.class).getMapViewUtility().getPointModel4Point(p);
    }

}