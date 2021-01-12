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
    private static SharedPreferences sharedPreferences;

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
