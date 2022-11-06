/*
 * Copyright 2017 - 2022 mg4gh
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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;

public class BgJobUtil {

    AppCompatActivity activity;
    MGMapApplication application;

    public BgJobUtil(AppCompatActivity activity, MGMapApplication application) {
        this.activity = activity;
        this.application = application;
    }

    public void processConfirmDialog(BgJobGroup bgJobGroup){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(bgJobGroup.title);
        builder.setMessage(bgJobGroup.getDetails());

        builder.setPositiveButton("YES", (dialog, which) -> {
            dialog.dismiss();
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " do it." );
            FullscreenUtil.enforceState(activity);
            bgJobGroup.doit();
        });

        builder.setNegativeButton("NO", (dialog, which) -> {
            // Do nothing
            dialog.dismiss();
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " don't do it." );
            FullscreenUtil.enforceState(activity);
        });

        AlertDialog alert = builder.create();
        alert.show();
        alert.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(bgJobGroup.size()>0);
    }

    void reportResult(BgJobGroup bgJobGroup){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle(bgJobGroup.title);
        builder.setMessage(bgJobGroup.getResultDetails());

        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            bgJobGroup.onReportResultOk();
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " ok" );
        });

        if (bgJobGroup.offerRetries){
            builder.setNegativeButton("Retry", (dialog, which) -> {
                Log.i(MGMapApplication.LABEL, NameUtil.context() + " Retry" );
                bgJobGroup.retry();
            });
        }

        AlertDialog alert = builder.create();
        alert.show();
    }

}
