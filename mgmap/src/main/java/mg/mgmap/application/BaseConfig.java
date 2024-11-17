package mg.mgmap.application;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.TreeSet;

public record BaseConfig(String appDirName, String preferencesName,
                         SharedPreferences sharedPreferences,
                         mg.mgmap.application.BaseConfig.Mode mode) {

    public enum Mode {NORMAL, UNIT_TEST, INSTRUMENTATION_TEST, SYSTEM_TEST}

    @NonNull
    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("BaseConfig{" +
                "appDirName='" + appDirName + '\'' +
                ", preferencesName='" + preferencesName + '\'' +
                ", #sharedPreferences='" + sharedPreferences.getAll().size() + '\'' +
                ", mode=" + mode);
        Map<String, ?> sp = sharedPreferences.getAll();
        for (String key :new TreeSet<>( sp.keySet())){
            res.append("\n        ").append(key).append("='").append(sp.get(key)).append("'");
        }
        res.append(" }");
        return res.toString();
    }
}
