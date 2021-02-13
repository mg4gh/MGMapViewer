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

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import mg.mgmap.R;

public class FurtherPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.further_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        setTestTriggerOCL();
    }

    private void setTestTriggerOCL() {
        Preference prefSet =  findPreference(getResources().getString(R.string.preference_testSet_key));
        if (prefSet != null){
            prefSet.setSummaryProvider(EditTextPreference.SimpleSummaryProvider.getInstance());
        }

        Preference pref = findPreference(getResources().getString(R.string.preference_testTrigger_key));
        if (pref != null){
            pref.setOnPreferenceClickListener(preference -> {
                Activity activity = getActivity();
                if (activity != null){
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
                    boolean triggerValue = sharedPreferences.getBoolean(getResources().getString(R.string.preference_testTrigger_key), true);
                    sharedPreferences.edit().putBoolean(getResources().getString(R.string.preference_testTrigger_key), !triggerValue).apply();
                    getActivity().finish();
                }
                return true;
            });
        }
    }

}
