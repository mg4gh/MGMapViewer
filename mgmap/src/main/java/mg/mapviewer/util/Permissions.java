/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mapviewer.util;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.os.Handler;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import mg.mapviewer.MGMapApplication;

/** Contains methos to check and request permissions. Additionally provides an application restart utility. */
public class Permissions {

    public static boolean check(Context context, String permission) {
        return (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED);
    }

    public static boolean check(Context context, String[] permissions) {
        boolean bRes = true;
        for (String permission : permissions){
            bRes &= check(context, permission);
        }
        return bRes;
    }


    public static void request(Activity activity, String permission, int requestCode) {
        String[] permissions = new String[]{permission};
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    public static void request(Activity activity, String[] permissions, int requestCode) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    static Handler timer = new Handler();

    public static void doRestart(final Context c) {
        timer.postDelayed(new Runnable() {
            @Override
            public void run() {
                doRestart2(c);
            }
        }, 10);
    }

    /**
     * copied from https://stackoverflow.com/questions/6609414/how-do-i-programmatically-restart-an-android-app
     * @param c
     */
    public static void doRestart2(Context c) {
        try {
            //check if the context is given
            if (c != null) {
                //fetch the packagemanager so we can get the default launch activity
                // (you can replace this intent with any other activity if you want
                PackageManager pm = c.getPackageManager();
                //check if we got the PackageManager
                if (pm != null) {
                    //create the intent with the default start activity for your application
                    Intent mStartActivity = pm.getLaunchIntentForPackage(
                            c.getPackageName()
                    );
                    if (mStartActivity != null) {
                        mStartActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        //create a pending intent so the application is restarted after System.exit(0) was called.
                        // We use an AlarmManager to call this intent in 100ms
                        int mPendingIntentId = 223344;
                        PendingIntent mPendingIntent = PendingIntent
                                .getActivity(c, mPendingIntentId, mStartActivity,
                                        PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        //kill the application
                        System.exit(0);
                    } else {
                        Log.e(MGMapApplication.LABEL, NameUtil.context() + " Was not able to restart application, mStartActivity null");
                    }
                } else {
                    Log.e(MGMapApplication.LABEL, NameUtil.context() + " Was not able to restart application, PM null");
                }
            } else {
                Log.e(MGMapApplication.LABEL, NameUtil.context() + " Was not able to restart application, Context null");
            }
        } catch (Exception ex) {
            Log.e(MGMapApplication.LABEL, NameUtil.context() + " Was not able to restart application");
        }
    }

}
