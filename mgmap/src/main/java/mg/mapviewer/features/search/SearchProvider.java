package mg.mapviewer.features.search;

public abstract class SearchProvider {

    protected SearchView searchView = null;
    protected MSSearch msSearch = null;

    protected void init(MSSearch msSearch, SearchView searchView){
        this.msSearch = msSearch;
        this.searchView = searchView;
    }

    public abstract void doSearch(SearchRequest searchRequest);
}
