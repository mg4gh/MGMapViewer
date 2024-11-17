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

import android.os.Build;
import android.os.Bundle;
import android.text.InputType;

import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Collectors;

import mg.mgmap.BuildConfig;
import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapLayerFactory;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.hints.HintMapLayerAssignment;

public class MainPreferenceScreen extends MGPreferenceScreen {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private PrefCache prefCache;

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(MGMapApplication.getByContext(getContext()).getPreferencesName());
        setPreferencesFromResource(R.xml.main_preferences, rootKey);

        try {
            MGMapApplication application = (MGMapApplication) requireActivity().getApplication();
            prefCache = application.getPrefCache();
            List<String> mapKeys = MGMapLayerFactory.getMapLayerKeys(getContext()).stream().map(key->prefCache.get(key,"").getValue()).collect(Collectors.toList());
            application.getHintUtil().showHint( new HintMapLayerAssignment(getActivity(), mapKeys) );
        } catch (Exception e) {
            mgLog.e(e);
        }

        setEditTextPreferenceNumeric(R.string.preferences_display_fullscreen_offset_key);
        setEditTextPreferenceNumeric(R.string.preferences_pressure_smoothing_gl_key);
        setEditTextPreferenceNumeric(R.string.preferences_height_consistency_check_key);
        setEditTextPreferenceNumeric(R.string.FSControl_pref_menu_animation_timeout_key);
    }

    final EditTextPreference.OnBindEditTextListener etNumberFormatter = editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER);

    private void setEditTextPreferenceNumeric(int rid){
        EditTextPreference pref = findPreference(getResources().getString(rid));
        if (pref != null){
            pref.setOnBindEditTextListener(etNumberFormatter);
        }
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onResume() {
        super.onResume();

        Preference prefHeadline = findPreference(getResources().getString(R.string.preferences_headline_version_key));
        assert prefHeadline != null;
        prefHeadline.setTitle("MGMapViewer "+ BuildConfig.VERSION_NAME);
        setBrowseIntent(R.string.preferences_headline_version_key, R.string.url_doc_main);

        EditTextPreference prefDisplayFullscreenOffset = findPreference(getResources().getString(R.string.preferences_display_fullscreen_offset_key));
        prefDisplayFullscreenOffset.setVisible(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R);

        Preference prefVersion = findPreference(getResources().getString(R.string.preferences_info_version_key));
        assert prefVersion != null;
        InfoPreferenceScreen.setBuildNumberSummary(prefVersion);

        boolean developer = MGMapApplication.getByContext(getContext()).getPrefCache().get(R.string.MGMapApplication_pref_Developer,false).getValue();
        int[] developerPrefIds = new int[]{
                R.string.FSSearch_pref_SearchDetails_key,
                R.string.preferences_alarm_ps_key,
                R.string.FSGrad_pref_WayDetails_key,
                R.string.FSRouting_routing_category_key,
                R.string.preferences_gnss_locationBuilder_key,
                R.string.preferences_gnss_minMeter_key,
                R.string.preferences_gnss_minMillis_key
        };
        for (int prefId : developerPrefIds){
            Preference preference = findPreference(getResources().getString(prefId));
            if (preference != null){
                preference.setVisible(developer);
            }
        }

        Pref<Boolean> prefUseRoutingProfiles = prefCache.get(R.string.preferences_routingProfile_key, false);
        Preference pRoutingProfiles = findPreference(getResources().getString(R.string.FSRouting_routing_profiles_menu_key));
        prefUseRoutingProfiles.addObserver((Observer) evt -> pRoutingProfiles.setEnabled(prefUseRoutingProfiles.getValue()) );
        prefUseRoutingProfiles.onChange();
    }

}
