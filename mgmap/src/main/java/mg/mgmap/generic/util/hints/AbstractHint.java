package mg.mgmap.generic.util.hints;

import android.app.Activity;
import android.util.Log;

import java.util.ArrayList;

import mg.mgmap.R;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;
import mg.mgmap.generic.util.basic.NameUtil;

public abstract class AbstractHint implements Runnable{

    protected Activity activity;
    protected String title = "";
    protected String spanText = "";
    protected ArrayList<Runnable> gotItActions = new ArrayList<>();
    Pref<Boolean> prefShowHints;
    Pref<Boolean> prefShowHint;
    boolean showOnce;

    protected PrefCache prefCache;

    public AbstractHint(Activity activity, int prefShowHintId, boolean showOnce){
        this.activity = activity;
        this.showOnce = showOnce;
        prefCache = PrefCache.getApplicationPrefCache(activity);
        assert prefCache != null;
        prefShowHints = PrefCache.getApplicationPrefCache(activity).get(activity.getResources().getString(R.string.preferences_hints_key), true);
        prefShowHint = prefCache.get(activity.getResources().getString(prefShowHintId), true);
    }

    @Override
    public void run() {
        Log.i(MGMapApplication.LABEL, NameUtil.context());
        HintUtil.showHint(this);
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

    public boolean checkHintCondition(){
        return prefShowHints.getValue() && prefShowHint.getValue();
    }

    public void addGotItAction(Runnable gotItAction){
        gotItActions.add(gotItAction);
    }
}
