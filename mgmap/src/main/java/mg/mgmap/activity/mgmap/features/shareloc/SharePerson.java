package mg.mgmap.activity.mgmap.features.shareloc;

import mg.mgmap.R;
import mg.mgmap.generic.util.CC;
import mg.mgmap.generic.util.ObservableImpl;

public class SharePerson extends ObservableImpl {

    final public static String DUMMY_EMAIL = "email@example.com";

    String email;
    boolean shareWithActive;
    long shareWithUntil;
    boolean shareFromActive;
    long shareFromUntil;
    int color;

    String crt; // not persisted

    public SharePerson(){
        init();
    }
    public SharePerson(SharePerson person){
        email = person.email;
        shareWithActive = person.shareWithActive;
        shareWithUntil = person.shareWithUntil;
        shareFromActive = person.shareFromActive;
        shareFromUntil = person.shareFromUntil;
        color = person.color;
        crt = person.crt;
    }

    void init(){
        long now = System.currentTimeMillis();
        email = DUMMY_EMAIL;
        shareWithActive = false;
        shareWithUntil = now;
        shareFromActive = false;
        shareFromUntil = now;
        color = CC.getColor(R.color.CC_GREEN);
        crt = "";
    }

    String toPrefString(){
        return email+":"+shareWithActive+":"+shareWithUntil+":"+shareFromActive+":"+shareFromUntil+":"+color;
    }
    static SharePerson fromPrefString(String prefString){
        SharePerson person = new SharePerson();
        String[] parts = prefString.split(":");
        if (parts.length >= 6){
            person.email = parts[0];
            person.shareWithActive = Boolean.parseBoolean(parts[1]);
            person.shareWithUntil = Long.parseLong(parts[2]);
            person.shareFromActive = Boolean.parseBoolean(parts[3]);
            person.shareFromUntil = Long.parseLong(parts[4]);
            person.color = Integer.parseInt(parts[5]);
        }
        return person;
    }
}
