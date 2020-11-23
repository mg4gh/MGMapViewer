package mg.mapviewer.settings;

import android.os.Bundle;

import mg.mapviewer.R;

public class FurtherPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.further_preferences, rootKey);
    }

}
