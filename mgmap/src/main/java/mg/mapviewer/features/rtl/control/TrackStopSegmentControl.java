package mg.mapviewer.features.rtl.control;

import android.view.View;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.features.rtl.RecordingTrackLog;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.MGPref;

public class TrackStopSegmentControl extends Control {

    private final MGPref<Boolean> prefGps = MGPref.get(R.string.FSPosition_prev_GpsOn, false);

    public TrackStopSegmentControl(){
        super(true);
    }

    public void onClick(View view) {
        super.onClick(view);
        MGMapApplication application = controlView.getApplication();
        MGMapActivity activity = controlView.getActivity();

        long timestamp = System.currentTimeMillis();
        RecordingTrackLog rtl = application.recordingTrackLogObservable.getTrackLog();
        rtl.stopSegment(timestamp);
        prefGps.setValue(false);
        application.lastPositionsObservable.handlePoint(null);
        activity.triggerTrackLoggerService();
    }

    @Override
    public void onPrepare(View v) {
        final RecordingTrackLog rtl = controlView.getApplication().recordingTrackLogObservable.getTrackLog();
        v.setEnabled( (rtl != null) && (rtl.isSegmentRecording()) );
        setText(v, controlView.rstring(R.string.btStopSegment) );
    }

}
