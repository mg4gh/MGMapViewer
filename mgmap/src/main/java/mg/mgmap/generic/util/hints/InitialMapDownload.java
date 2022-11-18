package mg.mgmap.generic.util.hints;

import android.app.Activity;

import mg.mgmap.R;
import mg.mgmap.generic.util.Pref;

public class InitialMapDownload extends AbstractHint implements Runnable{

    Pref<Boolean> prefInitial;

    public InitialMapDownload(Activity activity){
        super(activity);
        title = "Initial map download";
        spanText = "After fresh install it is advised to download a mapsforge map. " +
                "Press menu R.drawable.group_task{0xFFC0C0C0,80,80} and menu item R.drawable.download{0xFFC0C0C0,80,80} " +
                "to open the download preference screen.";
        prefInitial = prefCache.get(activity.getResources().getString(R.string.hintInitialMapDownload), true);
        addGotItAction(()->prefInitial.setValue(false));
    }

    @Override
    public boolean checkHintCondition() {
        return prefInitial.getValue();
    }

}
