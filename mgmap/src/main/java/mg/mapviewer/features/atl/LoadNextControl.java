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

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogRef;
import mg.mapviewer.model.TrackLogRefZoom;
import mg.mapviewer.util.Control;

public class LoadNextControl extends Control {

    public LoadNextControl(){
        super(true);
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        MGMapApplication application = controlView.getApplication();
        if (!application.metaTrackLogs.isEmpty()) { // prevent NoSuchElementException in metaTrackLogs.first() and metaTrackLogs.last() call
            TrackLog trackLog = null;
            TrackLog oldSelected = application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog();
            if (oldSelected != null){
                trackLog = application.metaTrackLogs.lower(oldSelected);
            } else {
                trackLog = application.metaTrackLogs.last();
            }
            if (trackLog != null){
                TrackLogRef refSelected = new TrackLogRefZoom(trackLog,trackLog.getNumberOfSegments()-1,true);
                application.availableTrackLogsObservable.setSelectedTrackLogRef(refSelected);
            }
        }

    }

    @Override
    public void onPrepare(View v) {
        setText(v, controlView.rstring(R.string.btLoadNext) );
    }
}
