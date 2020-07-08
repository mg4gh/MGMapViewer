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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.preference.PreferenceManager;
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
public abstract class MapViewerBase extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {

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

    protected void createControls() {
        setMapScaleBar();
        mapView.getModel().displayModel.setMaxTextWidthFactor(Float.valueOf(sharedPreferences.getString(getResources().getString(R.string.preferences_textwidth_key), "0.7")));
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
    }


    protected void createSharedPreferences() {
        this.preferencesFacade = new AndroidPreferences(this.getSharedPreferences( this.getClass().getSimpleName(), MODE_PRIVATE));
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        this.sharedPreferences.edit().clear();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
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
        MGMapApplication application = (MGMapApplication)getApplication();

        if (getResources().getString(R.string.preferences_scale_key).equals(key)) {
            this.mapView.getModel().displayModel.setUserScaleFactor(DisplayModel.getDefaultUserScaleFactor());
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" Tilesize now " + this.mapView.getModel().displayModel.getTileSize());
        }
        if (getResources().getString(R.string.preferences_language_key).equals(key)) {
            String language = preferences.getString(getResources().getString(R.string.preferences_language_key), null);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" Preferred language now " + language);
            // MG (04.08.17) : seems zo have no effect, since there is no action, 4 lines added
            Resources resources = getResources();
            Configuration configuration = resources.getConfiguration();
            configuration.setLocale(new Locale("en"));
            getApplicationContext().createConfigurationContext(configuration);
        }
        if (getResources().getString(R.string.preferences_textwidth_key).equals(key)) {
            // will take effect due to activity restart
        }
        if (getResources().getString(R.string.preferences_scalebar_key).equals(key)) {
            setMapScaleBar();
        }
        if (getResources().getString(R.string.preferences_debug_timing_key).equals(key)) {
            MapWorkerPool.DEBUG_TIMING = preferences.getBoolean(getResources().getString(R.string.preferences_debug_timing_key), false);
        }
        if (getResources().getString(R.string.preferences_way_details_key).equals(key)) {
            application.wayDetails.setValue( preferences.getBoolean(getResources().getString(R.string.preferences_way_details_key), false) );
        }
        if (getResources().getString(R.string.preferences_stl_gl_key).equals(key)) {
            application.stlWithGL.setValue( preferences.getBoolean(getResources().getString(R.string.preferences_stl_gl_key), true) );
        }
        if (getResources().getString(R.string.preferences_rendering_threads_key).equals(key)) {
//            MapWorkerPool.NUMBER_OF_THREADS = Integer.parseInt(preferences.getString(MGMapApplication.SETTING_RENDERING_THREADS, Integer.toString(MapWorkerPool.DEFAULT_NUMBER_OF_THREADS)));
        }
        if (getResources().getString(R.string.preferences_wayfiltering_distance_key).equals(key) ||
                getResources().getString(R.string.preferences_wayfiltering_key).equals(key)) {
            MapFile.wayFilterEnabled = preferences.getBoolean(getResources().getString(R.string.preferences_wayfiltering_key), true);
            if (MapFile.wayFilterEnabled) {
                MapFile.wayFilterDistance = Integer.parseInt(preferences.getString(getResources().getString(R.string.preferences_wayfiltering_distance_key), "20"));
            }
        }
        for (TileCache tileCache : tileCaches) {
            tileCache.purge();
        }
        AndroidUtil.restartActivity(this);
    }



}
