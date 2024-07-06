package mg.mgmap.application.util;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.invoke.MethodHandles;

import mg.mgmap.generic.util.basic.MGLog;

public class ActivityLifecycleAdapter implements Application.ActivityLifecycleCallbacks {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        mgLog.d(activity.getClass().getName());
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        mgLog.d(activity.getClass().getName());
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        mgLog.d(activity.getClass().getName());
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        mgLog.d(activity.getClass().getName());
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        mgLog.d(activity.getClass().getName());
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        mgLog.d(activity.getClass().getName());
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        mgLog.d(activity.getClass().getName());
    }
}
