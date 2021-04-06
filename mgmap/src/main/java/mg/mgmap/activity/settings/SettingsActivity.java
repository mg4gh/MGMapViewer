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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.util.FullscreenUtil;
import mg.mgmap.generic.util.HomeObserver;
import mg.mgmap.generic.util.basic.NameUtil;

public class SettingsActivity extends AppCompatActivity implements
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceFragmentCompat pfc = new MainPreferenceScreen();
        Intent intent = getIntent();
        if (intent != null) {
            String clazzname = intent.getStringExtra("FSControl.info");
            if (clazzname != null) {
                try {
                    Log.i(MGMapApplication.LABEL, NameUtil.context() + " open PreferenceFragment " + clazzname);
                    Class<?> clazz = Class.forName(clazzname);
                    Object obj = clazz.newInstance();
                    if (obj instanceof PreferenceFragmentCompat) {
                        pfc = (PreferenceFragmentCompat) obj;
                    }
                } catch (Exception e) {
                    Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
                }
            }
            super.onNewIntent(intent);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, pfc )
                .commit();
        ViewGroup qcs = findViewById(R.id.sa_qc);
        ControlView.createQuickControlETV(qcs)
                .setData(R.drawable.back)
                .setOnClickListener(createBackOCL());
        ControlView.createQuickControlETV(qcs)
                .setData(R.drawable.home)
                .setOnClickListener(createHomeOCL());
    }
    private View.OnClickListener createBackOCL(){
        return v -> SettingsActivity.this.onBackPressed();
    }
    private View.OnClickListener createHomeOCL() {
        return v -> HomeObserver.launchHomeScreen(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        boolean fullscreen = prefs.getBoolean(getResources().getString(R.string.FSControl_qcFullscreenOn), true);
        FullscreenUtil.enforceState(this, fullscreen);
    }


    // copied from:
    // https://developer.android.com/guide/topics/ui/settings/organize-your-settings#java
    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" set fragment "+pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);
        // Replace the existing Fragment with the new Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null)
                .commit();
        return true;
    }

}