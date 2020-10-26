package mg.mapviewer.settings;

import android.os.Bundle;

import androidx.preference.Preference;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import mg.mapviewer.BuildConfig;
import mg.mapviewer.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new MainPreferenceScreen())
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

}