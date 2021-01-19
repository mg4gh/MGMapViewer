package mg.mgmap.util;

import android.app.Activity;
import android.content.SharedPreferences;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;

import java.util.Observable;
import java.util.Observer;

import mg.mgmap.R;

public class FullscreenUtil {

    public static void enforceState(AppCompatActivity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        boolean fullscreenOn = sharedPreferences.getBoolean(activity.getResources().getString(R.string.FSControl_qcFullscreenOn), true);
        enforceState(activity, fullscreenOn);
    }

    public static void enforceState(AppCompatActivity activity, boolean fullscreenOn) {

        if (activity.getLifecycle().getCurrentState().isAtLeast( Lifecycle.State.STARTED )){
            if (fullscreenOn){
                setFullscreen(activity);
            } else {
                hideFullscreen(activity);
            }
        }
    }

    public static void setFullscreen(Activity activity) {
        int newUiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

    public static void hideFullscreen(Activity activity) {
        int newUiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        newUiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

}
