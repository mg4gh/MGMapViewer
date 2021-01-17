package mg.mgmap.features.search;

import android.content.SharedPreferences;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;

public abstract class SearchProvider {

    protected MGMapActivity activity = null;
    protected SearchView searchView = null;
    protected FSSearch fsSearch = null;
    protected SharedPreferences preferences = null;

    protected void init(MGMapActivity activity, FSSearch fsSearch, SearchView searchView, SharedPreferences preferences){
        this.activity = activity;
        this.fsSearch = fsSearch;
        this.searchView = searchView;
        this.preferences = preferences;
    }

    public abstract void doSearch(SearchRequest searchRequest);
}
