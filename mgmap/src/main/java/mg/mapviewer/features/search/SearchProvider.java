package mg.mapviewer.features.search;

import android.content.SharedPreferences;

import mg.mapviewer.MGMapApplication;

public abstract class SearchProvider {

    protected MGMapApplication application = null;
    protected SearchView searchView = null;
    protected FSSearch fsSearch = null;
    protected SharedPreferences preferences = null;

    protected void init(MGMapApplication application, FSSearch fsSearch, SearchView searchView, SharedPreferences preferences){
        this.application = application;
        this.fsSearch = fsSearch;
        this.searchView = searchView;
        this.preferences = preferences;
    }

    public abstract void doSearch(SearchRequest searchRequest);
}
