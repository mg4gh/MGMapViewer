package mg.mgmap.features.rtl.control;

import android.view.View;

import mg.mgmap.ControlView;
import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.features.rtl.RecordingTrackLog;
import mg.mgmap.util.Control;
import mg.mgmap.util.Pref;

public class TrackStopSegmentControl extends Control {

    private Pref<Boolean> prefGps;

    @Override
    public void setControlView(ControlView controlView) {
        super.setControlView(controlView);
        prefGps = getPref(R.string.FSPosition_pref_GpsOn, false);
    }

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
