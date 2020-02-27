package mg.mapviewer.features.rtl.control;

import android.view.View;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.features.rtl.RecordingTrackLog;
import mg.mapviewer.util.Control;

public class TrackStopSegmentControl extends Control {

    public TrackStopSegmentControl(){
        super(true);
    }

    public void onClick(View view) {
        super.onClick(view);
        MGMapApplication application = controlView.getApplication();
        MGMapActivity activity = controlView.getActivity();

        long timestamp = System.currentTimeMillis();
        RecordingTrackLog rtl = null;
//        Intent intent = null;

        rtl = application.recordingTrackLogObservable.getTrackLog();
        rtl.stopSegment(timestamp);
        application.gpsOn.setValue(false);
        application.lastPositionsObservable.handlePoint(null);
        activity.triggerTrackLoggerService();
//        intent = new Intent(activity, TrackLoggerService.class);
//
//        activity.startForegroundService(intent);
    }

    @Override
    public void onPrepare(View v) {
        final RecordingTrackLog rtl = controlView.getApplication().recordingTrackLogObservable.getTrackLog();
        v.setEnabled( (rtl != null) && (rtl.isSegmentRecording()) );
        setText(v, controlView.rstring(R.string.btStopSegment) );
    }

}
