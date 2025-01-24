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
package mg.mgmap.activity.mgmap;

import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.MapViewPosition;
import org.mapsforge.map.scalebar.ImperialUnitAdapter;
import org.mapsforge.map.scalebar.MetricUnitAdapter;
import org.mapsforge.map.scalebar.NauticalUnitAdapter;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import mg.mgmap.activity.mgmap.util.MapViewUtility;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.CC;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.util.basic.MGLog;

/**
 * Base class of the MGMapActivity.
 * Covers the most handling concerning the preferences and also concerning the MapView initialization
 */
public abstract class MapViewerBase extends AppCompatActivity{

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static final int TILE_SIZE = 256;

    protected MapView mapView;
    protected MapViewUtility mapViewUtility;
    protected SharedPreferences sharedPreferences;
    protected final List<TileCache> tileCaches = new ArrayList<>();

    @Override
    protected void onPause() {
        saveMapViewModel();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        tileCaches.clear();
        super.onDestroy();
    }

    public void addTileCache(TileCache tileCache){
        this.tileCaches.add(tileCache);
    }

    /** MGMapViewer use exactly one mapView object, which is initialized here */
    protected void initMapView() {
        mapView = findViewById(R.id.mapView);
        mapViewUtility = new MapViewUtility(this, mapView);
        restoreMapViewModel();
        mapView.setClickable(true);
        mapView.getMapScaleBar().setVisible(true);
        mapView.setBuiltInZoomControls(false);
        setMapScaleBar();
        mapView.getModel().displayModel.setBackgroundColor(CC.getColor(R.color.CC_GRAY240));
    }

    protected void saveMapViewModel(){
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("Mapsforge.position", mapViewUtility.getCenter().getLaLo());
        editor.putInt("Mapsforge.zoom", mapViewUtility.getZoomLevel());
        editor.apply();
    }

    private void restoreMapViewModel(){
        PointModel pmDefaultCenter = new PointModelImpl(49.4057, 8.6789);
        PointModel pmCenter = PointModelImpl.createFromLaLo(sharedPreferences.getLong("Mapsforge.position",pmDefaultCenter.getLaLo()));
        mapViewUtility.setCenter(pmCenter);
        byte zoomLevel = (byte) sharedPreferences.getInt("Mapsforge.zoom", 5);
        mapViewUtility.setZoomLevel(zoomLevel);
        MapViewPosition mvp = mapView.getModel().mapViewPosition;
        mgLog.d("initial Position: "+mvp.getMapPosition());
        mvp.setZoomLevelMax(MapViewUtility.ZOOM_LEVEL_MAX);
        mvp.setZoomLevelMin(MapViewUtility.ZOOM_LEVEL_MIN);
    }


    protected void createSharedPreferences() {
        this.sharedPreferences = MGMapApplication.getByContext(this).getSharedPreferences();
    }

    protected float getUserScaleFactor(){
        float fs = 1;
        try {
            fs = Float.parseFloat(sharedPreferences.getString(getResources().getString(R.string.preferences_scale_key), Float.toString(DisplayModel.getDefaultUserScaleFactor())));
        } catch (Exception e) {
            mgLog.e(e.getMessage());
        }
        return fs;
    }

    protected void setMapScaleBar() {
        String value = this.sharedPreferences.getString(getResources().getString(R.string.preferences_scalebar_key), getResources().getString(R.string.preferences_scalebar_metric_key));

        mapView.setMapScaleBar(null);
        if (getResources().getString(R.string.preferences_scalebar_both_key).equals(value)) {
            AndroidUtil.setMapScaleBar(this.mapView, MetricUnitAdapter.INSTANCE, ImperialUnitAdapter.INSTANCE);
        } else if (getResources().getString(R.string.preferences_scalebar_metric_key).equals(value)) {
            AndroidUtil.setMapScaleBar(this.mapView, MetricUnitAdapter.INSTANCE, null);
        } else if (getResources().getString(R.string.preferences_scalebar_imperial_key).equals(value)) {
            AndroidUtil.setMapScaleBar(this.mapView, ImperialUnitAdapter.INSTANCE, null);
        } else if (getResources().getString(R.string.preferences_scalebar_nautical_key).equals(value)) {
            AndroidUtil.setMapScaleBar(this.mapView, NauticalUnitAdapter.INSTANCE, null);
        }
    }


}
