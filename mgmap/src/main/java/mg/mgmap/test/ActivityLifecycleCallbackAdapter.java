package mg.mgmap.test;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;

public class ActivityLifecycleCallbackAdapter implements Application.ActivityLifecycleCallbacks{

    Activity activity;

    public ActivityLifecycleCallbackAdapter(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if ((this.activity == activity) && Log.isLoggable(MGMapApplication.LABEL, Log.VERBOSE)){
            Log.v(MGMapApplication.LABEL, NameUtil.context()+" "+activity.getClass().getSimpleName());
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if ((this.activity == activity) && Log.isLoggable(MGMapApplication.LABEL, Log.VERBOSE)){
            Log.v(MGMapApplication.LABEL, NameUtil.context()+" "+activity.getClass().getSimpleName());
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        if ((this.activity == activity) && Log.isLoggable(MGMapApplication.LABEL, Log.VERBOSE)){
            Log.v(MGMapApplication.LABEL, NameUtil.context()+" "+activity.getClass().getSimpleName());
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if ((this.activity == activity) && Log.isLoggable(MGMapApplication.LABEL, Log.VERBOSE)){
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+activity.getClass().getSimpleName());
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if ((this.activity == activity) && Log.isLoggable(MGMapApplication.LABEL, Log.VERBOSE)){
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+activity.getClass().getSimpleName());
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if ((this.activity == activity) && Log.isLoggable(MGMapApplication.LABEL, Log.VERBOSE)){
            Log.v(MGMapApplication.LABEL, NameUtil.context()+" "+activity.getClass().getSimpleName());
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if ((this.activity == activity) && Log.isLoggable(MGMapApplication.LABEL, Log.VERBOSE)){
                Log.v(MGMapApplication.LABEL, NameUtil.context()+" "+activity.getClass().getSimpleName());
        }
    }
}
