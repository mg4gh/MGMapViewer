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
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import androidx.core.content.FileProvider;

import org.mapsforge.map.model.DisplayModel;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import mg.mapviewer.features.gdrive.MSGDrive;
import mg.mapviewer.util.BgJob;
import mg.mapviewer.util.Permissions;
import mg.mapviewer.util.PersistenceManager;
import mg.mapviewer.util.TopExceptionHandler;
import mg.mapviewer.util.Zipper;

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
                builder.setMessage(" Without permission "+Manifest.permission.WRITE_EXTERNAL_STORAGE+" "+msg);
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

        {
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
        registerOCL(R.string.preferences_dl_maps_wd_key,    R.string.url_oam_dl);
        registerOCL(R.string.preferences_dl_maps_eu_key, R.string.url_oam_dl_eu);
        registerOCL(R.string.preferences_dl_maps_de_key, R.string.url_oam_dl_de);
        registerOCL(R.string.preferences_dl_theme_el_key, R.string.url_oam_th_el);
        registerOCL(R.string.preferences_doc_main_key, R.string.url_doc_main);
        registerOCL(R.string.preferences_dl_sw_other_key, R.string.url_github_apk_dir);
        registerOCLDownloadLatest();

        Preference preference = findPreference(getResources().getString(R.string.preferences_version_key));
        preference.setSummary(BuildConfig.VERSION_NAME);

    }

    @SuppressWarnings("deprecation")
    private void registerOCL(int pref_key_id, int uri_id){
        {
            Preference pref = findPreference(getResources().getString(pref_key_id));
            pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (mgMapActivity != null){
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(uri_id)));
                        startActivity(browserIntent);
                    }
                    return true;
                }
            });
        }
    }

    @SuppressWarnings("deprecation")
    private void registerOCLDownloadLatest(){
        Preference pref = findPreference(getResources().getString(R.string.preferences_dl_sw_latest_key));
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ArrayList<BgJob> jobs = new ArrayList(){ };
                BgJob job = new BgJob() {
                    @Override
                    protected void doJob() throws Exception {
                        super.doJob();
                        Zipper zipper = new Zipper(null);
                        String urlString = getResources().getString(R.string.url_github_apk_latest)+((BuildConfig.DEBUG)?"debug":"release")+"/apk.zip";
                        URL url = new URL(urlString);
                        PersistenceManager.getInstance().cleanApkDir();
                        zipper.unpack(url, PersistenceManager.getInstance().getApkDir(), null, this);

                        File file = PersistenceManager.getInstance().getApkFile();
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
                        Context context = getApplicationContext();

                        Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
                        intent.setDataAndType(uri, "application/vnd.android.package-archive");
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                    }
                };
                jobs.add(job);
                ((MGMapApplication)getApplication()).addBgJobs(jobs);
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
