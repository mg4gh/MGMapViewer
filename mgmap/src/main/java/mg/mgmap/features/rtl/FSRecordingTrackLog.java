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
package mg.mgmap.features.rtl;

import android.view.ViewGroup;

import org.mapsforge.core.graphics.Paint;

import java.util.Observable;
import java.util.Observer;

import mg.mgmap.MGMapActivity;
import mg.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.features.rtl.control.TrackStartControl;
import mg.mgmap.features.rtl.control.TrackStartSegmentControl;
import mg.mgmap.features.rtl.control.TrackStopControl;
import mg.mgmap.features.rtl.control.TrackStopSegmentControl;
import mg.mgmap.model.TrackLogRef;
import mg.mgmap.util.CC;
import mg.mgmap.util.Control;
import mg.mgmap.util.GpxExporter;
import mg.mgmap.util.MGPref;
import mg.mgmap.util.MetaDataUtil;
import mg.mgmap.util.PersistenceManager;
import mg.mgmap.view.ExtendedTextView;
import mg.mgmap.view.LabeledSlider;

public class FSRecordingTrackLog extends FeatureService {

    private final Paint PAINT_STROKE_RTL = CC.getStrokePaint(R.color.RED, getMapViewUtility().getTrackWidth());
    private final Paint PAINT_STROKE_RTL_GL = CC.getStrokePaint(R.color.GRAY100_A100, getMapViewUtility().getTrackWidth()*1.4f);

    private ViewGroup dashboardRtl = null;
    private ViewGroup dashboardRtls = null;

    private final MGPref<Boolean> prefGps = MGPref.get(R.string.FSPosition_prev_GpsOn, false);
    private final MGPref<Float> prefAlphaRtl = MGPref.get(R.string.FSRecording_pref_alphaRTL, 1.0f);
    private final MGPref<Boolean> prefRtlVisibility = MGPref.get(R.string.FSRecording_pref_RTL_visibility, false);
    private final MGPref<Boolean>  prefRtlGL = MGPref.get(R.string.FSRecording_pref_rtlGl_key, false);
    private final MGPref<Boolean> prefRecordTrackAction = MGPref.anonymous(false);
    private final MGPref<Boolean> prefRecordTrackState = MGPref.anonymous(false);
    private final MGPref<Boolean> prefRecordSegmentAction = MGPref.anonymous(false);
    private final MGPref<Boolean> prefRecordSegmentState = MGPref.anonymous(false);

    public FSRecordingTrackLog(MGMapActivity mmActivity) {
        super(mmActivity);
        getApplication().recordingTrackLogObservable.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                final RecordingTrackLog rtl = getApplication().recordingTrackLogObservable.getTrackLog();
                prefRecordTrackState.setValue( (rtl != null) && ( rtl.isTrackRecording()) );
                prefRecordSegmentState.setValue( (rtl != null) && ( rtl.isTrackRecording()) && (rtl.isSegmentRecording()));
            }
        });
        prefRecordTrackAction.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                RecordingTrackLog rtl = getApplication().recordingTrackLogObservable.getTrackLog();
                long timestamp = System.currentTimeMillis();
                if (rtl == null){
                    rtl = new RecordingTrackLog(true);
                    getApplication().recordingTrackLogObservable.setTrackLog(rtl);
                    rtl.startTrack(timestamp);
                    rtl.startSegment(timestamp);
                    prefGps.setValue(true);
                } else {
                    if (rtl.isSegmentRecording()) {
                        rtl.stopSegment(timestamp);
                        prefGps.setValue(false);
                        getApplication().lastPositionsObservable.handlePoint(null);
                    }
                    rtl.stopTrack(timestamp);
                    GpxExporter.export(rtl);
                    PersistenceManager.getInstance().clearRaw();

                    getApplication().availableTrackLogsObservable.availableTrackLogs.add(rtl);
                    getApplication().metaTrackLogs.put(rtl.getNameKey(), rtl);
                    getApplication().recordingTrackLogObservable.setTrackLog(null);

                    MetaDataUtil.createMetaData(rtl);
                    MetaDataUtil.writeMetaData(PersistenceManager.getInstance().openMetaOutput(rtl.getName()), rtl);
                    getApplication().availableTrackLogsObservable.availableTrackLogs.add(rtl);

                    TrackLogRef selected = new TrackLogRef(rtl,rtl.getNumberOfSegments()-1);
                    getApplication().availableTrackLogsObservable.setSelectedTrackLogRef(selected);
                }
            }
        });
        prefRecordSegmentAction.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                long timestamp = System.currentTimeMillis();
                RecordingTrackLog rtl = getApplication().recordingTrackLogObservable.getTrackLog();
                if ((rtl != null) && (rtl.isTrackRecording())){
                    if (!rtl.isSegmentRecording()){
                        rtl.startSegment(timestamp);
                        prefGps.setValue(true);
                    } else {
                        rtl.stopSegment(timestamp);
                        prefGps.setValue(false);
                        getApplication().lastPositionsObservable.handlePoint(null);
                    }
                }
            }
        });
        getApplication().recordingTrackLogObservable.addObserver(refreshObserver);
        prefAlphaRtl.addObserver(refreshObserver);
        prefRtlGL.addObserver(refreshObserver);
    }


    @Override
    public ViewGroup initDashboard(ViewGroup dvg, String info) {
        super.initDashboard(dvg,info);
        getControlView().setViewGroupColors(dvg, R.color.WHITE, R.color.RED100_A100);
        if ("rtl".equals(info)) {
            dashboardRtl = dvg;
        }
        if ("rtls".equals(info)) {
            dashboardRtls = dvg;
        }
        return dvg;
    }

    @Override
    public LabeledSlider initLabeledSlider(LabeledSlider lsl, String info) {
        if ("rtl".equals(info)) {
            lsl.initPrefData(prefRtlVisibility, prefAlphaRtl, CC.getColor(R.color.RED), "RecordingTrackLog");
        }
        return lsl;
    }



    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info) {
        super.initQuickControl(etv,info);
        if ("track".equals(info)){
            etv.setData(prefRecordTrackState,R.drawable.record_track1,R.drawable.record_track2);
            etv.setPrAction(prefRecordTrackAction);
            etv.setHelp(r(R.string.FSRecording_qcRec_help)).setHelp(r(R.string.FSRecording_qcRec_help1),r(R.string.FSRecording_qcRec_help2));
        } else if ("segment".equals(info)){
            etv.setData(prefRecordSegmentState,R.drawable.record_segment1,R.drawable.record_segment2);
            etv.setPrAction(prefRecordSegmentAction);
            etv.setDisabledData(prefRecordTrackState, R.drawable.record_segment_dis);
            etv.setHelp(r(R.string.FSRecording_qcRecSeg_help)).setHelp(r(R.string.FSRecording_qcRecSeg_help1),r(R.string.FSRecording_qcRecSeg_help2));
        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void doRefreshResumedUI() {
        RecordingTrackLog rtl = getApplication().recordingTrackLogObservable.getTrackLog();
        boolean bRtlAlphaVisibility = false;
        if ((rtl != null) && (rtl.isTrackRecording())) {
            showRecordingTrackLog(rtl);
            bRtlAlphaVisibility = (rtl.getTrackStatistic().getNumPoints() >= 2);
        } else {
            hideRecordingTrackLog();
        }
        prefRtlVisibility.setValue(bRtlAlphaVisibility);
    }


    private void showRecordingTrackLog(RecordingTrackLog rtl) {
        unregisterAll();
        if (prefRtlGL.getValue()){
            CC.initGlPaints( prefAlphaRtl.getValue() );
            showTrack(rtl, CC.getAlphaClone(PAINT_STROKE_RTL_GL, prefAlphaRtl.getValue()), true);
        } else {
            showTrack(rtl, CC.getAlphaClone(PAINT_STROKE_RTL, prefAlphaRtl.getValue()), false);
        }
        getControlView().setDashboardValue(true, dashboardRtl ,rtl.getTrackStatistic());
        int segIdx = rtl.getNumberOfSegments()-1;
        getControlView().setDashboardValue(segIdx > 0, dashboardRtls,(segIdx>0)?rtl.getTrackLogSegment(segIdx).getStatistic():null);
    }

    private synchronized void hideRecordingTrackLog() {
        unregisterAll();
        getControlView().setDashboardValue(false,dashboardRtl,null);
        getControlView().setDashboardValue(false,dashboardRtls,null);
    }

    public Control[] getMenuTrackControls(){

        return new Control[]{
                new TrackStartControl(),
                new TrackStartSegmentControl(),
                new TrackStopSegmentControl(),
                new TrackStopControl() };
    }
}