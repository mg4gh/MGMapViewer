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
package mg.mgmap.features.position;

import android.view.View;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.features.rtl.RecordingTrackLog;
import mg.mgmap.util.Control;
import mg.mgmap.util.MGPref;

public class GpsControl extends Control {

    private final MGPref<Boolean> prefGps = MGPref.get(R.string.FSPosition_prev_GpsOn, false);

    public GpsControl(){
        super(true);
    }

    public void onClick(View v) {
        super.onClick(v);
        MGMapApplication application = controlView.getApplication();
        MGMapActivity activity = controlView.getActivity();

        prefGps.toggle();
        activity.triggerTrackLoggerService();
        application.lastPositionsObservable.changed();
    }

    @Override
    public void onPrepare(View v) {
        setText(v, controlView.rstring(prefGps.getValue()? R.string.btGpsOff:R.string.btGpsOn) );

        RecordingTrackLog rtl = controlView.getApplication().recordingTrackLogObservable.getTrackLog();
        v.setEnabled( (rtl == null) || (!rtl.isTrackRecording()) );
    }

}
