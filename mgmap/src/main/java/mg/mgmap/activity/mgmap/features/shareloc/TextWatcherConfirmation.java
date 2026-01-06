package mg.mgmap.activity.mgmap.features.shareloc;

import android.text.Editable;
import android.text.TextWatcher;

import java.lang.invoke.MethodHandles;
import java.util.regex.Pattern;

import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;

public class TextWatcherConfirmation implements TextWatcher {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    static final Pattern PATTERN_CONFIRMATION = Pattern.compile("[a-f0-9]{8}");

    static final Pattern PATTERN_CONFIRMATION_RAW = Pattern.compile("[a-f0-9]{0,8}");
    static final Pattern PATTERN_CONFIRMATION_NEG = Pattern.compile("[^a-f0-9]+");

    private final Pref<Boolean> enablePref;

    public TextWatcherConfirmation(Pref<Boolean> enablePref){
        this.enablePref = enablePref;
    }
    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {}

    @Override
    public void afterTextChanged(Editable s) {
        if (!PATTERN_CONFIRMATION_RAW.matcher(s).matches()){
            String before = s.toString();
            s.replace(0,s.length(), PATTERN_CONFIRMATION_NEG.matcher(s).replaceAll("") );
            mgLog.d("changed from \""+before+"\" to \""+s+"\"");
        }
        enablePref.setValue(PATTERN_CONFIRMATION.matcher(s).matches());
        enablePref.changed();
    }
}
