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
package mg.mapviewer.features.routing;

import android.view.View;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.pref.MGPref;

public class RouteOnOffControl extends Control {

    MSRouting msRouting;
    MGPref<Float> prefAlphaRoTL = MGPref.get(R.string.MSRouting_pref_alphaRoTL,1.0f);

    public RouteOnOffControl(MSRouting msRouting){
        super(true);
        this.msRouting = msRouting;
    }

    public void onClick(View v) {
        super.onClick(v);
        MGMapApplication application = controlView.getApplication();
        prefAlphaRoTL.setValue((prefAlphaRoTL.getValue() > 0.25f)?0f:1f);
    }

    @Override
    public void onPrepare(View v) {
        setText(v, controlView.rstring((prefAlphaRoTL.getValue() > 0.25f)? R.string.btRouteOff:R.string.btRouteOn) );
    }

}
