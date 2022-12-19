/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.generic.util;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.view.WindowCompat;
import androidx.lifecycle.Lifecycle;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.MGLog;

public class FullscreenUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static void enforceState(AppCompatActivity activity) {
        SharedPreferences sharedPreferences = MGMapApplication.getByContext(activity).getSharedPreferences();
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
        mgLog.i();
        if (activity instanceof MGMapActivity){
            int newUiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
            newUiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            newUiOptions |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            newUiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
                activity.getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            }
        } else {
            hideFullscreen(activity);
        }
    }

    public static void hideFullscreen(Activity activity) {
        mgLog.i();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (activity instanceof MGMapActivity) {
                activity.getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
                WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
                activity.getWindow().setStatusBarColor(0x60000000);
                activity.getWindow().setNavigationBarColor(0x60000000);
            } else {
                activity.getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
                activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            }
        }
        int newUiOptions = activity.getWindow().getDecorView().getSystemUiVisibility();
        newUiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        activity.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }

// formerly used flags with significant impact:
//                activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//                activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

}
