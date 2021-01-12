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
package mg.mgmap.features.routing;

import android.view.View;

import mg.mgmap.ControlView;
import mg.mgmap.R;
import mg.mgmap.util.Control;
import mg.mgmap.util.Pref;

public class RouteOnOffControl extends Control {

    FSRouting msRouting;
    Pref<Float> prefAlphaRoTL;
    Pref<Float> prefAlphaMTL;

    public RouteOnOffControl(FSRouting msRouting){
        super(true);
        this.msRouting = msRouting;
    }

    @Override
    public void setControlView(ControlView controlView) {
        super.setControlView(controlView);
        prefAlphaRoTL = getPref(R.string.FSRouting_pref_alphaRoTL,1.0f);
        prefAlphaMTL = getPref(R.string.FSMarker_pref_alphaMTL,0.0f);
    }

    public void onClick(View v) {
        super.onClick(v);
        prefAlphaRoTL.setValue((prefAlphaRoTL.getValue() > 0.25f)?0f:1f);
        prefAlphaMTL.setValue(1.0f - prefAlphaRoTL.getValue());
    }

    @Override
    public void onPrepare(View v) {
        setText(v, controlView.rstring((prefAlphaRoTL.getValue() > 0.25f)? R.string.btRoTOff :R.string.btRoTOn) );
    }

}
