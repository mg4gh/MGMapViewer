package mg.mgmap.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import mg.mgmap.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        PreferenceFragmentCompat pfc = new MainPreferenceScreen();
        Intent intent = getIntent();
        if (intent != null){
            String clazzname = intent.getStringExtra("FSControl.info");
            if (clazzname != null){
                try {
                    Class<?> clazz = Class.forName(clazzname);
                    Object obj = clazz.newInstance();
                    if (obj instanceof PreferenceFragmentCompat) {
                        pfc = (PreferenceFragmentCompat) obj;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            super.onNewIntent(intent);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, pfc )
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

}