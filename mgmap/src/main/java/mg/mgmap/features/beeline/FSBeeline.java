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
package mg.mgmap.features.beeline;

import android.util.Log;

import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.model.LatLong;


import java.util.Observable;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.model.MultiPointModelImpl;
import mg.mgmap.model.PointModel;
import mg.mgmap.model.PointModelImpl;
import mg.mgmap.util.CC;
import mg.mgmap.util.Formatter;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.PointModelUtil;
import mg.mgmap.util.MGPref;
import mg.mgmap.view.ExtendedTextView;
import mg.mgmap.view.MultiPointView;

public class FSBeeline extends FeatureService {

    public static final Paint PAINT_BLACK_STROKE = CC.getStrokePaint(R.color.BLACK, 2);

    private final MGPref<Boolean> prefGps = MGPref.get(R.string.FSPosition_prev_GpsOn, false);
    private final MGPref<Integer> prefZoomLevel = MGPref.get(R.string.FSPosition_prev_ZoomLevel, 15);
    private ExtendedTextView etvCenter = null;
    private ExtendedTextView etvZoom = null;

    public FSBeeline(MGMapActivity mmActivity) {
        super(mmActivity);
        getMapView().getModel().mapViewPosition.addObserver(refreshObserver);
        getApplication().lastPositionsObservable.addObserver(refreshObserver);
        prefGps.addObserver(refreshObserver);
    }

    @Override
    public ExtendedTextView initStatusLine(ExtendedTextView etv, String info) {
        super.initStatusLine(etv,info);
        if (info.equals("center")){
            etv.setData(R.drawable.distance);
            etv.setFormat(Formatter.FormatType.FORMAT_DISTANCE);
            etvCenter = etv;
        }
        if (info.equals("zoom")){
            etv.setData(R.drawable.zoom);
            etv.setFormat(Formatter.FormatType.FORMAT_INT);
            etvZoom = etv;
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
    protected void onUpdate(Observable o, Object arg) {
        ttRefreshTime = 150; // avoid refresh faster than FSPosition
    }

    @Override
    protected void doRefreshResumedUI() {
        ttRefreshTime = 10;
        int zoomLevel = getMapView().getModel().mapViewPosition.getZoomLevel();
        PointModel lp = getApplication().lastPositionsObservable.lastGpsPoint;
        if (prefGps.getValue() && (lp != null)){
            showHidePositionToCenter(lp);
        } else {
            showHidePositionToCenter(null);
        }
        getControlView().setStatusLineValue(etvZoom, zoomLevel);
        prefZoomLevel.setValue(zoomLevel);
    }

    private void showHidePositionToCenter(PointModel pm){
        if (fsLayers.isEmpty() && (pm == null)) return; // default is fast
        unregisterAll();
        LatLong center = getMapView().getModel().mapViewPosition.getCenter();
        PointModel pmCenter = new PointModelImpl(center);
        boolean showNewValue = (pm != null);
        double distance = 0;
        if (showNewValue){
            distance = PointModelUtil.distance(pm, pmCenter);
            showNewValue = (distance > 10.0); //m
        }
        if (showNewValue){
            Log.v(MGMapApplication.LABEL, NameUtil.context()+" pm="+pm+" pmCenter="+pmCenter);
            MultiPointModelImpl mpm = new MultiPointModelImpl();
            mpm.addPoint(pmCenter);
            mpm.addPoint(pm);
            register( new MultiPointView(mpm, PAINT_BLACK_STROKE));
        } else {
            distance = -1;
        }
        getControlView().setStatusLineValue(etvCenter, distance);
    }

}
