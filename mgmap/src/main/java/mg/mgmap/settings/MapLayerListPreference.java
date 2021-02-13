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
import android.util.Log;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;

import mg.mgmap.MGMapApplication;
import mg.mgmap.MGMapLayerFactory;
import mg.mgmap.util.NameUtil;

public class MapLayerListPreference extends ListPreference {

    public MapLayerListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        String[] maps = MGMapLayerFactory.getAvailableMapLayers();
        setEntries(maps);
        setEntryValues(maps);
        setDefaultValue(maps[0]);
    }

    @Override
    protected void onClick() {
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" key="+getKey()+" value="+getValue());
        super.onClick();
    }
}
