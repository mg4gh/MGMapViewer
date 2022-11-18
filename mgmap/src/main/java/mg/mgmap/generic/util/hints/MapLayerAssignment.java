package mg.mgmap.generic.util.hints;

import android.app.Activity;

import java.util.Arrays;

import mg.mgmap.R;
import mg.mgmap.activity.settings.MapLayerListPreference;
import mg.mgmap.generic.util.Pref;

public class MapLayerAssignment extends AbstractHint implements Runnable{

    Pref<Boolean> prefUseHint;

    public MapLayerAssignment(Activity activity){
        super(activity);
        title = "Map layer assignment";
        spanText = "This app provides up to 5 map layers that can be put one over the other and the option to control the layer transparency. " +
                "For the typical usage press now \"Select map layer 2\", and then select the downloaded map entry. Once this is done use the Android" +
                " Back button to go back to the map view.  ";
        prefUseHint = prefCache.get(activity.getResources().getString(R.string.hintMapLayerAssignment), true);
        addGotItAction(()-> prefUseHint.setValue(false));
    }

    @Override
    public boolean checkHintCondition() {
        if (prefUseHint.getValue())
            return Arrays.stream(new MapLayerListPreference(activity, null).getAvailableMapLayers(activity)).anyMatch(s -> s.startsWith("MAPSFORGE"));
        return false;
    }

}
