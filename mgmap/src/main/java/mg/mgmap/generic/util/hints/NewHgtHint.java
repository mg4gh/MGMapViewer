package mg.mgmap.generic.util.hints;

import android.app.Activity;

import mg.mgmap.R;
import mg.mgmap.application.util.HgtProvider;

public class NewHgtHint extends AbstractHint implements Runnable{

    final HgtProvider hgtProvider;

    /** This hint helps with the introduction of new hgt data source and provides automatic re-download of hgt data */
    public NewHgtHint(Activity activity, HgtProvider hgtProvider){
        super(activity, R.string.hintNewHgtData);
        this.hgtProvider = hgtProvider;
        allowAbort = true;
        title = "New HGT data available";
        spanText = "There are new (better) height data for europa available. Check for download now?";
    }

    @Override
    public boolean checkHintCondition() {
        if (super.checkHintCondition()){
            if (!hgtProvider.getEhgtList().isEmpty()){
                return true;
            } else {
                prefShowHint.setValue(false);
            }
        }
        return false;
//        return super.checkHintCondition() && (hgtProvider.getEhgtList().size() > 0);
    }
}
