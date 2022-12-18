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

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import java.lang.invoke.MethodHandles;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.MGLog;

public class SearchProviderListPreference extends ListPreference {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public SearchProviderListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setSummaryProvider(SimpleSummaryProvider.getInstance());

        if (context.getApplicationContext() instanceof MGMapApplication) {
            MGMapApplication application = (MGMapApplication) context.getApplicationContext();
            String[] searchCfgs = application.getPersistenceManager().getSearchConfigNames();
            String[] searchProviders = new String[searchCfgs.length];
            for (int i=0; i<searchCfgs.length; i++){
                searchProviders[i] = searchCfgs[i].replaceAll(".cfg$", "");
            }
            if (searchProviders.length == 0) {
                searchProviders = new String[]{ "Nominatim" };
            }
            setEntries(searchProviders);
            setEntryValues(searchProviders);

            setDefaultValue("Nominatim");

            setOnPreferenceChangeListener((preference, newValue) -> {
                mgLog.i("key="+preference.getKey()+" value="+newValue);
                return true;
            });
        }
    }

    @Override
    protected void onClick() {
        mgLog.i("key="+getKey()+" value="+getValue());
        super.onClick();
    }
}
