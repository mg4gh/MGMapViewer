package mg.mgmap.generic.util.hints;

import android.app.Activity;

import java.util.Arrays;
import java.util.List;

import mg.mgmap.R;
import mg.mgmap.activity.settings.MapLayerListPreference;

public class HintMapLayerAssignment extends AbstractHint implements Runnable{

    final List<String> mapKeys;
    public HintMapLayerAssignment(Activity activity, List<String> mapKeys){
        super(activity, R.string.hintMapLayerAssignment);
        this.mapKeys = mapKeys;
        title = "Map layer assignment";
        spanText = """
                There are up to five map layers that can be put one over the other with the option to control transparency of each layer. \
                For the typical usage press now "Select map layer 2", and then select the downloaded map entry. Once this is done use the Android \
                Back button to go back to the map view.""";
        addGotItAction(()-> prefShowHint.setValue(false));
    }

    @Override
    public boolean checkHintCondition() {

        return super.checkHintCondition()
                && Arrays.stream(new MapLayerListPreference(activity, null).getAvailableMapLayers(activity)).anyMatch(s1 -> s1.startsWith("MAPSFORGE") // there is a mapsforge map available
                && mapKeys.stream().noneMatch(s2 -> s2.startsWith("MAPSFORGE")) // but no mapsforge map is yet selected
        );
    }


}
