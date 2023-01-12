package mg.mgmap.application;

import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.util.Map;

public class BaseConfig {

    public enum Mode { NORMAL, UNIT_TEST, SYSTEM_TEST }

    private final String appDirName;
    private final String preferencesName;
    private final SharedPreferences sharedPreferences;
    private final Mode mode;

    public BaseConfig(String appDirName, String preferencesName, SharedPreferences sharedPreferences, Mode mode) {
        this.appDirName = appDirName;
        this.preferencesName = preferencesName;
        this.sharedPreferences = sharedPreferences;
        this.mode = mode;
    }

    public String getAppDirName() {
        return appDirName;
    }

    public String getPreferencesName() {
        return preferencesName;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public Mode getMode() {
        return mode;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("BaseConfig{" +
                "appDirName='" + appDirName + '\'' +
                ", preferencesName='" + preferencesName + '\'' +
                ", #sharedPreferences='" + sharedPreferences.getAll().size() + '\'' +
                ", mode=" + mode);
        for (Map.Entry<String, ?> entry :sharedPreferences.getAll().entrySet()){
            res.append("\n        ").append(entry.getKey()).append("='").append(entry.getValue()).append("'");
        }
        res.append(" }");
        return res.toString();
    }
}
