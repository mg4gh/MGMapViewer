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

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapLayerFactory;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.hints.HintMapLayerAssignment;

public class MapLayersPreferenceScreen extends MGPreferenceScreen {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(MGMapApplication.getByContext(getContext()).getPreferencesName());
        setPreferencesFromResource(R.xml.map_layers_preferences, rootKey);
        try {
            MGMapApplication application = (MGMapApplication) requireActivity().getApplication();
            PrefCache prefCache = application.getPrefCache();
            List<String> mapKeys = MGMapLayerFactory.getMapLayerKeys(getContext()).stream().map(key->prefCache.get(key,"").getValue()).collect(Collectors.toList());
            application.getHintUtil().showHint( new HintMapLayerAssignment(getActivity(), mapKeys) );
        } catch (Exception e) {
            mgLog.e(e);
        }
    }

}
