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
package mg.mapviewer.features.position;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.model.IMapViewPosition;

import java.util.Observable;
import java.util.Observer;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.features.rtl.RecordingTrackLog;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLogPoint;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.view.ExtendedTextView;
import mg.mapviewer.view.PointView;
import mg.mapviewer.view.PrefTextView;

public class MSPosition extends MGMicroService {

    private static final Paint PAINT_FIX2_FILL = CC.getFillPaint(R.color.RED_A50);
    private static final Paint PAINT_FIX2_STROKE = CC.getStrokePaint(R.color.RED, 5);
    private static final Paint PAINT_ACC_FILL = CC.getFillPaint(R.color.BLUE_A50);
    private static final Paint PAINT_ACC_STROKE = CC.getStrokePaint(R.color.BLUE_A150, 5);

    private final MGPref<Boolean> prefAppRestart = MGPref.get(R.string.MGMapApplication_pref_Restart, false);
    private final MGPref<Boolean> prefCenter = MGPref.get(R.string.MSPosition_prev_Center, true);
    private final MGPref<Boolean> prefGps = MGPref.get(R.string.MSPosition_prev_GpsOn, false);
    private final MGPref<Boolean> prefGpsEnabled = MGPref.anonymous(false);

    private PrefTextView ptvHeight = null;

    public MSPosition(MGMapActivity mmActivity) {
        super(mmActivity);
        if (prefAppRestart.getValue()){
            prefCenter.setValue(true);
        }
        getApplication().recordingTrackLogObservable.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                RecordingTrackLog rtl = getApplication().recordingTrackLogObservable.getTrackLog();
                prefGpsEnabled.setValue( (rtl == null) || (!rtl.isTrackRecording()) );
            }
        });
        prefGps.addObserver(refreshObserver);
    }

    @Override
    public PrefTextView initStatusLine(PrefTextView ptv, String info) {
        if (info.equals("height")){
            ptv.setPrefData(null, new int[]{R.drawable.ele});
            ptv.setFormat(Formatter.FormatType.FORMAT_HEIGHT);
            ptvHeight = ptv;
        }
        return ptv;
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info) {
        if ("gps".equals(info)){
            etv.setData(prefGps, R.drawable.gps1,R.drawable.gps2);
            etv.setPrAction(prefGps);
            etv.setDisabledData(prefGpsEnabled, R.drawable.gps_dis);
            etv.setHelp(r(R.string.MSPosition_qcGps_Help)).setHelp(r(R.string.MSPosition_qcGps_Help1),r(R.string.MSPosition_qcGps_Help2));
        } else if ("center".equals(info)){
            etv.setData(prefCenter,R.drawable.center1,R.drawable.center2);
            etv.setPrAction(prefCenter);
            etv.setDisabledData(prefGps, R.drawable.center_dis);
            etv.setHelp(r(R.string.MSPosition_qcCenter_Help)).setHelp(r(R.string.MSPosition_qcCenter_Help1),r(R.string.MSPosition_qcCenter_Help2));
        } else if ("group_record".equals(info)){
            etv.setData(prefGps,R.drawable.group_record1,R.drawable.group_record2);
            etv.setPrAction(MGPref.anonymous(false));
        }
        return etv;
    }

    @Override
    protected void onResume() {
        getApplication().lastPositionsObservable.addObserver(refreshObserver);
        refreshObserver.onChange();
        prefCenter.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                refreshObserver.onChange();
            }
        });
    }

    @Override
    protected void onPause() {
        getApplication().lastPositionsObservable.deleteObserver(refreshObserver);
    }

    @Override
    protected void doRefresh() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PointModel pm = getApplication().lastPositionsObservable.lastGpsPoint;
                if (prefGps.getValue() && (pm != null)){
                    showPosition(pm);
                    centerCurrentPosition(pm);
                } else {
                    hidePosition();
                    if (pm != null){
                        centerCurrentPosition(pm);
                    }
                }
            }
        });
    }


    private void hidePosition(){
        unregisterAll();
        getControlView().setStatusLineValue(ptvHeight, PointModel.NO_ELE);
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

        getControlView().setStatusLineValue(ptvHeight, pm.getEleA());
    }

    private void centerCurrentPosition(PointModel pm){
        if ((pm != null) && prefCenter.getValue()) {
            IMapViewPosition mvp = getMapView().getModel().mapViewPosition;
            LatLong pos = new LatLong(pm.getLat(), pm.getLon());
            mvp.setMapPosition(new MapPosition(pos, mvp.getZoomLevel()));
        }
    }

}
