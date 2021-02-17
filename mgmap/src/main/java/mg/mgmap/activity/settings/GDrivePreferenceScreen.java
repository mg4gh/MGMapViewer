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

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import mg.mgmap.R;

public class GDrivePreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.gdrive_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        setGDriveOCL();
    }

    private void setGDriveOCL() {
        Preference pref = findPreference(getResources().getString(R.string.preferences_gdrive_sync_key));
        if (pref != null){
            pref.setOnPreferenceClickListener(preference -> {
                Activity activity = getActivity();
                if (activity != null){
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                    sharedPreferences.edit().putBoolean(getResources().getString(R.string.preferences_gdrive_trigger), true).apply();
                    getActivity().finish();
                }
                return true;
            });
        }
    }

}
