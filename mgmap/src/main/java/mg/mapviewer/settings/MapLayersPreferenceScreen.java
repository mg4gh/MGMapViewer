package mg.mapviewer.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapLayerFactory;
import mg.mapviewer.R;

public class MapLayersPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.map_layers_preferences, rootKey);
    }

}
