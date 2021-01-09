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
package mg.mgmap.features.atl;

import android.view.View;

import mg.mgmap.R;
import mg.mgmap.util.Control;

public class HideSelectedControl extends Control {

    public HideSelectedControl(){
        super(true);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        controlView.getApplication().availableTrackLogsObservable.removeSelected();
    }

    @Override
    public void onPrepare(View v) {
        v.setEnabled( controlView.getApplication().availableTrackLogsObservable.selectedTrackLogRef.getTrackLog() != null );
        setText(v, controlView.rstring(R.string.btHideSelected) );
    }
}
