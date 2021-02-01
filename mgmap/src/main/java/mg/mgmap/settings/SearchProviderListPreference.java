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

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import mg.mgmap.util.PersistenceManager;

public class SearchProviderListPreference extends ListPreference {

    public SearchProviderListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setSummaryProvider(SimpleSummaryProvider.getInstance());

        String[] searchCfgs = PersistenceManager.getInstance().getSearchConfigNames();
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
    }
}
