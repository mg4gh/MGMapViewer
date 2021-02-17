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

import android.os.Bundle;

import androidx.preference.Preference;

import mg.mgmap.BuildConfig;
import mg.mgmap.R;

public class MainPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();
        setBrowseIntent(R.string.preferences_doc_main_key, R.string.url_doc_main);

        Preference preference = findPreference(getResources().getString(R.string.preferences_version_key));
        preference.setSummary(BuildConfig.VERSION_NAME+" ("+ (BuildConfig.DEBUG?"debug":"release")+")");
    }

}
