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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.lang.invoke.MethodHandles;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.hints.AbstractHint;

public abstract class MGPreferenceScreen extends PreferenceFragmentCompat {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    protected void setIntent(int resId, Intent intent, AbstractHint hint){
        Preference preference = findPreference( getResources().getString(resId) );
        final Activity activity = getActivity();
        if ((preference != null) && (activity != null)){
            if (hint != null){
                hint.addGotItAction(() -> activity.startActivity(intent));
            }
            preference.setOnPreferenceClickListener(preference1 -> {
                mgLog.i("onPreferenceClick key="+preference1.getKey()+" "+intent.getDataString());
                MGMapApplication application = (MGMapApplication) getActivity().getApplication();
                if (!application.getHintUtil().showHint( hint )){
                    activity.startActivity(intent);
                }
                return true;
            });
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected void setBrowseIntent(int resId, int uriId, AbstractHint hint){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(uriId)));
        setIntent(resId, browserIntent, hint);
    }

    protected void setBrowseIntent(int resId, int uriId){
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getResources().getString(uriId)));
        setIntent(resId, browserIntent, null);
    }

    protected void setPreferenceClickListener(int resId, Runnable r){
        Preference preference = findPreference(getResources().getString(resId));
        if (preference != null){
            preference.setOnPreferenceClickListener(preference1 -> {
                mgLog.i("onPreferenceClick key="+preference1.getKey());
                r.run();
                return false;
            });
        }
    }
}
