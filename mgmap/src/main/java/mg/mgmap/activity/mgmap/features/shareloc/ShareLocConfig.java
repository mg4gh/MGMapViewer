package mg.mgmap.activity.mgmap.features.shareloc;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.ObservableImpl;

public class ShareLocConfig extends ObservableImpl {

    private static final String PREFS_NAME = "ShareLocationPrefs";
    private static final String KEY_SHARE_PERSONS = "key_share_persons";

    Context context;

    ArrayList<SharePerson> persons = new ArrayList<>();


    public ShareLocConfig(MGMapApplication application){
        context = application;
    }

    public ShareLocConfig(ShareLocConfig config){ // create a deep copy
        context = config.context;
        persons = new ArrayList<>();
        for (SharePerson person : config.persons){
            persons.add(new SharePerson(person));
        }
    }

    void loadConfig(SharePerson me) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> sharePersonsSet = prefs.getStringSet(KEY_SHARE_PERSONS, new HashSet<>());
        persons.clear();
        for (String entry : sharePersonsSet) {
            SharePerson person = SharePerson.fromPrefString(entry);
            persons.add(person);
        }
        if (me != null){
            MqttUtil.updateCertificate(context, me, persons);
        }
    }

    void saveConfig() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Set<String> sharePersonsSet = new HashSet<>();
        for (SharePerson person : persons){
            sharePersonsSet.add(person.toPrefString());
        }
        editor.putStringSet(KEY_SHARE_PERSONS, sharePersonsSet);
        editor.apply();
    }

    boolean isShareWithActive(){
        long now = System.currentTimeMillis();
        for (SharePerson person : persons) {
            if (person.shareWithActive && (person.shareWithUntil > now)) {
                return true;
            }
        }
        return false;
    }

    boolean isShareFromActive(){
        long now = System.currentTimeMillis();
        for (SharePerson person : persons) {
            if (person.shareFromActive && (person.shareFromUntil > now)) {
                return true;
            }
        }
        return false;
    }

}
