package mg.mgmap.generic.util.hints;

import android.app.Activity;
import android.text.SpannableString;

import java.util.ArrayList;

import mg.mgmap.R;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;

public abstract class AbstractHint implements Runnable{

    protected final Activity activity;
    protected String title = "";
    protected String spanText = "";
    protected final ArrayList<Runnable> gotItActions = new ArrayList<>();
    Pref<Boolean> prefShowHints;
    Pref<Boolean> prefShowHint;
    protected boolean showOnce = true;
    protected boolean allowAbort = false;
    protected boolean showAlways = false;

    protected final PrefCache prefCache;

    public AbstractHint(Activity activity, int prefShowHintId){
        this.activity = activity;
        prefCache = PrefCache.getApplicationPrefCache(activity);
        assert prefCache != null;
        prefShowHints = prefCache.get(activity.getResources().getString(R.string.preferences_hints_key), true);
        prefShowHint = prefCache.get(activity.getResources().getString(prefShowHintId), true);
    }

    @Override
    public void run() {
        MGMapApplication application = (MGMapApplication) getActivity().getApplication();
        application.getHintUtil().showHint(this);
    }

    public Activity getActivity(){
        return activity;
    }
    public String getHeadline(){
        return "Hint: "+title;
    }

    public String getText(){
        return spanText;
    }

    public boolean noticeSpannableString(SpannableString spannableString){
        return false;
    }

    public boolean checkHintCondition(){
        return (prefShowHints.getValue() && prefShowHint.getValue()) || showAlways;
    }

    public AbstractHint addGotItAction(Runnable gotItAction){
        gotItActions.add(gotItAction);
        return this;
    }

    public boolean isAllowAbort() {
        return allowAbort;
    }
}
