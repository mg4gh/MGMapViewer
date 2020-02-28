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
package mg.mapviewer.features.rtl;

import org.mapsforge.core.graphics.Paint;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.features.rtl.control.TrackStartControl;
import mg.mapviewer.features.rtl.control.TrackStartSegmentControl;
import mg.mapviewer.features.rtl.control.TrackStopControl;
import mg.mapviewer.features.rtl.control.TrackStopSegmentControl;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.Control;

public class MSRecordingTrackLog extends MGMicroService {

    private final Paint PAINT_STROKE_RTL = CC.getStrokePaint(R.color.RED, getMapViewUtility().getTrackWidth());

    public MSRecordingTrackLog(MGMapActivity mmActivity) {
        super(mmActivity);
    }

    @Override
    protected void start() {
        getApplication().recordingTrackLogObservable.addObserver(refreshObserver);
    }

    @Override
    protected void stop() {
        getApplication().recordingTrackLogObservable.deleteObserver(refreshObserver);
    }

    @Override
    protected void doRefresh() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                RecordingTrackLog rtl = getApplication().recordingTrackLogObservable.getTrackLog();
                if ((rtl != null) && (rtl.isTrackRecording())) {
                    showRecordingTrackLog(rtl);
                } else {
                    hideRecordingTrackLog();
                }
            }
        });
    }


    private void showRecordingTrackLog(RecordingTrackLog rtl) {
        unregisterAll();
        showTrack(rtl, PAINT_STROKE_RTL, false);

        getControlView().showHideUpdateDashboard(true, R.id.rtl ,rtl.getTrackStatistic());
        int segIdx = rtl.getNumberOfSegments()-1;
        getControlView().showHideUpdateDashboard(segIdx > 0, R.id.rtls,(segIdx>0)?rtl.getTrackLogSegment(segIdx).getStatistic():null);
    }

    private synchronized void hideRecordingTrackLog() {
        unregisterAll();
        getControlView().showHideUpdateDashboard(false,R.id.rtl,null);
        getControlView().showHideUpdateDashboard(false,R.id.rtls,null);
    }

    public Control[] getMenuTrackControls(){

        return new Control[]{
                new TrackStartControl(),
                new TrackStartSegmentControl(),
                new TrackStopSegmentControl(),
                new TrackStopControl() };


    }
}
