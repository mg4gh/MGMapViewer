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

import android.content.SharedPreferences;
import android.view.View;

import java.util.Observable;
import java.util.UUID;

public class Pref<T> extends Observable implements View.OnClickListener, View.OnLongClickListener {

    protected final String key;
    protected T value;
    protected final SharedPreferences sharedPreferences;

    public Pref(T initialValue){
        this(UUID.randomUUID().toString(), initialValue, null);
    }

    public Pref(String key, T initialValue, SharedPreferences sharedPreferences){
        if (initialValue == null){
            throw new RuntimeException("null not allowed");
        }
        this.key = key;
        this.sharedPreferences = sharedPreferences;
        value = initialValue;
        if (sharedPreferences != null){
            value = getSharedPreference();
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
        if (sharedPreferences != null){
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
        setValue(t, (sharedPreferences!=null));
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
        toggle();
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
