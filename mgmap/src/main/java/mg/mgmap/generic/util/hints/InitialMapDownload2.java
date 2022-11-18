package mg.mgmap.generic.util.hints;

import android.app.Activity;

import mg.mgmap.R;
import mg.mgmap.generic.util.Pref;

public class InitialMapDownload2 extends AbstractHint implements Runnable{

    Pref<Boolean> prefInitial;

    public InitialMapDownload2(Activity activity){
        super(activity);
        title = "Initial map download";
        spanText = "Now your browser will open www.openandromaps.de map download page. " +
                "Find the map you want to download (e.g. R.drawable.germany{0xFFC0C0C0,300,80}). Press the R.drawable.plus{0xFFC0C0C0,80,80} " +
                "in front of the map name. Among the visible options select the line starting with\n\"Android mf-V5-map:\" and press R.drawable.im{0xFFC0C0C0,320,100}.";
        prefInitial = prefCache.get(activity.getResources().getString(R.string.hintInitialMapDownload2), true);
        addGotItAction(()->prefInitial.setValue(false));
    }

    @Override
    public boolean checkHintCondition() {
        return prefInitial.getValue();
    }

}
