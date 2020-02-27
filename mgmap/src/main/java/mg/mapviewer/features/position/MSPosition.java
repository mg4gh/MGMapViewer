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
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;
import org.mapsforge.map.model.IMapViewPosition;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.model.TrackLogPoint;
import mg.mapviewer.util.CC;

public class MSPosition extends MGMicroService {

    private static final Paint PAINT_FIX2_FILL = CC.getFillPaint(R.color.RED_A50);
    private static final Paint PAINT_FIX2_STROKE = CC.getStrokePaint(R.color.RED, 5);
    private static final Paint PAINT_ACC_FILL = CC.getFillPaint(R.color.BLUE_A50);
    private static final Paint PAINT_ACC_STROKE = CC.getStrokePaint(R.color.BLUE_A150, 5);


    public MSPosition(MGMapActivity mmActivity) {
        super(mmActivity);
    }

    @Override
    protected void start() {
        getApplication().lastPositionsObservable.addObserver(refreshObserver);
    }

    @Override
    protected void stop() {
        if (getApplication().recordingTrackLogObservable.getTrackLog() == null){
            LatLong center = getMapView().getModel().mapViewPosition.getCenter();
            PointModel pmCenter = new PointModelImpl(center);
            getApplication().addTrackLogPoint(pmCenter);
        }
        getApplication().lastPositionsObservable.deleteObserver(refreshObserver);
    }

    @Override
    protected void doRefresh() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PointModel pm = getApplication().lastPositionsObservable.lastGpsPoint;
                if (getApplication().gpsOn.getValue() && (pm != null)){
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
        getControlView().updateTvHeight(PointModel.NO_ELE);
    }

    private void showPosition(PointModel pm) {
        hidePosition();

        LatLong pos = new LatLong(pm.getLat(), pm.getLon());
        if (pm instanceof TrackLogPoint) {
            TrackLogPoint trackLogPoint = (TrackLogPoint) pm;
            Layer accuracyCircle = new Circle(pos, (float) trackLogPoint.getAccuracy() , PAINT_ACC_FILL, PAINT_ACC_STROKE);
            register(accuracyCircle);

        }

        float circleSize = 6;
        Layer locationCircle = new FixedPixelCircle(pos, circleSize, PAINT_FIX2_FILL, PAINT_FIX2_STROKE);
        register(locationCircle);

        getControlView().updateTvHeight(pm.getEleA());
    }

    private void centerCurrentPosition(PointModel pm){
        if ((pm != null) && getApplication().centerCurrentPosition.getValue()) {
            IMapViewPosition mvp = getMapView().getModel().mapViewPosition;
            LatLong pos = new LatLong(pm.getLat(), pm.getLon());
            mvp.setMapPosition(new MapPosition(pos, mvp.getZoomLevel()));
        }
    }

}
