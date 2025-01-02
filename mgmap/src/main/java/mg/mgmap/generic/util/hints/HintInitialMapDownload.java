package mg.mgmap.generic.util.hints;

import android.app.Activity;


import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;

public class HintInitialMapDownload extends AbstractHint implements Runnable{

    public HintInitialMapDownload(Activity activity){
        super(activity, R.string.hintInitialMapDownload);
        title = "Initial map download";
        spanText = """
                After fresh install it is advised to download a mapsforge map. \
                Press menu R.drawable.group_task{0xFFC0C0C0,80,80} and menu item R.drawable.download{0xFFC0C0C0,80,80} \
                to open the download preference screen, then select a map download menu item.""";
    }

    @Override
    public boolean checkHintCondition() {
        boolean res = super.checkHintCondition();
        if (res && getActivity() instanceof MGMapActivity mgMapActivity){
            for (String key : mgMapActivity.getMapLayerFactory().getMapDataStoreMap().values()){
                if (key.startsWith("MAPSFORGE")){
                    res = false;
                    break;
                }
            }
        }
        return res;
    }

}
