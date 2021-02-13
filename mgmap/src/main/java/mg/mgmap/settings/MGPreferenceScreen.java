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

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import mg.mgmap.MGMapApplication;
import mg.mgmap.util.NameUtil;

public abstract class MGPreferenceScreen extends PreferenceFragmentCompat {

    protected void setIntent(int resId, Intent intent){
        Preference preference = findPreference( getResources().getString(resId) );
        if (preference != null){
            preference.setIntent( intent );
            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+intent.getDataString());
                    return false;
                }
            });
        }
    }

    protected void setBrowseIntent(int resId, int uriId){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(uriId)));
        setIntent(resId, browserIntent);
    }



}
