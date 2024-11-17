package mg.mgmap.generic.util.hints;

import android.app.Activity;

import java.util.Arrays;

import mg.mgmap.R;
import mg.mgmap.activity.settings.MapLayerListPreference;

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
        return  super.checkHintCondition() &&
                Arrays.stream(new MapLayerListPreference(activity, null).getAvailableMapLayers(activity)).noneMatch(s -> s.startsWith("MAPSFORGE"));
    }

}
