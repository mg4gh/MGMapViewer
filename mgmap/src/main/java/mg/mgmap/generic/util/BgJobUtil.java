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
import android.app.AlertDialog;
import android.content.DialogInterface;
//import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

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
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;

public class BgJobUtil {

    AppCompatActivity activity;
    MGMapApplication application;

    public BgJobUtil(AppCompatActivity activity, MGMapApplication application) {
        this.activity = activity;
        this.application = application;
    }

    public void processConfirmDialog(String title, String message, ArrayList<BgJob> jobs){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(title);
        builder.setMessage(message);

        builder.setPositiveButton("YES", (dialog, which) -> {
            dialog.dismiss();
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " do it." );
            FullscreenUtil.enforceState(activity);

            if (jobs.size() > 0){
                application.addBgJobs(jobs);
            }
        });

        builder.setNegativeButton("NO", (dialog, which) -> {
            // Do nothing
            dialog.dismiss();
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " don't do it." );
            FullscreenUtil.enforceState(activity);
        });

        AlertDialog alert = builder.create();
        alert.show();
        alert.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(jobs.size()>0);
    }

//    public void processConfirmDialog2(String title, String message, ArrayList<BgJob> jobs){
//        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//
//        builder.setTitle(title);
//        builder.setMessage(message);
//
//        builder.setPositiveButton("YES", (dialog, which) -> {
//            dialog.dismiss();
//            Log.i(MGMapApplication.LABEL, NameUtil.context() + " do it." );
//            FullscreenUtil.enforceState(activity);
//
//            if (jobs.size() > 0){
//                application.addBgJobs(jobs);
//            }
//        });
//
//        builder.setNegativeButton("NO", (dialog, which) -> {
//            // Do nothing
//            dialog.dismiss();
//            Log.i(MGMapApplication.LABEL, NameUtil.context() + " don't do it." );
//            FullscreenUtil.enforceState(activity);
//        });
//
//        AlertDialog alert = builder.create();
//        alert.show();
//        alert.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(jobs.size()>0);
//    }


    public static void showToast(Activity activity, CharSequence text){
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(activity, text, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.show();
            }
        });

    }



}
