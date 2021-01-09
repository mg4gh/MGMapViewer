package mg.mgmap.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.features.gdrive.FSGDrive;

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
                MGMapApplication application = (MGMapApplication)getActivity().getApplication(); // getActivity() returns Settings Activity, not MGMapActivity
                MGMapActivity mgMapActivity = application.getMgMapActivity();

                FSGDrive msGDrive = application.getFS(FSGDrive.class);
                msGDrive.trySynchronisation();

                Intent intent = new Intent(mgMapActivity, MGMapActivity.class);
                mgMapActivity.startActivity(intent);

                return true;
            }
        });
    }

}
