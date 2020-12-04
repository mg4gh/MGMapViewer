package mg.mapviewer.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.features.gdrive.MSGDrive;

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
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                MGMapApplication application = (MGMapApplication)getActivity().getApplication(); // getActivity return Settings Activity 
                MGMapActivity mgMapActivity = application.getMgMapActivity();

                MSGDrive msGDrive = mgMapActivity.getMS(MSGDrive.class);
                msGDrive.trySynchronisation();

                Intent intent = new Intent(mgMapActivity, MGMapActivity.class);
                mgMapActivity.startActivity(intent);

                return true;
            }
        });
    }

}
