package mg.mapviewer.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.R;
import mg.mapviewer.features.gdrive.MSGDrive;

public class FurtherPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.further_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();

//        setBrowseIntent(R.string.preferences_dl_maps_wd_key, R.string.url_oam_dl);
//        setBrowseIntent(R.string.preferences_dl_maps_eu_key, R.string.url_oam_dl_eu);
//        setBrowseIntent(R.string.preferences_dl_maps_de_key, R.string.url_oam_dl_de);
//
//        setBrowseIntent(R.string.preferences_dl_theme_el_key, R.string.url_oam_th_el);
//
//        setBrowseIntent(R.string.preferences_dl_sw_other_key, R.string.url_github_apk_dir);
        setGDriveOCL();
    }

    private void setGDriveOCL() {
        Preference pref = findPreference(getResources().getString(R.string.preferences_gdrive_sync_key));
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Activity activity = getActivity();
                if (activity instanceof MGMapActivity) {
                    MGMapActivity mgMapActivity = (MGMapActivity) activity;

                    MSGDrive msGDrive = mgMapActivity.getMS(MSGDrive.class);
                    msGDrive.trySynchronisation();

                    Intent intent = new Intent(mgMapActivity, MGMapActivity.class);
                    mgMapActivity.startActivity(intent);
                }
                return true;
            }
        });
    }

}
