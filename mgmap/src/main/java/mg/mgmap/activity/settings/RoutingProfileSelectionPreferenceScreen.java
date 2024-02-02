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

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceCategory;

import java.util.ArrayList;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.FSRouting;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.PrefCache;

@SuppressWarnings("ConstantConditions")
public class RoutingProfileSelectionPreferenceScreen extends MGPreferenceScreen {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName(MGMapApplication.getByContext(getContext()).getPreferencesName());
        setPreferencesFromResource(R.xml.routine_profile_selection, rootKey);
    }

    @Override
    public void onResume() {
        super.onResume();

        MGMapApplication application = (MGMapApplication) requireActivity().getApplication();
        PrefCache prefCache = application.getPrefCache();

        PreferenceCategory pcRoutingProfiles = findPreference(getResources().getString(R.string.FSRouting_routing_profiles_menu_key));

        ArrayList<RoutingProfile> definedRoutingProfiles = FSRouting.getDefinedRoutingProfiles();
        if (definedRoutingProfiles != null){
            for (RoutingProfile routingProfile : definedRoutingProfiles){
                CheckBoxPreference checkbox = new CheckBoxPreference(getContext());

                String key = routingProfile.getId();
                checkbox.setKey(key);
                checkbox.setPersistent(true);
                checkbox.setTitle(routingProfile.getClass().getSimpleName());
                checkbox.setIconSpaceReserved(true);
                checkbox.setIcon(routingProfile.getIconIdActive());

                checkbox.setChecked( prefCache.get( routingProfile.getId(), false).getValue() );
                pcRoutingProfiles.addPreference(checkbox);
            }
        }
    }

 }
