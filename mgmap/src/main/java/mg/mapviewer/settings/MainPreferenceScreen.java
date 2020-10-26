package mg.mapviewer.settings;

import android.os.Bundle;

import androidx.preference.Preference;

import mg.mapviewer.BuildConfig;
import mg.mapviewer.R;

public class MainPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        setBrowseIntent(R.string.preferences_doc_main_key, R.string.url_doc_main);

        Preference preference = findPreference(getResources().getString(R.string.preferences_version_key));
        preference.setSummary(BuildConfig.VERSION_NAME+" ("+ (BuildConfig.DEBUG?"debug":"release")+")");
    }

}
