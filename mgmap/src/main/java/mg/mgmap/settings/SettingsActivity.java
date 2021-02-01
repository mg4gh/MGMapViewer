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