package mg.mgmap.generic.util.hints;

import android.app.Activity;
import android.content.Context;
import android.os.PowerManager;

import mg.mgmap.R;
import mg.mgmap.generic.util.basic.MGLog;

public class HintBatteryUsage extends AbstractHint implements Runnable{

    public HintBatteryUsage(Activity activity){
        super(activity, R.string.hintBatteryUsage);
        showOnce = false;
        allowAbort = true;
        title = "Battery Usage";
        spanText = """
                Track recording in background requires that the battery usage is not optimized by Android.

                For proper function select 'Allow' in the next screen.""";
    }

    @Override
    public boolean checkHintCondition() {
        PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        MGLog.si("ignoreBatteryOptimizations="+pm.isIgnoringBatteryOptimizations(activity.getPackageName()));
        return super.checkHintCondition() &&
                !pm.isIgnoringBatteryOptimizations(activity.getPackageName());
    }
}
