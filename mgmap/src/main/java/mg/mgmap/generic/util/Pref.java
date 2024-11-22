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
package mg.mgmap.generic.util;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Objects;
import java.util.UUID;

import mg.mgmap.generic.util.basic.Formatter;

public class Pref<T> extends ObservableImpl  {

    protected final String key;
    protected T value;
    protected final SharedPreferences sharedPreferences;

    public Pref(T initialValue){
        this(UUID.randomUUID().toString(), initialValue, null);
    }

    public Pref(String key, T initialValue, SharedPreferences sharedPreferences){
        super(key);
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

    @SuppressWarnings({"unchecked", "WrapperTypeMayBePrimitive"})
    private T getSharedPreference(){
        if (!sharedPreferences.contains(key)){
            setSharedPreference(value);
        }
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
        } else if (value instanceof Calendar){
            Calendar res = Calendar.getInstance();
            res.setTimeInMillis( sharedPreferences.getLong(key, ((Calendar) value).getTimeInMillis() ));
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
            } else if (value instanceof Calendar){
                sharedPreferences.edit().putLong(key, ((Calendar) t).getTimeInMillis() ).apply();
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
        if (changeSharedPrefs){
            setSharedPreference(t);
        }
        {
            if (! value.equals(t)){
                value = t;
                onChange();
            }
        }
    }

    @SuppressWarnings({"unchecked", "WrapperTypeMayBePrimitive"})
    public void toggle(){
        if (value instanceof Boolean){
            Boolean bNewValue = !((Boolean)value);
            setValue((T)(bNewValue));
        } else {
            throw new RuntimeException("type not allowed: "+value.getClass().getName());
        }
    }

    public void onChange(){
        setChanged();
        notifyObservers();
    }

    @SuppressWarnings({"unchecked", "WrapperTypeMayBePrimitive, StringEqualsEmptyString"})
    public T verify(String v){
        try {
            if (v.contains("\n")) throw new ParseException(v, v.indexOf("\n"));
            if (value instanceof Boolean){
                Boolean res = Boolean.parseBoolean(v);
                return (T)res;
            } else if (value instanceof Integer){
                Integer res = ("".equals(v))?0:Integer.parseInt(v);
                return (T)res;
            } else if (value instanceof Float){
                Float res = ("".equals(v))?0:Float.parseFloat(v);
                return (T)res;
            } else if (value instanceof String){
                return (T) v;
            } else if (value instanceof Long){
                Long res = ("".equals(v))?0:Long.parseLong(v);
                return (T)res;
            } else if (value instanceof Calendar){
                Calendar res = Calendar.getInstance();
                res.setTime(Objects.requireNonNull(Formatter.SDF1a.parse(v)));
                return (T)res;
            } else {
                throw new RuntimeException("type not allowed: "+value.getClass().getName());
            }
        } catch (NumberFormatException | ParseException e) {
            return null;
        }
    }

    public void setStringValue(String v){
        T t = verify(v);
        if (t != null){
            setValue(t);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "MGPref{key='" + key + "', value='" + value + "'}";
    }
}
