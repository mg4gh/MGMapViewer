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
package mg.mapviewer.features.control;

import android.content.Intent;
import android.view.View;

import mg.mapviewer.HeightProfileActivity;
import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.util.Control;

public class HeightProfileControl extends Control {

    public HeightProfileControl(){
        super(true);
    }

    public void onClick(View v) {
        super.onClick(v);
        MGMapActivity activity = controlView.getActivity();
        Intent intent = new Intent(activity, HeightProfileActivity.class);
        activity.startActivity(intent);
    }

    @Override
    public void onPrepare(View v) {
        setText(v, controlView.rstring(R.string.btHeightProfile) );
        MGMapApplication application = controlView.getApplication();
        v.setEnabled(HeightProfileActivity.check4trackLogRef(application));
    }

}
