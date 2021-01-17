package mg.mgmap.features.rtl.control;

import android.view.View;

import mg.mgmap.ControlView;
import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.features.rtl.RecordingTrackLog;
import mg.mgmap.model.TrackLogRef;
import mg.mgmap.util.Control;
import mg.mgmap.util.GpxExporter;
import mg.mgmap.util.MetaDataUtil;
import mg.mgmap.util.PersistenceManager;
import mg.mgmap.util.Pref;

public class TrackStopControl extends Control {

    private Pref<Boolean> prefGps;

    @Override
    public void setControlView(ControlView controlView) {
        super.setControlView(controlView);
        prefGps = getPref(R.string.FSPosition_pref_GpsOn, false);
    }

    public TrackStopControl(){
        super(true);
    }

    public void onClick(View view) {
        super.onClick(view);
        MGMapApplication application = controlView.getApplication();
        MGMapActivity activity = controlView.getActivity();

        long timestamp = System.currentTimeMillis();
        RecordingTrackLog rtl = null;

        rtl = application.recordingTrackLogObservable.getTrackLog();
        if (rtl.isSegmentRecording()) {
            rtl.stopSegment(timestamp);
            prefGps.setValue(false);
            application.lastPositionsObservable.handlePoint(null);
            activity.triggerTrackLoggerService();
        }
        rtl.stopTrack(timestamp);
        GpxExporter.export(rtl);
        PersistenceManager.getInstance().clearRaw();

        application.availableTrackLogsObservable.availableTrackLogs.add(rtl);
        application.metaTrackLogs.put(rtl.getNameKey(), rtl);
        application.recordingTrackLogObservable.setTrackLog(null);

        MetaDataUtil.createMetaData(rtl);
        MetaDataUtil.writeMetaData(PersistenceManager.getInstance().openMetaOutput(rtl.getName()), rtl);
        application.availableTrackLogsObservable.availableTrackLogs.add(rtl);

        TrackLogRef selected = new TrackLogRef(rtl,rtl.getNumberOfSegments()-1);
        application.availableTrackLogsObservable.setSelectedTrackLogRef(selected);


    }

    @Override
    public void onPrepare(View v) {
        final RecordingTrackLog rtl = controlView.getApplication().recordingTrackLogObservable.getTrackLog();
        v.setEnabled( (rtl != null) && (rtl.isTrackRecording()) );
        setText(v, controlView.rstring(R.string.btStopTrack) );
    }

}
