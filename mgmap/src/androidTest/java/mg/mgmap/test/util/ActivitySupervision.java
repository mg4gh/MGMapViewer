package mg.mgmap.test.util;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import mg.mgmap.generic.util.basic.MGLog;

public class ActivitySupervision implements Application.ActivityLifecycleCallbacks{

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private Application application;
    public SortedMap<String, Activity> activityMap = new TreeMap<>();
    public Set<String> activityCreatedToDestroyed = new TreeSet<>();
    public Set<String> activityStartedToStopped = new TreeSet<>();
    public Set<String> activityResumedToPaused = new TreeSet<>();


    public ActivitySupervision(Application application){
        this.application = application;
        application.unregisterActivityLifecycleCallbacks(this);
        application.registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityStopped(Activity activity) {
        activityStartedToStopped.remove(activity.getClass().getSimpleName());
        mgLog.v(activity.getClass().getSimpleName()+ " "+activityStartedToStopped);
    }
    @Override
    public void onActivityStarted(Activity activity) {
        activityMap.put(activity.getClass().getSimpleName(), activity);
        activityStartedToStopped.add(activity.getClass().getSimpleName());
        mgLog.d(activity.getClass().getSimpleName()+ " "+activityStartedToStopped);
    }
    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        mgLog.v(activity.getClass().getSimpleName());
    }
    @Override
    public void onActivityResumed(Activity activity) {
        activityMap.put(activity.getClass().getSimpleName(), activity);
        activityResumedToPaused.add(activity.getClass().getSimpleName());
        mgLog.d(activity.getClass().getSimpleName()+ " "+activityResumedToPaused);
    }
    @Override
    public void onActivityPaused(Activity activity) {
        activityResumedToPaused.remove(activity.getClass().getSimpleName());
        mgLog.v(activity.getClass().getSimpleName()+ " "+activityResumedToPaused);
    }
    @Override
    public void onActivityDestroyed(Activity activity) {
        activityCreatedToDestroyed.remove(activity.getClass().getSimpleName());
        mgLog.v(activity.getClass().getSimpleName()+ " "+activityCreatedToDestroyed);
    }
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        activityMap.put(activity.getClass().getSimpleName(), activity);
        activityCreatedToDestroyed.add(activity.getClass().getSimpleName());
        mgLog.d(activity.getClass().getSimpleName()+ " "+activityCreatedToDestroyed);
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

    public void clear(){
        activityMap.clear();
        mgLog.i(activityMap.size());
        activityResumedToPaused.clear();
        activityStartedToStopped.clear();
        activityCreatedToDestroyed.clear();
        application.unregisterActivityLifecycleCallbacks(this);
        application = null;
    }
}
