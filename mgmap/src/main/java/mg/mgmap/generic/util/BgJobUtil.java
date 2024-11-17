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

import androidx.appcompat.app.AppCompatActivity;

import mg.mgmap.R;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.view.DialogView;

public class BgJobUtil {

    final AppCompatActivity activity;
    final MGMapApplication application;

    public BgJobUtil(AppCompatActivity activity, MGMapApplication application) {
        this.activity = activity;
        this.application = application;
    }

    public void processConfirmDialog(BgJobGroup bgJobGroup){
        DialogView dialogView = activity.findViewById(R.id.dialog_parent);
        dialogView.lock(() -> dialogView
                .setTitle(bgJobGroup.title)
                .setMessage(bgJobGroup.getDetails())
                .setContentView(bgJobGroup.groupCallback.getContentView())
                .setLogPrefix("bgJobGroupConfirm "+bgJobGroup.title)
                .setPositive("YES", evt -> bgJobGroup.doit())
                .setNegative("NO", null)
                .show());
    }

    void reportResult(BgJobGroup bgJobGroup){
        DialogView dialogView = activity.findViewById(R.id.dialog_parent);
        dialogView.lock(() -> dialogView
                .setTitle(bgJobGroup.title)
                .setMessage(bgJobGroup.getResultDetails())
                .setLogPrefix("bgJobGroupResult "+bgJobGroup.title)
                .setPositive("OK", evt -> bgJobGroup.onReportResultOk())
                .setNegative(bgJobGroup.offerRetries ? "Retry" : null, evt -> bgJobGroup.retry())
                .show());

    }

}
