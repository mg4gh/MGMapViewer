package mg.mgmap.generic.util;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import mg.mgmap.application.MGMapApplication;

public class PrefUtil {

    private Application application = null;
    private String base = null;

    public PrefUtil(String base, Application application){
        this.application = application;
        this.base = base;
    }

    public SharedPreferences getSharedPreferences(){
        return application.getSharedPreferences(application.getPackageName() + "_preferences" +base, Context.MODE_PRIVATE);
    }
    public SharedPreferences getSharedPreferences(String name){
        return application.getSharedPreferences(name + "_preferences" +base, Context.MODE_PRIVATE);
    }

    public static SharedPreferences getSharedPreferences(Context context){
        if (context != null){
            Context applicationContext = context.getApplicationContext();
            if (applicationContext instanceof MGMapApplication) {
                MGMapApplication mgMapApplication = (MGMapApplication) applicationContext;
                return mgMapApplication.getPrefUtil().getSharedPreferences();
            }
        }
        return null;
    }
}
