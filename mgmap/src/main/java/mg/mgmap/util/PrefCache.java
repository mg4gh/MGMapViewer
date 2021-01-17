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
package mg.mgmap.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.TreeSet;

import mg.mgmap.MGMapApplication;

public class PrefCache implements SharedPreferences.OnSharedPreferenceChangeListener{

    private final Context context;
    private final HashMap<String, Pref<?>> prefMap;
    private final SharedPreferences sharedPreferences;

    public PrefCache(Context context){
        this.context = context;
        prefMap = new HashMap<>();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Pref<?> pref = prefMap.get(key);
        if (pref != null){
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " key="+key+" value="+ sharedPreferences.getAll().get(key).toString());
            pref.onSharedPreferenceChanged();
        }
    }

    public void cleanup(){
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        for (Pref<?> pref : prefMap.values()){
            pref.deleteObservers();
        }
    }

    public <T> Pref<T> get(int keyId, T defaultValue) {
        String key = context.getResources().getString(keyId);
        return get(key, defaultValue);
    }

    public <T> Pref<T> get(String key, T defaultValue){
        Pref<?> pref = prefMap.get(key);
        if (pref == null){
            pref = new Pref<T>(key, defaultValue, sharedPreferences);
            prefMap.put(key, pref);
        }
        return (Pref<T>)pref;
    }

    public void dumpPrefs(){
        StringBuilder sPrefs = new StringBuilder("Preferences:\n");
        TreeSet<String> keys = new TreeSet<>(prefMap.keySet());
        for (String key : keys ){
            Pref<?> pref = prefMap.get(key);
            sPrefs.append(pref.toString()).append("\n");
        }
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+sPrefs);
    }

}
