package mg.mgmap.test;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroupAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import mg.mgmap.activity.settings.SettingsActivity;
import mg.mgmap.application.BaseConfig;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.application.Setup;
import mg.mgmap.generic.util.WaitUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.generic.view.TestView;

@SuppressWarnings("unused")
public class TestControl implements Setup.TestRunner,Application.ActivityLifecycleCallbacks{

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public final Handler timer = new Handler(); // timer for tests

    private final MGMapApplication mgMapApplication;

    public SortedMap<String, Activity> activityMap = new TreeMap<>();
    public Set<String> activityCreatedToDestroyed = new TreeSet<>();
    public Set<String> activityStartedToStopped = new TreeSet<>();
    public Set<String> activityResumedToPaused = new TreeSet<>();
    Point screenDimension = new Point();

    public TestControl(MGMapApplication mgMapApplication, Setup setup){
        this.mgMapApplication = mgMapApplication;

        setup.setTestRunner(this); // dependency injection
        mgMapApplication.registerActivityLifecycleCallbacks(this);
        ExtendedTextView.setViewPositionHook((key, left, top, right, bottom) -> {
            if ((key != null) && (key.length() > 0)){
                Rect rect = new Rect(left,top,right,bottom);
                mgLog.d("register key: "+key +" rect="+rect);
                viewPositionRegistry.put(key, rect);
            }
        });
        TestView.setTestViewHook(new TestView.TestViewHook() {
            @Override
            public void registerTestView(TestView testView){
                if (mgMapApplication.baseConfig.getMode() == BaseConfig.Mode.SYSTEM_TEST){
                    if (TestControl.this.currentTestView != testView){
                        mgLog.v(testView.getActivity());
                        TestControl.this.currentTestView = testView;
                        setCursorVisibility(currentCursorVisibility);
                        currentTestView.setCursorPosition(currentCursorPos);
                    }
                }
            }
            @Override
            public void unregisterTestView(TestView testView){
                if (mgMapApplication.baseConfig.getMode() == BaseConfig.Mode.SYSTEM_TEST){
                    if (TestControl.this.currentTestView == testView){
                        mgLog.v(testView.getActivity());
                        TestControl.this.currentTestView = null;
                    }
                }
            }
            @Override
            public void onTestViewLayout(TestView testView){
                if (testView.getHeight() > screenDimension.y){
                    screenDimension = new Point(testView.getWidth(), testView.getHeight());
                }
            }
        });
    }

    protected Point currentCursorPos = new Point(0,0); // on screen (not in window)

    @SuppressWarnings("unchecked")
    public void runTests(SortedMap<String, String> testCases, Properties pTestResults){
        for (Map.Entry<String, String> tcEntry : testCases.entrySet()){
            try {
                Class<? extends  AbstractTestCase> testCaseClazz = (Class<? extends AbstractTestCase>) Class.forName(tcEntry.getValue());
                Constructor<? extends  AbstractTestCase> constructor = testCaseClazz.getConstructor(MGMapApplication.class, TestControl.class);
                AbstractTestCase testCase = constructor.newInstance(mgMapApplication, this);

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

                long now = System.currentTimeMillis() + testCase.getDurationLimit();
                while (System.currentTimeMillis() < now + testCase.getDurationLimit()){
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

    private final SortedMap<String, Rect> viewPositionRegistry = new TreeMap<>();

    public Rect getViewPosition(String key){
        return viewPositionRegistry.get(key);
    }
    public Point getViewClickPos(String key){
        Rect rect = viewPositionRegistry.get(key);
        if (rect != null){
            int[] loc = new int[2];
            currentTestView.getLocationOnScreen(loc);
            return new Point((rect.left+rect.right)/2 - loc[0], (rect.top+rect.bottom)/2 - loc[1]);
        }
        return null;
    }


    public Point getCurrentCursorPos() {
        return currentCursorPos;
    }
    public void setCurrentCursorPos(Point currentCursorPos) {
        this.currentCursorPos = currentCursorPos;
        if (currentTestView != null){
            currentTestView.setCursorPosition(currentCursorPos);
        }
    }

    protected boolean currentCursorVisibility;
    public void setCursorVisibility(boolean cursorVisibility){
        this.currentCursorVisibility = cursorVisibility;
        if (currentTestView != null){
            currentTestView.setVisibility(cursorVisibility? View.VISIBLE:View.INVISIBLE, currentTestView.getCursor());
        }
    }
    public void setClickVisibility(boolean clickVisibility){
        if (currentTestView != null){
            currentTestView.setVisibility(clickVisibility? View.VISIBLE:View.INVISIBLE, currentTestView.getClick());
        }
    }

    // execute a test click
    public void doClick(){
        final TestView tv = currentTestView;
        if (tv != null){
            int[] tvLoc = new int[2];
            tv.getLocationOnScreen(tvLoc);
            Point screenPos = new Point(currentCursorPos.x + tvLoc[0], currentCursorPos.y + tvLoc[1]);
            tv.setClickPosition(screenPos);
            setClickVisibility(true);
            timer.postDelayed(new ScreenClicker(screenPos), 200);
            // do the click after 200ms + some time to execute this command
            tv.getActivity().runOnUiThread(() -> {
                ObjectAnimator animation = ObjectAnimator.ofFloat(tv.getClick(), "scaleX", 2);
                animation.setDuration(500);                             // ... run the click animation
                animation.start();                                      // but start this immediately
                ObjectAnimator animation2 = ObjectAnimator.ofFloat(tv.getClick(), "scaleY", 2);
                animation2.setDuration(500);                            // same for Y direction
                animation2.start();
            });
            timer.postDelayed(() -> {                               // finally after 600ms the click animation steps
                setClickVisibility(false);                          // ImageIcon will disappear
                tv.getClick().setScaleX(1);                              // and the scale will be reset to normal size (1)
                tv.getClick().setScaleY(1);
            },600);
            WaitUtil.doWait(TestControl.class, 800);
        }
    }

    public void animateTo(Point newPosition, int duration){
        TestView tv = currentTestView;
        if (tv != null){
            tv.getActivity().runOnUiThread(() -> {
                {
                    ObjectAnimator animation = ObjectAnimator.ofFloat(tv.getCursor(), "translationX", newPosition.x);
                    animation.setDuration(duration);
                    animation.start();
                    ObjectAnimator animation2 = ObjectAnimator.ofFloat(tv.getCursor(), "translationY", newPosition.y);
                    animation2.setDuration(duration);
                    animation2.start();
                }
                {
                    ObjectAnimator animation = ObjectAnimator.ofFloat(tv.getClick(), "translationX", newPosition.x);
                    animation.setDuration(duration);
                    animation.start();
                    ObjectAnimator animation2 = ObjectAnimator.ofFloat(tv.getClick(), "translationY", newPosition.y);
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
            tv.getClick().setScaleX(1.5f);                              // scale will bebe enlarged (1.5)
            tv.getClick().setScaleY(1.5f);
            setClickVisibility(true);
            timer.postDelayed(new ScreenSwiper(currentCursorPos, newPosition, duration), 1);
            timer.postDelayed(() -> animateTo(newPosition, duration), 100);
            timer.postDelayed(() -> {
                setClickVisibility(false);                          // ImageIcon will disappear
                tv.getClick().setScaleX(1);                              // and the scale will be reset to normal size (1)
                tv.getClick().setScaleY(1);
            },duration + 200);
            WaitUtil.doWait(TestControl.class, duration+400);
        }
    }


    public static void dismissPreferenceDialogFragmentCompat(FragmentActivity activity) {
        List<Fragment> fragments = activity.getSupportFragmentManager().getFragments();
        PreferenceDialogFragmentCompat res = null;
        for (Fragment fragment : fragments) {
            if (fragment instanceof PreferenceDialogFragmentCompat) {
                ((PreferenceDialogFragmentCompat)fragment).dismiss();
            }
        }
    }

    public void handleSettingsDialog(int keyId, String valueToSet){
        AppCompatActivity settingsActivity = getActivity(SettingsActivity.class);
        dismissPreferenceDialogFragmentCompat(settingsActivity);

        if (keyId != 0){
            String key = settingsActivity.getResources().getString(keyId);
            Fragment f = settingsActivity.getSupportFragmentManager().getFragments().get(0);
            if (f instanceof PreferenceFragmentCompat) {
                PreferenceFragmentCompat pfc = (PreferenceFragmentCompat) f; // corresponds to preference screen
                Preference preference = pfc.findPreference(key);
                assert preference != null;
                if (preference instanceof ListPreference) {
                    ListPreference listPreference = (ListPreference) preference;
                    settingsActivity.runOnUiThread(()->listPreference.setValue(valueToSet));
                } else if (preference instanceof EditTextPreference) {
                    EditTextPreference listPreference = (EditTextPreference) preference;
                    settingsActivity.runOnUiThread(()->listPreference.setText(valueToSet));
                }
            }
        }
    }

    public Point getPreferenceCenter(int  keyId) {
        AppCompatActivity settingsActivity = getActivity(SettingsActivity.class);
        String key = settingsActivity.getResources().getString(keyId);

        Fragment f = settingsActivity.getSupportFragmentManager().getFragments().get(0);
        if (f instanceof PreferenceFragmentCompat) {
            PreferenceFragmentCompat pfc = (PreferenceFragmentCompat) f; // corresponds to preference screen

            RecyclerView rv = pfc.getListView();
            RecyclerView.Adapter<?> ra = rv.getAdapter();

            if (ra instanceof PreferenceGroupAdapter) {
                PreferenceGroupAdapter pga = (PreferenceGroupAdapter) ra;
                @SuppressLint("RestrictedApi") int pIdx = pga.getPreferenceAdapterPosition(key);


                RecyclerView.LayoutManager rlm = rv.getLayoutManager();
                if (rlm instanceof LinearLayoutManager) {
                    LinearLayoutManager llm = (LinearLayoutManager) rlm;

                    View v = llm.getChildAt(pIdx);
                    if (v!=null){
                        int[] loc1 = new int[2];
                        int[] loc2 = new int[2];
                        v.getLocationOnScreen(loc1);
                        currentTestView.getLocationOnScreen(loc2);
                        Point pt = new Point(loc1[0] + v.getWidth() / 2, loc1[1] + v.getHeight() / 2 - loc2[1]);
                        mgLog.d(pt);
                        return pt;
                    }
                }
            }
        }
        return null;
    }

}
