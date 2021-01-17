package mg.mgmap.settings;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import mg.mgmap.R;

public class GDrivePreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.gdrive_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        setGDriveOCL();
    }

    private void setGDriveOCL() {
        Preference pref = findPreference(getResources().getString(R.string.preferences_gdrive_sync_key));
        if (pref != null){
            pref.setOnPreferenceClickListener(preference -> {
                Activity activity = getActivity();
                if (activity != null){
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                    sharedPreferences.edit().putBoolean(getResources().getString(R.string.preferences_gdrive_trigger), true).apply();
//                    new FSGDrive(getActivity()).trySynchronisation();
                    getActivity().finish();
                }
                return true;
            });
        }
    }

}
