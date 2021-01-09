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
package mg.mgmap.features.control;

import android.content.Intent;
import android.view.View;

import mg.mgmap.MGMapActivity;
import mg.mgmap.R;
import mg.mgmap.settings.SettingsActivity;
import mg.mgmap.util.Control;

public class SettingsControl extends Control {

    public SettingsControl(){
        super(true);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        MGMapActivity activity = controlView.getActivity();
        Intent intent = new Intent(activity, SettingsActivity.class);
        activity.startActivity(intent);
    }

    @Override
    public void onPrepare(View v) {
        setText(v, controlView.rstring(R.string.btSettings) );
    }
}
