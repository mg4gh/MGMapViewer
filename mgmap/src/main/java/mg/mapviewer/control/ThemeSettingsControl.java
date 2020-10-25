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
package mg.mapviewer.control;

import android.content.Intent;
import android.view.View;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.R;
import mg.mapviewer.ThemeSettings;
import mg.mapviewer.util.Control;

public class ThemeSettingsControl extends Control {

    public ThemeSettingsControl(){
        super(true);
    }

    public void onClick(View v) {
        super.onClick(v);
        MGMapActivity activity = controlView.getActivity();
        Intent intent = new Intent(activity, ThemeSettings.class);
        if (activity.getRenderThemeStyleMenu() != null) {
            intent.putExtra(activity.getResources().getString(R.string.my_rendertheme_menu_key), activity.getRenderThemeStyleMenu());
        }
        activity.startActivity(intent);
    }

    @Override
    public void onPrepare(View v) {
        setText(v, controlView.rstring(R.string.btThemes) );
    }
}
