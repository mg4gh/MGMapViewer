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

import android.view.View;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.features.routing.MSRouting;
import mg.mapviewer.features.rtl.RecordingTrackLog;
import mg.mapviewer.util.Control;

public class GpsControl extends Control {

    public GpsControl(){
        super(true);
    }

    public void onClick(View v) {
        super.onClick(v);
        MGMapApplication application = controlView.getApplication();
        MGMapActivity activity = controlView.getActivity();

        application.gpsOn.toggle();
        activity.triggerTrackLoggerService();
        application.lastPositionsObservable.changed();
    }

    @Override
    public void onPrepare(View v) {
        boolean bGps = controlView.getApplication().gpsOn.getValue();
        setText(v, controlView.rstring(bGps? R.string.btGpsOff:R.string.btGpsOn) );

        RecordingTrackLog rtl = controlView.getApplication().recordingTrackLogObservable.getTrackLog();
        v.setEnabled( (rtl == null) || (!rtl.isTrackRecording()) );
    }

}
