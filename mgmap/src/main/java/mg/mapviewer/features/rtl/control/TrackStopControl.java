package mg.mapviewer.features.rtl.control;

import android.view.View;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.features.rtl.RecordingTrackLog;
import mg.mapviewer.model.TrackLogRef;
import mg.mapviewer.util.Control;
import mg.mapviewer.util.GpxExporter;
import mg.mapviewer.util.MetaDataUtil;
import mg.mapviewer.util.PersistenceManager;
import mg.mapviewer.util.MGPref;

public class TrackStopControl extends Control {

    private final MGPref<Boolean> prefGps = MGPref.get(R.string.FSPosition_prev_GpsOn, false);

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
