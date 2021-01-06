package mg.mapviewer.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;

import androidx.preference.PreferenceManager;

import java.util.HashMap;
import java.util.Observable;
import java.util.TreeSet;
import java.util.UUID;

import mg.mapviewer.MGMapApplication;

public class MGPref<T> extends Observable implements View.OnClickListener, View.OnLongClickListener {

    private static Context context = null;
    private static SharedPreferences sharedPreferences = null;
    private static HashMap<String, MGPref<?>> prefMap = new HashMap<>();
    private static SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener(){
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(MGMapApplication.LABEL, NameUtil.context()+" "+key);
            MGPref<?> pref = prefMap.get(key);
            if (pref != null){
                pref.onSharedPreferenceChanged();
            }
        }
    };

    public static void init(Context acontext){
        context = acontext;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    public static <T> MGPref<T> anonymous(T initialValue) {
        String key = UUID.randomUUID().toString();
        return new MGPref<T>(key, initialValue, false);
    }

    public static MGPref<Boolean> bool(int keyId) {
        String key = context.getResources().getString(keyId);
        return get(key, false);
    }

    public static <T> MGPref<T> get(int keyId, T defaultValue) {
        String key = context.getResources().getString(keyId);
        return get(key, defaultValue);
    }

    public static <T> MGPref<T> get(String key, T defaultValue){
        MGPref<?> pref = prefMap.get(key);
        if (pref == null){
            pref = new MGPref<T>(key, defaultValue);
            prefMap.put(key, pref);
        }
        return (MGPref<T>)pref;
    }

    public static void dumpPrefs(){
        String sPrefs = "Preferences:\n";
        TreeSet<String> keys = new TreeSet<>(prefMap.keySet());
        for (String key : keys ){
            MGPref<?> pref = prefMap.get(key);
            sPrefs += pref.toString()+" "+pref.getSharedPreference().toString()+"\n";
        }
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+sPrefs);
    }
    public static void clear(){
        prefMap.clear();
    }




    protected String key;
    protected T value;
    protected boolean changeSharedPrefs = true;

    private MGPref(int keyId, T initialValue) {
        this(context.getResources().getString(keyId), initialValue);
    }

    public MGPref(String key, T initialValue){
        this(key, initialValue, true);
    }
    public MGPref(String key, T initialValue, boolean changeSharedPrefs){
        if (initialValue == null){
            throw new RuntimeException("null not allowed");
        }
        this.key = key;
        this.changeSharedPrefs = changeSharedPrefs;
        value = initialValue;
        if (changeSharedPrefs){
            value = getSharedPreference();
            prefMap.put(key, this);
        }
    }

    private T getSharedPreference(){
        if (value instanceof Boolean){
            Boolean res = sharedPreferences.getBoolean(key, (Boolean) value);
            return (T)res;
        } else if (value instanceof Integer){
            Integer res = sharedPreferences.getInt(key, (Integer) value);
            return (T)res;
        } else if (value instanceof Float){
            Float res = sharedPreferences.getFloat(key, (Float) value);
            return (T)res;
        } else if (value instanceof String){
            String res = sharedPreferences.getString(key, (String) value);
            return (T)res;
        } else if (value instanceof Long){
            Long res = sharedPreferences.getLong(key, (Long) value);
            return (T)res;
        } else {
            throw new RuntimeException("type not allowed: "+value.getClass().getName());
        }
    }

    private void setSharedPreference(T t){
        if (value instanceof Boolean){
            sharedPreferences.edit().putBoolean(key, (Boolean) t).apply();
        } else if (value instanceof Integer){
            sharedPreferences.edit().putInt(key, (Integer) t).apply();
        } else if (value instanceof Float){
            sharedPreferences.edit().putFloat(key, (Float) t).apply();
        } else if (value instanceof String){
            sharedPreferences.edit().putString(key, (String) t).apply();
        } else if (value instanceof Long){
            sharedPreferences.edit().putLong(key, (Long) t).apply();
        } else {
            throw new RuntimeException("type not allowed: "+value.getClass().getName());
        }
    }

    void onSharedPreferenceChanged(){
        T t = getSharedPreference();
        setValue(t, false);
    }

    public String getKey(){
        return key;
    }

    public T getValue(){
        return value;
    }

    public void setValue(T t){
        setValue(t, changeSharedPrefs);
    }

    protected void setValue(T t, boolean changeSharedPrefs){
        if (t == null){
            throw new RuntimeException("null not allowed");
        }
        if (! value.equals(t)){
            value = t;
            onChange();
        }
        if (changeSharedPrefs){
            setSharedPreference(t);
        }
    }

    public void toggle(){
        if (value instanceof Boolean){
            Boolean bNewValue = !((Boolean)value);
            setValue((T)(bNewValue));
        } else {
            throw new RuntimeException("type not allowed: "+value.getClass().getName());
        }
    }

    @Override
    public void onClick(View v) {
//        Log.d(MGMapApplication.LABEL, NameUtil.context() +" key="+key);
//        if (v.isEnabled()){
        toggle();
//        }
    }

    @Override
    public boolean onLongClick(View v) {
        toggle();
        return true;
    }

    public void onChange(){
        setChanged();
        notifyObservers();
    }

    @Override
    public String toString() {
        return "MGPref{key='" + key + "', value='" + value + "'}";
    }
}
