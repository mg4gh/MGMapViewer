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

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import mg.mgmap.BuildConfig;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapLayerFactory;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.hints.HintMapLayerAssignment;

public class MainPreferenceScreen extends MGPreferenceScreen {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(MGMapApplication.getByContext(getContext()).getPreferencesName());
        setPreferencesFromResource(R.xml.main_preferences, rootKey);

        try {
            MGMapApplication application = (MGMapApplication) requireActivity().getApplication();
            PrefCache prefCache = application.getPrefCache();
            List<String> mapKeys = MGMapLayerFactory.getMapLayerKeys(getContext()).stream().map(key->prefCache.get(key,"").getValue()).collect(Collectors.toList());
            application.getHintUtil().showHint( new HintMapLayerAssignment(getActivity(), mapKeys) );
        } catch (Exception e) {
            mgLog.e(e);
        }

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onResume() {
        super.onResume();
        setBrowseIntent(R.string.preferences_doc_main_key, R.string.url_doc_main);

        Preference prefHeadline = findPreference(getResources().getString(R.string.preferences_headline_version_key));
        assert prefHeadline != null;
        prefHeadline.setTitle("MGMapViewer "+ BuildConfig.VERSION_NAME);

        Preference prefVersion = findPreference(getResources().getString(R.string.preferences_info_version_key));
        assert prefVersion != null;
        InfoPreferenceScreen.setBuildNumberSummary(prefVersion);

        boolean developer = MGMapApplication.getByContext(getContext()).getPrefCache().get(R.string.MGMapApplication_pref_Developer,false).getValue();
        int[] develeperPrefIds = new int[]{R.string.FSSearch_pref_SearchDetails_key,R.string.preferences_alarm_ps_key,R.string.FSGrad_pref_WayDetails_key};
        for (int prefId : develeperPrefIds){
            Preference preference = findPreference(getResources().getString(prefId));
            if (preference != null){
                preference.setVisible(developer);
            }
        }
    }

}
