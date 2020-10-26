package mg.mapviewer.settings;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import mg.mapviewer.util.PersistenceManager;

public class SearchProviderListPreference extends ListPreference {

    public SearchProviderListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setSummaryProvider(SimpleSummaryProvider.getInstance());

        String[] searchCfgs = PersistenceManager.getInstance().getSearchConfigNames();
        String[] searchProviders = new String[searchCfgs.length];
        for (int i=0; i<searchCfgs.length; i++){
            searchProviders[i] = searchCfgs[i].replaceAll(".cfg$", "");
        }
        if (searchProviders.length == 0) {
            searchProviders = new String[]{ "Nominatim" };
        }
        setEntries(searchProviders);
        setEntryValues(searchProviders);

        setDefaultValue("Nominatim");
    }
}
