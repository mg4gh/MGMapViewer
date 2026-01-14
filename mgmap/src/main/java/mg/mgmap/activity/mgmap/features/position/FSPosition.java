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
package mg.mgmap.activity.mgmap.features.position;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.map.view.InputListener;

import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.rtl.RecordingTrackLog;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.activity.mgmap.view.PointView;

public class FSPosition extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final Paint PAINT_FIX2_FILL = CC.getFillPaint(R.color.CC_RED_A50);
    private static final Paint PAINT_FIX2_STROKE = CC.getStrokePaint(R.color.CC_RED, 5);
    private static final Paint PAINT_ACC_FILL = CC.getFillPaint(R.color.CC_BLUE_A50);
    private static final Paint PAINT_ACC_STROKE = CC.getStrokePaint(R.color.CC_BLUE_A150, 5);

    private final Pref<Boolean> prefAppRestart = getPref(R.string.MGMapApplication_pref_Restart, false);
    private final Pref<Boolean> prefCenter = getPref(R.string.FSPosition_pref_Center, true);
    private final Pref<Boolean> prefGps = getPref(R.string.FSPosition_pref_GpsOn, false);
    private final Pref<Boolean> prefGpsEnabled = new Pref<>(Boolean.FALSE);
    private final Pref<Boolean> prefShareWithActive = getPref(R.string.FSShareLoc_shareWithActive,false);
    private final Pref<Boolean> prefRefreshMapView = getPref(R.string.FSPosition_pref_RefreshMapView, false);
    private final Pref<Boolean> prefMapMoving = getPref(R.string.FSPosition_pref_MapMoving, false);
    private final Pref<Boolean> prefEditMarkerTrack =  getPref(R.string.FSMarker_qc_EditMarkerTrack, false);
    private final Pref<Boolean> prefBboxOn = getPref(R.string.FSBB_qc_bboxOn, false);

    private ExtendedTextView etvHeight = null;

    public FSPosition(MGMapActivity mmActivity) {
        super(mmActivity);
        if (prefAppRestart.getValue()){
            prefCenter.setValue(true);
        }
        getApplication().recordingTrackLogObservable.addObserver((e) -> {
            RecordingTrackLog rtl = getApplication().recordingTrackLogObservable.getTrackLog();
            prefGpsEnabled.setValue( (rtl == null) || (!rtl.isTrackRecording()) );
        });
        prefGps.addObserver(refreshObserver);
        prefCenter.addObserver(refreshObserver);
        getApplication().lastPositionsObservable.addObserver(refreshObserver);
        prefEditMarkerTrack.addObserver((e) -> setupTTMapMovingOff());
        prefBboxOn.addObserver((e) -> setupTTMapMovingOff());
        prefRefreshMapView.addObserver((e) -> refreshMapView());

        prefMapMoving.addObserver((e) -> {
            if ( ! prefMapMoving.getValue() ){
                refreshObserver.onChange();
                cancelTTMapMovingOff();
            }
        });
        getMapView().addInputListener(new InputListener() {
            @Override
            public void onMoveEvent() {
                if (prefGps.getValue()){
                    setupTTMapMovingOff();
                }
            }
            @Override
            public void onZoomEvent() { }
        });
    }

    @Override
    public ExtendedTextView initStatusLine(ExtendedTextView etv, String info) {
        super.initStatusLine(etv,info);
        if (info.equals("height")){
            etv.setData(R.drawable.ele);
            etv.setFormat(Formatter.FormatType.FORMAT_HEIGHT);
            etvHeight = etv;
        }
        return etv;
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info) {
        super.initQuickControl(etv,info);
        if ("gps".equals(info)){
            etv.setData(prefGps, R.drawable.gps1,R.drawable.gps2);
            etv.setPrAction(prefGps);
            etv.setDisabledData(prefGpsEnabled, R.drawable.gps_dis);
            etv.setHelp(r(R.string.FSPosition_qcGps_Help)).setHelp(r(R.string.FSPosition_qcGps_Help1),r(R.string.FSPosition_qcGps_Help2));
        } else if ("center".equals(info)){
            etv.setData(prefCenter,R.drawable.center2,R.drawable.center1);
            etv.setPrAction(prefCenter);
            etv.setDisabledData(prefGps, R.drawable.center_dis);
            etv.setHelp(r(R.string.FSPosition_qcCenter_Help)).setHelp(r(R.string.FSPosition_qcCenter_Help1),r(R.string.FSPosition_qcCenter_Help2));
        } else if ("group_record".equals(info)){
            etv.setData(prefGps, prefShareWithActive,R.drawable.group_record1, R.drawable.group_record2, R.drawable.group_record1, R.drawable.group_record3);
            etv.setPrAction(new Pref<>(Boolean.FALSE));
        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefMapMoving.setValue(false);
        refreshObserver.onChange();
    }

    @Override
    protected void onPause() {
        super.onPause();
        getTimer().removeCallbacks(ttMapMovingOff);
    }

    @Override
    protected void doRefreshResumedUI() {
        PointModel pm = getApplication().lastPositionsObservable.lastGpsPoint;
        if (prefGps.getValue() && (pm != null)){
            showPosition(pm);
            centerCurrentPosition(pm);
        } else {
            hidePosition();
        }
    }

    private void hidePosition(){
        unregisterAll();
        getControlView().setStatusLineValue(etvHeight, PointModel.NO_ELE);
    }

    private void showPosition(PointModel pm) {
        hidePosition();

        if (pm instanceof TrackLogPoint trackLogPoint) {
            PointView accuracyCircle2 = new PointView(pm, PAINT_ACC_STROKE, PAINT_ACC_FILL);
            register(accuracyCircle2);
            accuracyCircle2.setRadiusMeter(trackLogPoint.getNmeaAcc());

        }
        register(new PointView(pm, PAINT_FIX2_STROKE, PAINT_FIX2_FILL).setRadius( 6 ));

        getControlView().setStatusLineValue(etvHeight, pm.getEle());
    }

    private void centerCurrentPosition(PointModel pm){
        if ((pm != null) && prefCenter.getValue() && !prefMapMoving.getValue() && !prefEditMarkerTrack.getValue() && !prefBboxOn.getValue()) {
            getMapViewUtility().setCenter(pm);
        }
    }

    public void refreshMapView(){
        PointModel pm = getMapViewUtility().getCenter();
        getMapViewUtility().setCenter(pm);
    }

    private final Runnable ttMapMovingOff = () -> prefMapMoving.setValue(false);
    private void setupTTMapMovingOff(){
        prefMapMoving.setValue(true);
        getTimer().removeCallbacks(ttMapMovingOff);
        getTimer().postDelayed(ttMapMovingOff, 7000);
        mgLog.v("setup MapMoving 7000 ");
    }
    private void cancelTTMapMovingOff(){
        mgLog.v("cancel MapMoving Timer ");
        getTimer().removeCallbacks(ttMapMovingOff);
    }

    public boolean check4MapMovingOff(PointModel pointModel){
        if (prefMapMoving.getValue()){
            double dist = PointModelUtil.distance(pointModel, getMapViewUtility().getCenter());
            if (getMapViewUtility().isClose(dist)){
                prefMapMoving.setValue(false);
                return true;
            }
        }
        return false;
    }
}
