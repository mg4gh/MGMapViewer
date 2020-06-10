package mg.mapviewer.features.search;

import android.content.SharedPreferences;

public abstract class SearchProvider {

    protected SearchView searchView = null;
    protected MSSearch msSearch = null;
    protected SharedPreferences preferences = null;

    protected void init(MSSearch msSearch, SearchView searchView, SharedPreferences preferences){
        this.msSearch = msSearch;
        this.searchView = searchView;
        this.preferences = preferences;
    }

    public abstract void doSearch(SearchRequest searchRequest);
}
