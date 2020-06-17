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

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.model.DisplayModel;

import java.util.ArrayList;

import mg.mapviewer.features.gdrive.MSGDrive;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.Permissions;
import mg.mapviewer.util.TopExceptionHandler;

/**
 * Activity to edit the application preferences.
 */
public class Settings extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    private static ArrayList<String> mapLayerKeys = new ArrayList<>();
    private static MGMapActivity mgMapActivity = null;

    public static void initMapLayers(Context context, MGMapActivity activity){
        mapLayerKeys.clear();
        int[] prefIds = new int[]{
                R.string.preference_choose_map_key1,
                R.string.preference_choose_map_key2,
                R.string.preference_choose_map_key3,
                R.string.preference_choose_map_key4,
                R.string.preference_choose_map_key5};

        for (int id : prefIds){
            mapLayerKeys.add( context.getResources().getString( id ));
        }
        mgMapActivity = activity;
    }
    public static ArrayList<String> getMapLayerKeys() {
        return mapLayerKeys;
    }

    SharedPreferences prefs;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences,
                                          String key) {
        if (getResources().getString(R.string.preferences_scale_key).equals(key)) {
            float userScaleFactor = DisplayModel.getDefaultUserScaleFactor();
            float fs = Float.valueOf(preferences.getString(
                    getResources().getString(R.string.preferences_scale_key),
                    Float.toString(userScaleFactor)));
            if (fs != userScaleFactor) {
                DisplayModel.setDefaultUserScaleFactor(fs);
            }
        } else if (getMapLayerKeys().contains(key)) {
            String pref = preferences.getString(key, null);
            Preference preference = findPreference(key);
            preference.setSummary(pref);
            if (pref.endsWith(".ref")){
                checkRequestStoragePermissions(WRITE_EXTERNAL_STORAGE_CODE2);
            }
        } else if (getResources().getString(R.string.preference_choose_theme_key).equals(key)) {
            String pref = preferences.getString(key, null);
            Preference preference = findPreference(key);
            preference.setSummary(pref);
        } else if (getResources().getString(R.string.preference_choose_search_key).equals(key)) {
            String pref = preferences.getString(key, null);
            Preference preference = findPreference(key);
            preference.setSummary(pref);
        }
    }

    private boolean checkRequestStoragePermissions(int requestCode){
        if (!Permissions.check(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ) {
            Permissions.request(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE} , requestCode);
            return true;
        }
        return false;
    }
    private static final int WRITE_EXTERNAL_STORAGE_CODE2 = 992; // just an id to identify the callback
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == WRITE_EXTERNAL_STORAGE_CODE2){
            if (Permissions.check(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                Permissions.doRestart(this);
            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Warning");
                String msg = "the app will not work";
                if (requestCode == WRITE_EXTERNAL_STORAGE_CODE2) msg = "the selected link can't be used.";
                builder.setMessage("Without permission "+Manifest.permission.WRITE_EXTERNAL_STORAGE+" "+msg);
                builder.show();
            }
        }
    }



    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler());

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        addPreferencesFromResource(R.xml.preferences);

        Preference pref = findPreference(getResources().getString(R.string.preferences_gdrive_sync_key));
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (mgMapActivity != null){
                    MSGDrive msGDrive = mgMapActivity.getMS(MSGDrive.class);
                    msGDrive.trySynchronisation();

                    Intent intent = new Intent(mgMapActivity, MGMapActivity.class);
                    mgMapActivity.startActivity(intent);
                }
                return true;
            }
        });

    }

    @Override
    protected void onPause() {
        super.onPause();
        this.prefs.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.prefs.registerOnSharedPreferenceChangeListener(this);
    }




}
