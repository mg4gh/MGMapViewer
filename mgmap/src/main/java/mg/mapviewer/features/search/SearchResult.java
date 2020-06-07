package mg.mapviewer.features.search;

import mg.mapviewer.model.PointModel;

public class SearchResult {
    public SearchRequest searchRequest;
    public String resultText = null;
    public PointModel pos = null;

    public SearchResult(SearchRequest searchRequest, String resultText, PointModel pos) {
        this.searchRequest = searchRequest;
        this.resultText = resultText;
        this.pos = pos;
    }
}
