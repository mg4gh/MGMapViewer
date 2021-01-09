package mg.mgmap.settings;

import android.os.Bundle;

import mg.mgmap.R;

public class MapLayersPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.map_layers_preferences, rootKey);
    }

}
