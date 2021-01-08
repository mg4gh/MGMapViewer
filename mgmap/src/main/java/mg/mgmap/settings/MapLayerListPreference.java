package mg.mgmap.settings;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import mg.mgmap.MGMapLayerFactory;

public class MapLayerListPreference extends ListPreference {

    public MapLayerListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
        String[] maps = MGMapLayerFactory.getAvailableMapLayers();
        setEntries(maps);
        setEntryValues(maps);
        setDefaultValue(maps[0]);
    }
}
