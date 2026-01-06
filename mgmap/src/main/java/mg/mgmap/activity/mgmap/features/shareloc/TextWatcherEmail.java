package mg.mgmap.activity.mgmap.features.shareloc;

import android.text.Editable;
import android.text.TextWatcher;

import java.lang.invoke.MethodHandles;
import java.util.regex.Pattern;

import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;

public class TextWatcherEmail implements TextWatcher {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    static final Pattern PATTERN_EMAIL = Pattern.compile("^[a-zA-Z0-9]+([.\\-][a-zA-Z0-9]+)*@[a-zA-Z0-9]+([.\\-][a-zA-Z0-9]+)*[.][a-zA-Z0-9]+");
    static final Pattern PATTERN_EMAIL_RAW = Pattern.compile("[a-zA-Z0-9.\\-@]*");
    static final Pattern PATTERN_EMAIL_RAW_NEG = Pattern.compile("[^a-zA-Z0-9.\\-@]+");

    private final Pref<Boolean> enablePref;

    public TextWatcherEmail(Pref<Boolean> enablePref){
        this.enablePref = enablePref;
    }
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (!PATTERN_EMAIL_RAW.matcher(s).matches()){
            String before = s.toString();
            s.replace(0,s.length(), PATTERN_EMAIL_RAW_NEG.matcher(s).replaceAll("") );
            mgLog.d("changed from \""+before+"\" to \""+s+"\"");
        }
        boolean valid = PATTERN_EMAIL.matcher(s).matches();
        valid &= !SharePerson.DUMMY_EMAIL.equals(s.toString());
        enablePref.setValue(valid);
        enablePref.changed();
    }
}
