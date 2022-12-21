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

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import mg.mgmap.R;
import mg.mgmap.application.MGMapApplication;

public class FurtherPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(MGMapApplication.getByContext(getContext()).getPreferencesName());
        setPreferencesFromResource(R.xml.further_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        Preference preference = findPreference(getResources().getString(R.string.preferences_developer_settings_key));
        assert preference != null;
        boolean developer = MGMapApplication.getByContext(getContext()).getPrefCache().get(R.string.MGMapApplication_pref_Developer,false).getValue();
        preference.setVisible(developer);
    }


}
