package mg.mapviewer.features.search;

import android.content.SharedPreferences;

import mg.mapviewer.MGMapApplication;

public abstract class SearchProvider {

    protected MGMapApplication application = null;
    protected SearchView searchView = null;
    protected MSSearch msSearch = null;
    protected SharedPreferences preferences = null;

    protected void init(MGMapApplication application, MSSearch msSearch, SearchView searchView, SharedPreferences preferences){
        this.application = application;
        this.msSearch = msSearch;
        this.searchView = searchView;
        this.preferences = preferences;
    }

    public abstract void doSearch(SearchRequest searchRequest);
}
