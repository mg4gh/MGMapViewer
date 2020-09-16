/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mapviewer.util;

import android.content.Context;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMapLayerFactory;
import mg.mapviewer.R;
import mg.mapviewer.Settings;

/**
 * Provide Dynamic List Preference for selection of a map file and selection of a theme.
 */

public class DynamicListPreference extends ListPreference {

    @SuppressWarnings( "deprecation" )
    public DynamicListPreference (Context context, AttributeSet attrs) {
        super(context, attrs);

        PersistenceManager pm = PersistenceManager.getInstance();

        String key = getKey();
        Log.i(MGMapApplication.LABEL, NameUtil.context() +" key="+key );
        if (Settings.getMapLayerKeys().contains(key)){
            String[] maps = MGMapLayerFactory.getAvailableMapLayers();
            setEntries(maps);
            setEntryValues(maps);
            setSummary(PreferenceManager.getDefaultSharedPreferences(context).getString(key,""));
        }

        if (getKey().equals( context.getResources().getString(R.string.preference_choose_theme_key) )){
            String[] themes = pm.getThemeNames();
            if (themes.length == 0) {
                themes = new String[]{ "Elevate.xml"};
            }
            setEntries(themes);
            setEntryValues(themes);
            setSummary(PreferenceManager.getDefaultSharedPreferences(context).getString(key,""));
        }

        if (getKey().equals( context.getResources().getString(R.string.preference_choose_search_key) )){
            String[] searchCfgs = pm.getSearchConfigNames();
            String[] searchProviders = new String[searchCfgs.length];
            for (int i=0; i<searchCfgs.length; i++){
                searchProviders[i] = searchCfgs[i].replaceAll(".cfg$", "");
            }
            if (searchProviders.length == 0) {
                searchProviders = new String[]{ "Nominatim" };
            }
            setEntries(searchProviders);
            setEntryValues(searchProviders);
            setSummary(PreferenceManager.getDefaultSharedPreferences(context).getString(key,""));

        }

    }

    @Override
    protected void onClick() {
        super.onClick();
        Log.i(MGMapApplication.LABEL,NameUtil.context());
    }


}
