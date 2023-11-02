/*
 * Copyright 2017 - 2021 mg4gh
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
package mg.mgmap.activity.mgmap.features.rtl;

import android.view.ViewGroup;

import org.mapsforge.core.graphics.Paint;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.generic.model.TrackLogRef;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.gpx.GpxExporter;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.activity.mgmap.view.LabeledSlider;

public class FSRecordingTrackLog extends FeatureService {

    private final Paint PAINT_STROKE_RTL = CC.getStrokePaint(R.color.CC_RED, getMapViewUtility().getTrackWidth());
    private final Paint PAINT_STROKE_RTL_GL = CC.getStrokePaint(R.color.CC_GRAY100_A100, getMapViewUtility().getTrackWidth()*1.4f);

    private ViewGroup dashboardRtl = null;
    private ViewGroup dashboardRtls = null;

    private final Pref<Boolean> prefGps = getPref(R.string.FSPosition_pref_GpsOn, false);
    private final Pref<Float> prefAlphaRtl = getPref(R.string.FSRecording_pref_alphaRTL, 1.0f);
    private final Pref<Boolean> prefRtlVisibility = getPref(R.string.FSRecording_pref_RTL_visibility, false);
    private final Pref<Boolean> prefRtlGL = getPref(R.string.FSRecording_pref_rtlGl, false);
    private final Pref<Boolean> toggleRecordTrack = new Pref<>(false);
    private final Pref<Boolean> prefRecordTrack = new Pref<>(false);
    private final Pref<Boolean> toggleRecordSegment = new Pref<>(false);
    private final Pref<Boolean> prefRecordSegment = new Pref<>(false);

    public FSRecordingTrackLog(MGMapActivity mmActivity) {
        super(mmActivity);
        getApplication().recordingTrackLogObservable.addObserver((e) -> {
            final RecordingTrackLog rtl = getApplication().recordingTrackLogObservable.getTrackLog();
            prefRecordTrack.setValue( (rtl != null) && ( rtl.isTrackRecording()) );
            prefRecordSegment.setValue( (rtl != null) && ( rtl.isTrackRecording()) && (rtl.isSegmentRecording()));
        });
        toggleRecordTrack.addObserver((e) -> {
            RecordingTrackLog rtl = getApplication().recordingTrackLogObservable.getTrackLog();
            long timestamp = System.currentTimeMillis();
            if (rtl == null){
                rtl = new RecordingTrackLog( getPersistenceManager(), true);
                rtl.startTrack(timestamp);
                getApplication().recordingTrackLogObservable.setTrackLog(rtl);
                rtl.startSegment(timestamp);
                prefGps.setValue(true);
            } else {
                if (rtl.isSegmentRecording()) {
                    rtl.stopSegment(timestamp);
                    prefGps.setValue(false);
                    getApplication().lastPositionsObservable.handlePoint(null);
                }
                rtl.stopTrack(timestamp);
                if (rtl.getTrackStatistic().getNumPoints() > 0 ){  // ignore empty tracklog
                    GpxExporter.export(getPersistenceManager(), rtl);
                    getPersistenceManager().clearRaw();

                    getApplication().availableTrackLogsObservable.availableTrackLogs.add(rtl);
                    getApplication().metaTrackLogs.put(rtl.getNameKey(), rtl);
                    getApplication().recordingTrackLogObservable.setTrackLog(null);

                    application.getMetaDataUtil().createMetaData(rtl);
                    application.getMetaDataUtil().writeMetaData(application.getPersistenceManager().openMetaOutput(rtl.getName()), rtl);
                    getApplication().availableTrackLogsObservable.availableTrackLogs.add(rtl);

                    TrackLogRef selected = new TrackLogRef(rtl,rtl.getNumberOfSegments()-1);
                    getApplication().availableTrackLogsObservable.setSelectedTrackLogRef(selected);

                    getPref(R.string.preferences_sftp_uploadGpxTrigger, false).toggle(); // new gpx => trigger sync
                } else {
                    getPersistenceManager().clearRaw();
                    getApplication().recordingTrackLogObservable.setTrackLog(null);
                }
            }
        });
        toggleRecordSegment.addObserver((e) -> {
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
        });
        getApplication().recordingTrackLogObservable.addObserver(refreshObserver);
        prefAlphaRtl.addObserver(refreshObserver);
        prefRtlGL.addObserver(refreshObserver);
    }


    @Override
    public ViewGroup initDashboard(ViewGroup dvg, String info) {
        super.initDashboard(dvg,info);
        getControlView().setViewGroupColors(dvg, R.color.CC_WHITE, R.color.CC_RED100_A100);
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
            lsl.initPrefData(prefRtlVisibility, prefAlphaRtl, CC.getColor(R.color.CC_RED), "RecordingTrackLog");
        }
        return lsl;
    }



    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info) {
        super.initQuickControl(etv,info);
        if ("track".equals(info)){
            etv.setData(prefRecordTrack,R.drawable.record_track1,R.drawable.record_track2);
            etv.setPrAction(toggleRecordTrack);
            etv.setHelp(r(R.string.FSRecording_qcRec_help)).setHelp(r(R.string.FSRecording_qcRec_help1),r(R.string.FSRecording_qcRec_help2));
        } else if ("segment".equals(info)){
            etv.setData(prefRecordSegment,R.drawable.record_segment1,R.drawable.record_segment2);
            etv.setPrAction(toggleRecordSegment);
            etv.setDisabledData(prefRecordTrack, R.drawable.record_segment_dis);
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
        showTrack(rtl,prefRtlGL,PAINT_STROKE_RTL_GL,PAINT_STROKE_RTL, prefAlphaRtl.getValue(), -1);
        getControlView().setDashboardValue(true, dashboardRtl ,rtl.getTrackStatistic());
        int segIdx = rtl.getNumberOfSegments()-1;
        getControlView().setDashboardValue(segIdx > 0, dashboardRtls,(segIdx>0)?rtl.getTrackLogSegment(segIdx).getStatistic():null);
    }

    private synchronized void hideRecordingTrackLog() {
        unregisterAll();
        getControlView().setDashboardValue(false,dashboardRtl,null);
        getControlView().setDashboardValue(false,dashboardRtls,null);
    }
}
