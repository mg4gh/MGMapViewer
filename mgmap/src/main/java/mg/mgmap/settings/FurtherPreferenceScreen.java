package mg.mgmap.settings;

import android.os.Bundle;

import mg.mgmap.R;

public class FurtherPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.further_preferences, rootKey);
    }

}
