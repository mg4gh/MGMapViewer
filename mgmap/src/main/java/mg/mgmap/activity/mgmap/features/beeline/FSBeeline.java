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
package mg.mgmap.activity.mgmap.features.beeline;

import android.util.Log;

import org.mapsforge.core.graphics.Paint;

import java.beans.PropertyChangeEvent;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.activity.mgmap.util.CC;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.activity.mgmap.view.MultiPointView;

public class FSBeeline extends FeatureService {

    public static final Paint PAINT_BLACK_STROKE = CC.getStrokePaint(R.color.BLACK, 2);

    private final Pref<Boolean> prefGps = getPref(R.string.FSPosition_pref_GpsOn, false);
    private final Pref<Integer> prefZoomLevel = getPref(R.string.FSBeeline_pref_ZoomLevel, 15);
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
    protected void onUpdate(PropertyChangeEvent event) {
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
        PointModel pmCenter = new PointModelImpl( getMapView().getModel().mapViewPosition.getCenter() );
        double distance = -1;
        if (pm != null){
            double beelineDistance = PointModelUtil.distance(pm, pmCenter);
            if (beelineDistance > 10.0){ //m
                distance = beelineDistance;
                if (Log.isLoggable(MGMapApplication.LABEL, Log.VERBOSE)) Log.v(MGMapApplication.LABEL, NameUtil.context()+" pm="+pm+" pmCenter="+pmCenter);
                MultiPointModelImpl mpm = new MultiPointModelImpl().addPoint(pmCenter).addPoint(pm);
                register( new MultiPointView(mpm, PAINT_BLACK_STROKE));
            }
        }
        getControlView().setStatusLineValue(etvCenter, distance);
    }

}
