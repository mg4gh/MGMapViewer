package mg.mgmap.generic.util.hints;

import android.app.Activity;

import java.util.ArrayList;

import mg.mgmap.generic.util.PrefCache;

public abstract class AbstractHint implements Runnable{

    protected Activity activity;
    protected String title = "";
    protected String spanText = "";
    protected ArrayList<Runnable> gotItActions = new ArrayList<>();

    protected PrefCache prefCache;

    public AbstractHint(Activity activity){
        this.activity = activity;
        prefCache = PrefCache.getApplicationPrefCache(activity);
    }

    @Override
    public void run() {
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
        return true;
    }

    public void addGotItAction(Runnable gotItAction){
        gotItActions.add(gotItAction);
    }
}
