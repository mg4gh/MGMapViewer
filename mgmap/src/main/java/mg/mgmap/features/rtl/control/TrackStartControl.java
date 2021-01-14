package mg.mgmap.features.rtl.control;

import android.view.View;

import mg.mgmap.ControlView;
import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.features.rtl.RecordingTrackLog;
import mg.mgmap.util.Control;
import mg.mgmap.util.Pref;

public class TrackStartControl extends Control {

    private Pref<Boolean> prefGps;

    @Override
    public void setControlView(ControlView controlView) {
        super.setControlView(controlView);
        prefGps = getPref(R.string.FSPosition_pref_GpsOn, false);
    }

    public TrackStartControl(){
        super(true);
    }

    public void onClick(View view) {
        super.onClick(view);
        MGMapApplication application = controlView.getApplication();
        MGMapActivity activity = controlView.getActivity();

        long timestamp = System.currentTimeMillis();
        RecordingTrackLog rtl = null;

        rtl = new RecordingTrackLog(true);
        application.recordingTrackLogObservable.setTrackLog(rtl);
        rtl.startTrack(timestamp);
        rtl.startSegment(timestamp);
        prefGps.setValue(true);
        activity.triggerTrackLoggerService();
    }

    @Override
    public void onPrepare(View v) {
        final RecordingTrackLog rtl = controlView.getApplication().recordingTrackLogObservable.getTrackLog();
        v.setEnabled( (rtl == null) || (! rtl.isTrackRecording()) );
        setText(v, controlView.rstring(R.string.btStartTrack) );
    }

}
