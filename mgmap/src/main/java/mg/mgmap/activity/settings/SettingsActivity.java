/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.util.FullscreenUtil;
import mg.mgmap.generic.util.basic.MGLog;

public class SettingsActivity extends AppCompatActivity implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        prefs = MGMapApplication.getByContext(this).getSharedPreferences();
        PreferenceFragmentCompat pfc = new MainPreferenceScreen();
        Intent intent = getIntent();
        if (intent != null) {
            String clazzname = intent.getStringExtra("FSControl.info");
            if (clazzname != null) {
                try {
                    mgLog.i("open PreferenceFragment " + clazzname);
                    Class<?> clazz = Class.forName(clazzname);
                    Object obj = clazz.getDeclaredConstructor().newInstance();
                    if (obj instanceof PreferenceFragmentCompat) {
                        pfc = (PreferenceFragmentCompat) obj;
                    }
                } catch (Exception e) {
                    mgLog.e(e);
                }
            }
            super.onNewIntent(intent);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, pfc )
                .commit();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        boolean fullscreen = prefs.getBoolean(getResources().getString(R.string.FSControl_qcFullscreenOn), true);
        FullscreenUtil.enforceState(this, fullscreen);
    }

    // copied from:
    // https://developer.android.com/guide/topics/ui/settings/organize-your-settings#java
    // mg4gh: removed line "fragment.setTargetFragment(caller, 0);", since it is deprecated and it seems not necessary
    @Override
    public boolean onPreferenceStartFragment(@NonNull PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                Objects.requireNonNull(pref.getFragment()));
        mgLog.i("set fragment "+pref.getFragment());
        fragment.setArguments(args);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

}