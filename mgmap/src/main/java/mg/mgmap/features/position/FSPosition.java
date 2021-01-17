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
package mg.mgmap.features.position;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.model.IMapViewPosition;

import java.util.Observable;
import java.util.Observer;

import mg.mgmap.MGMapActivity;
import mg.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.features.rtl.RecordingTrackLog;
import mg.mgmap.model.PointModel;
import mg.mgmap.model.TrackLogPoint;
import mg.mgmap.util.CC;
import mg.mgmap.util.Formatter;
import mg.mgmap.util.Pref;
import mg.mgmap.view.ExtendedTextView;
import mg.mgmap.view.PointView;

public class FSPosition extends FeatureService {

    private static final Paint PAINT_FIX2_FILL = CC.getFillPaint(R.color.RED_A50);
    private static final Paint PAINT_FIX2_STROKE = CC.getStrokePaint(R.color.RED, 5);
    private static final Paint PAINT_ACC_FILL = CC.getFillPaint(R.color.BLUE_A50);
    private static final Paint PAINT_ACC_STROKE = CC.getStrokePaint(R.color.BLUE_A150, 5);

    private final Pref<Boolean> prefAppRestart = getPref(R.string.MGMapApplication_pref_Restart, false);
    private final Pref<Boolean> prefCenter = getPref(R.string.FSPosition_pref_Center, true);
    private final Pref<Boolean> prefGps = getPref(R.string.FSPosition_pref_GpsOn, false);
    private final Pref<Boolean> prefGpsEnabled = new Pref<>(false);
    private final Pref<Boolean> prefRefreshMapView = getPref(R.string.FSPosition_pref_RefreshMapView, false);

    private ExtendedTextView etvHeight = null;

    public FSPosition(MGMapActivity mmActivity) {
        super(mmActivity);
        if (prefAppRestart.getValue()){
            prefCenter.setValue(true);
        }
        getApplication().recordingTrackLogObservable.addObserver((o, arg) -> {
            RecordingTrackLog rtl = getApplication().recordingTrackLogObservable.getTrackLog();
            prefGpsEnabled.setValue( (rtl == null) || (!rtl.isTrackRecording()) );
        });
        prefGps.addObserver(refreshObserver);
        prefCenter.addObserver(refreshObserver);
        getApplication().lastPositionsObservable.addObserver(refreshObserver);
        prefRefreshMapView.addObserver((o, arg) -> refreshMapView());
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
            etv.setData(prefGps,R.drawable.group_record1,R.drawable.group_record2);
            etv.setPrAction(new Pref<>(false));
        }
        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshObserver.onChange();
    }

    @Override
    protected void onPause() {
        super.onPause();
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

        if (pm instanceof TrackLogPoint) {
            TrackLogPoint trackLogPoint = (TrackLogPoint) pm;
            PointView accuracyCircle2 = new PointView(pm, PAINT_ACC_STROKE, PAINT_ACC_FILL);
            register(accuracyCircle2);
            accuracyCircle2.setRadiusMeter(trackLogPoint.getAccuracy());

        }
        register(new PointView(pm, PAINT_FIX2_STROKE, PAINT_FIX2_FILL).setRadius( 6 ));

        getControlView().setStatusLineValue(etvHeight, pm.getEleA());
    }

    private void centerCurrentPosition(PointModel pm){
        if ((pm != null) && prefCenter.getValue()) {
            IMapViewPosition mvp = getMapView().getModel().mapViewPosition;
            LatLong pos = new LatLong(pm.getLat(), pm.getLon());
            mvp.setMapPosition(new MapPosition(pos, mvp.getZoomLevel()));
        }
    }

    public void refreshMapView(){
        IMapViewPosition mvp = getMapView().getModel().mapViewPosition;
        mvp.setMapPosition(new MapPosition(mvp.getCenter(), mvp.getZoomLevel()));
    }


}
