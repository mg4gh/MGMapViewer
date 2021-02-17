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
