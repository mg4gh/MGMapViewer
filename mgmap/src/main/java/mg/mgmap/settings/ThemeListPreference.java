package mg.mgmap.settings;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import mg.mgmap.util.PersistenceManager;

public class ThemeListPreference extends ListPreference {

    public ThemeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setSummaryProvider(SimpleSummaryProvider.getInstance());
        String[] themes = PersistenceManager.getInstance().getThemeNames();
        if (themes.length == 0) {
            themes = new String[]{ "Elevate.xml"};
        }
        setEntries(themes);
        setEntryValues(themes);
        setDefaultValue("Elevate.xml");
    }
}
