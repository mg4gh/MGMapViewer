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
package mg.mapviewer.features.atl;

import android.view.View;

import java.util.ArrayList;
import java.util.List;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.graph.GGraphTile;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.util.Control;

public class HideAllControl extends Control {

    public HideAllControl(){
        super(true);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        MGMapApplication application = controlView.getApplication();
        application.availableTrackLogsObservable.removeAll();
        application.markerTrackLogObservable.setTrackLog(null);
        GGraphTile.clearCache();
    }

    @Override
    public void onPrepare(View v) {
        MGMapApplication application = controlView.getApplication();
        boolean enable = (application.availableTrackLogsObservable.availableTrackLogs.size() > 0);
        enable |= ( application.markerTrackLogObservable.getTrackLog() != null );
        v.setEnabled( enable );
        setText(v, controlView.rstring(R.string.btHideAll) );
    }
}
