package mg.mgmap.activity.mgmap.features.shareloc;

import android.text.Editable;
import android.text.TextWatcher;

import java.lang.invoke.MethodHandles;
import java.util.regex.Pattern;

import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.DialogView;

public class TextWatcherEmail implements TextWatcher {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    static final Pattern PATTERN_EMAIL = Pattern.compile("^[a-zA-Z0-9]+([.\\-][a-zA-Z0-9]+)*@[a-zA-Z0-9]+([.\\-][a-zA-Z0-9]+)*[.][a-zA-Z0-9]+");
    static final Pattern PATTERN_EMAIL_RAW = Pattern.compile("[a-zA-Z0-9.\\-@]*");
    static final Pattern PATTERN_EMAIL_RAW_NEG = Pattern.compile("[^a-zA-Z0-9.\\-@]+");

    private final DialogView enableView;

    public TextWatcherEmail(DialogView enableView){
        this.enableView = enableView;
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
        enableView.setEnablePositive(PATTERN_EMAIL.matcher(s).matches());
    }
}
