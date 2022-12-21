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

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import mg.mgmap.BuildConfig;
import mg.mgmap.R;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.Pref;

@SuppressWarnings("ConstantConditions")
public class InfoPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(MGMapApplication.getByContext(getContext()).getPreferencesName());
        setPreferencesFromResource(R.xml.info_preferences, rootKey);
    }

    private int devCount = 0;

    @Override
    public void onResume() {
        super.onResume();

        Preference preference = findPreference(getResources().getString(R.string.preferences_build_number_key));
        assert preference != null;
        setBuildNumberSummary(preference);

        devCount = 0;

        preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(@NonNull Preference preference) {
                devCount++;
                if ((devCount % 7) == 0){
                    MGMapApplication.getByContext(getContext()).getPrefCache().get(R.string.MGMapApplication_pref_Developer,false).toggle();
                    setBuildNumberSummary(preference);
                }
                return false;
            }
        });
    }

    private void setBuildNumberSummary(Preference preference){
        boolean developer = MGMapApplication.getByContext(getContext()).getPrefCache().get(R.string.MGMapApplication_pref_Developer,false).getValue();
        preference.setSummary(BuildConfig.VERSION_NAME+" ("+ (BuildConfig.DEBUG?"debug":"release")+")"+" "+(developer?"(Developer)":""));
    }
}
