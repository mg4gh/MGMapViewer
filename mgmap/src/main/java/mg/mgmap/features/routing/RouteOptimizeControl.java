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

import mg.mgmap.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.model.WriteableTrackLog;
import mg.mgmap.util.Control;
import mg.mgmap.util.MGPref;

public class RouteOptimizeControl extends Control {

    FSRouting msRouting;

    public RouteOptimizeControl(FSRouting msRouting){
        super(true);
        this.msRouting = msRouting;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        msRouting.optimize();
    }

    @Override
    public void onPrepare(View v) {
        MGMapApplication application = controlView.getApplication();
        WriteableTrackLog mtl = application.markerTrackLogObservable.getTrackLog();

        v.setEnabled( (mtl != null) && (MGPref.get(R.string.FSRouting_pref_alphaRoTL,1.0f).getValue() > 0.25) );
        setText(v, controlView.rstring(R.string.btRoTMapMatching) );
    }
}
