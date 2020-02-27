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
package mg.mapviewer.features.motion;

import android.util.Log;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.LatLongUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.overlay.Circle;
import org.mapsforge.map.layer.overlay.FixedPixelCircle;
import org.mapsforge.map.layer.overlay.Polyline;


import java.util.Observable;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.model.MultiPointModel;
import mg.mapviewer.model.MultiPointModelImpl;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.model.TrackLogPoint;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PointModelUtil;
import mg.mapviewer.view.MultiPointView;

public class MSMotion extends MGMicroService {

    public static final Paint PAINT_BLACK_STROKE = CC.getStrokePaint(R.color.BLACK, 2);

    public MSMotion(MGMapActivity mmActivity) {
        super(mmActivity);
    }

    @Override
    protected void start() {
        getMapView().getModel().mapViewPosition.addObserver(refreshObserver);
        getApplication().lastPositionsObservable.addObserver(refreshObserver);

    }



    @Override
    @SuppressWarnings("EmptyCatchBlock")
    protected void stop() {
        getMapView().getModel().mapViewPosition.removeObserver(refreshObserver);
        getApplication().lastPositionsObservable.deleteObserver(refreshObserver);
    }

    @Override
    protected void onUpdate(Observable o, Object arg) {
        ttRefreshTime = 150; // avoid refresh faster than MSPosition
    }

    @Override
    protected void doRefresh() {
        ttRefreshTime = 10;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PointModel lp = getApplication().lastPositionsObservable.lastGpsPoint;
                if (getApplication().gpsOn.getValue() && (lp != null)){
                    showHidePositionToCenter(lp);
                } else {
                    showHidePositionToCenter(null);
                }
                getControlView().updateTvZoom(getMapView().getModel().mapViewPosition.getZoomLevel());
            }
        });
    }

    private void showHidePositionToCenter(PointModel pm){
        if (msLayers.isEmpty() && (pm == null)) return; // default is fast
        unregisterAll();
//        getMapViewUtility().hideLayers(msLayers);
        LatLong center = getMapView().getModel().mapViewPosition.getCenter();
        PointModel pmCenter = new PointModelImpl(center);
        boolean showNewValue = (pm != null);
        double distance = 0;
        if (showNewValue){
            distance = PointModelUtil.distance(pm, pmCenter);
            showNewValue &= (distance > 10.0); //m
        }
        if (showNewValue){
            Log.v(MGMapApplication.LABEL, NameUtil.context()+" pm="+pm+" pmCenter="+pmCenter);
            MultiPointModelImpl mpm = new MultiPointModelImpl();
            mpm.addPoint(pmCenter);
            mpm.addPoint(pm);
            register( new MultiPointView(mpm, PAINT_BLACK_STROKE));
        } else {
            distance = 0;
        }
        getControlView().updateTvCenter(distance);
    }

}
