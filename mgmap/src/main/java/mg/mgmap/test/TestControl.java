package mg.mgmap.test;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.Application;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WaitUtil;
import mg.mgmap.generic.util.basic.MGLog;

@SuppressWarnings("unused")
public class TestControl implements Application.ActivityLifecycleCallbacks{

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public final Handler timer = new Handler(); // timer for tests

    private final MGMapApplication mgMapApplication;
    boolean testMode = false;

    public SortedMap<String, Activity> activityMap = new TreeMap<>();
    public Set<String> activityCreatedToDestroyed = new TreeSet<>();
    public Set<String> activityStartedToStopped = new TreeSet<>();
    public Set<String> activityResumedToPaused = new TreeSet<>();

    public TestControl(MGMapApplication mgMapApplication){
        this.mgMapApplication = mgMapApplication;
    }

    public boolean isTestMode() {
        return testMode;
    }
    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }


    @SuppressWarnings("unchecked")
    public void runTests(SortedMap<String, String> testCases, Properties pTestResults){
        for (Map.Entry<String, String> tcEntry : testCases.entrySet()){
            try {
                Class<? extends  AbstractTestCase> testCaseClazz = (Class<? extends AbstractTestCase>) Class.forName(tcEntry.getValue());
                Constructor<? extends  AbstractTestCase> constructor = testCaseClazz.getConstructor(MGMapApplication.class);
                AbstractTestCase testCase = constructor.newInstance(mgMapApplication);

                testCase.start();
                new Thread(() -> {
                    try {
                        testCase.run();
                    } catch (Exception e) {
                        mgLog.e(e);
                    } finally {
                        testCase.stop();
                    }
                }).start();

                long limit = System.currentTimeMillis() + testCase.getDurationLimit();
                while (System.currentTimeMillis() < limit){
                    WaitUtil.doWait(this.getClass(), 1000);
                    if (! testCase.isRunning()) break; // leave loop if testcase is finished
                }
                testCase.stop();
                String result = testCase.getResult();
                pTestResults.put(testCase.getName(), result);
                mgLog.d(" finished "+testCase.getName()+" - result: "+result );
                WaitUtil.doWait(this.getClass(), 1000);
            } catch (Exception e) {
                mgLog.e(e);
            }
        }

    }



    @Override
    public void onActivityStopped(Activity activity) {
        activityMap.put(activity.getClass().getSimpleName(), activity);
        activityStartedToStopped.remove(activity.getClass().getSimpleName());
        mgLog.v(activity.getClass().getSimpleName()+ " "+activityStartedToStopped);
    }
    @Override
    public void onActivityStarted(Activity activity) {
        activityMap.put(activity.getClass().getSimpleName(), activity);
        activityStartedToStopped.add(activity.getClass().getSimpleName());
        mgLog.v(activity.getClass().getSimpleName()+ " "+activityStartedToStopped);
    }
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        mgLog.v(activity.getClass().getSimpleName());
    }
    @Override
    public void onActivityResumed(Activity activity) {
        activityMap.put(activity.getClass().getSimpleName(), activity);
        activityResumedToPaused.add(activity.getClass().getSimpleName());
        mgLog.v(activity.getClass().getSimpleName()+ " "+activityResumedToPaused);
    }
    @Override
    public void onActivityPaused(Activity activity) {
        activityMap.put(activity.getClass().getSimpleName(), activity);
        activityResumedToPaused.remove(activity.getClass().getSimpleName());
        mgLog.v(activity.getClass().getSimpleName()+ " "+activityResumedToPaused);
    }
    @Override
    public void onActivityDestroyed(Activity activity) {
        activityMap.put(activity.getClass().getSimpleName(), activity);
        activityCreatedToDestroyed.remove(activity.getClass().getSimpleName());
        mgLog.v(activity.getClass().getSimpleName()+ " "+activityCreatedToDestroyed);
    }
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        activityMap.put(activity.getClass().getSimpleName(), activity);
        activityCreatedToDestroyed.add(activity.getClass().getSimpleName());
        mgLog.v(activity.getClass().getSimpleName()+ " "+activityCreatedToDestroyed);
    }

    @SuppressWarnings("unchecked")
    public <T> T getActivity(Class<T> clazz){
        for (Map.Entry<String, Activity> entry : activityMap.entrySet()){
            if (clazz.isInstance(entry.getValue())){
                return (T) entry.getValue();
            }
        }
        return null;
    }


    public boolean isCreatedToDestroyed(Activity activity){
        return activityCreatedToDestroyed.contains(activity.getClass().getSimpleName());
    }
    public boolean isStartedToStoped(Activity activity){
        return activityStartedToStopped.contains(activity.getClass().getSimpleName());
    }
    public boolean isResumedToPaused(Activity activity){
        return activityResumedToPaused.contains(activity.getClass().getSimpleName());
    }

    public TestView currentTestView = null;
    public void registerTestView(TestView testView){
        if (isTestMode()){
            if (this.currentTestView != testView){
                mgLog.v(testView.getActivity());
                this.currentTestView = testView;
                setCursorVisibility(currentCursorVisibility);
                currentTestView.setCursorPosition(currentCursorPos);
            }
        }
    }
    public void unregisterTestView(TestView testView){
        if (isTestMode()){
            if (this.currentTestView == testView){
                mgLog.v(testView.getActivity());
                this.currentTestView = null;
            }
        }
    }


    private final SortedMap<String, Rect> viewPositionRegistry = new TreeMap<>();

    public void registerViewPosition(String key, int left, int top, int right, int bottom){
        if ((key != null) && (key.length() > 0)){
            viewPositionRegistry.put(key, new Rect(left,top,right,bottom));
        }
    }
    public Rect getViewPosition(String key){
        return viewPositionRegistry.get(key);
    }
    public Point getViewClickPos(String key){
        Rect rect = viewPositionRegistry.get(key);
        if (rect != null){
            return new Point((rect.left+rect.right)/2, (rect.top+rect.bottom)/2 );
        }
        return null;
    }


    protected Point currentCursorPos = new Point(0,0);
    public Point getCurrentCursorPos() {
        return currentCursorPos;
    }
    public void setCurrentCursorPos(Point currentCursorPos) {
        this.currentCursorPos = currentCursorPos;
        if (currentTestView != null){
            currentTestView.setCursorPosition(currentCursorPos);
        }
    }
    protected Point getCenterPos(){
        DisplayMetrics dm = mgMapApplication.getResources().getDisplayMetrics();
        return new Point(dm.widthPixels/2, dm.heightPixels/2);
    }

    protected boolean currentCursorVisibility;
    public void setCursorVisibility(boolean cursorVisibility){
        this.currentCursorVisibility = cursorVisibility;
        if (currentTestView != null){
            currentTestView.setVisibility(cursorVisibility? View.VISIBLE:View.INVISIBLE, currentTestView.cursor);
        }
    }
    public void setClickVisibility(boolean clickVisibility){
        if (currentTestView != null){
            currentTestView.setVisibility(clickVisibility? View.VISIBLE:View.INVISIBLE, currentTestView.click);
        }
    }





    // execute a test click
    public void doClick(){
        final TestView tv = currentTestView;
        if (tv != null){
            tv.setClickPosition(currentCursorPos);
            setClickVisibility(true);
            timer.postDelayed(new ScreenClicker(currentCursorPos), 200);
            // do the click after 200ms + some time to execute this command
            tv.getActivity().runOnUiThread(() -> {
                ObjectAnimator animation = ObjectAnimator.ofFloat(tv.click, "scaleX", 2);
                animation.setDuration(500);                             // ... run the click animation
                animation.start();                                      // but start this immediately
                ObjectAnimator animation2 = ObjectAnimator.ofFloat(tv.click, "scaleY", 2);
                animation2.setDuration(500);                            // same for Y direction
                animation2.start();
            });
            timer.postDelayed(() -> {                               // finally after 600ms the click animation steps
                setClickVisibility(false);                          // ImageIcon will disappear
                tv.click.setScaleX(1);                              // and the scale will be reset to normal size (1)
                tv.click.setScaleY(1);
            },600);
            WaitUtil.doWait(TestControl.class, 800);
        }
    }

    public void animateTo(Point newPosition, int duration){
        TestView tv = currentTestView;
        if (tv != null){
            tv.getActivity().runOnUiThread(() -> {
                {
                    ObjectAnimator animation = ObjectAnimator.ofFloat(tv.cursor, "translationX", newPosition.x);
                    animation.setDuration(duration);
                    animation.start();
                    ObjectAnimator animation2 = ObjectAnimator.ofFloat(tv.cursor, "translationY", newPosition.y);
                    animation2.setDuration(duration);
                    animation2.start();
                }
                {
                    ObjectAnimator animation = ObjectAnimator.ofFloat(tv.click, "translationX", newPosition.x);
                    animation.setDuration(duration);
                    animation.start();
                    ObjectAnimator animation2 = ObjectAnimator.ofFloat(tv.click, "translationY", newPosition.y);
                    animation2.setDuration(duration);
                    animation2.start();
                }

            });
            WaitUtil.doWait(TestControl.class, duration+200);
            currentCursorPos = newPosition;  // take the new position
        }
    }

    public void swipeTo(Point newPosition, int duration) {
        TestView tv = currentTestView;
        if (tv != null){
            tv.click.setScaleX(1.5f);                              // scale will bebe enlarged (1.5)
            tv.click.setScaleY(1.5f);
            setClickVisibility(true);
            timer.postDelayed(new ScreenSwiper(currentCursorPos, newPosition, duration), 1);
            timer.postDelayed(() -> animateTo(newPosition, duration), 100);
            timer.postDelayed(() -> {
                setClickVisibility(false);                          // ImageIcon will disappear
                tv.click.setScaleX(1);                              // and the scale will be reset to normal size (1)
                tv.click.setScaleY(1);
            },duration + 200);
            WaitUtil.doWait(TestControl.class, duration+400);
        }
    }


    public static DialogFragment hasOpenedDialogs(FragmentActivity activity) {
        List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
        DialogFragment res = null;
        for (Fragment fragment : fragments) {
            if (fragment instanceof DialogFragment) {
                res =  (DialogFragment)fragment;
            }
        }
        return res;
    }

    public void handleDialog(FragmentActivity activity, String prefKey, String valueToSet){
        DialogFragment dialogFragment = hasOpenedDialogs(activity);
        if (dialogFragment != null){
            dialogFragment.dismiss();
            if (prefKey != null){
                mgMapApplication.getSharedPreferences().edit().putString(prefKey, valueToSet).apply();
            }
        }
    }

}
