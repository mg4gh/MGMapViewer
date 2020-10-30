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

}
