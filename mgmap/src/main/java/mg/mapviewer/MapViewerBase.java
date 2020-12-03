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
package mg.mapviewer;

import android.app.Activity;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import android.util.Log;

import org.mapsforge.map.android.util.AndroidPreferences;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.MapWorkerPool;
import org.mapsforge.map.model.DisplayModel;
import org.mapsforge.map.model.common.PreferencesFacade;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.scalebar.ImperialUnitAdapter;
import org.mapsforge.map.scalebar.MetricUnitAdapter;
import org.mapsforge.map.scalebar.NauticalUnitAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mg.mapviewer.util.NameUtil;

/**
 * Base class of the MGMapActivity.
 * Covers the most handling concerning the preferences and also concerning the MapView initialization
 */
public abstract class MapViewerBase extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    protected MapView mapView;
    protected PreferencesFacade preferencesFacade;
    protected SharedPreferences sharedPreferences;
    protected List<TileCache> tileCaches = new ArrayList<TileCache>();

    @Override
    protected void onPause() {
        mapView.getModel().save(this.preferencesFacade);
        this.preferencesFacade.save();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        tileCaches.clear();
        this.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    public void addTileCache(TileCache tileCache){
        this.tileCaches.add(tileCache);
    }


    protected static final byte ZOOM_LEVEL_MIN = 1;
    protected static final byte ZOOM_LEVEL_MAX = 24;

    /** MGMapViewer use exactly one mapView object, which is initialized here */
    protected void initMapView() {
        mapView = findViewById(R.id.mapView);
        mapView.getModel().init(this.preferencesFacade);
        mapView.setClickable(true);
        mapView.getMapScaleBar().setVisible(true);
        mapView.setBuiltInZoomControls(false);
        setMapScaleBar();
    }


    protected void createSharedPreferences() {
        this.preferencesFacade = new AndroidPreferences(this.getSharedPreferences( this.getClass().getSimpleName(), MODE_PRIVATE));
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        this.sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }



    protected void setMapScaleBar() {
        String value = this.sharedPreferences.getString(getResources().getString(R.string.preferences_scalebar_key), getResources().getString(R.string.preferences_scalebar_both_key));

        if (getResources().getString(R.string.preferences_scalebar_none_key).equals(value)) {
            AndroidUtil.setMapScaleBar(this.mapView, null, null);
        } else {
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        Log.i(MGMapApplication.LABEL, NameUtil.context() + " key="+key+" value="+ preferences.getAll().get(key).toString());
        // Be aware that most preference changes take effect due to activity restart

        if (key.startsWith("SelectMap") || key.equals("SelectTheme") || key.equals("scale") || key.equals("scalebar") || key.equals("language_selection")){
            for (TileCache tileCache : tileCaches) {
                tileCache.purge();
            }
            this.recreate(); // restart activity
        }
//        if (!key.startsWith("no_recreate")){
//            for (TileCache tileCache : tileCaches) {
//                tileCache.purge();
//            }
//            this.recreate(); // restart activity
//        }
    }

}
