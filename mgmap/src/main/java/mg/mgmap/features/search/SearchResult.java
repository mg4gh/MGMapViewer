package mg.mgmap.features.search;

import mg.mgmap.model.PointModel;
import mg.mgmap.util.PointModelUtil;

public class SearchResult implements Comparable<SearchResult>{
    public SearchRequest searchRequest;
    public String resultText;
    public String longResultText = null;
    public PointModel pos;
    private final double distance;

    public SearchResult(SearchRequest searchRequest, String resultText, PointModel pos) {
        this.searchRequest = searchRequest;
        this.resultText = resultText;
        this.pos = pos;
        distance = PointModelUtil.distance(searchRequest.pos,pos);
    }

    @Override
    public int compareTo(SearchResult o) {
        if (distance != o.distance) return Double.compare(distance,o.distance);
        if (pos.getLat() != o.pos.getLat()) return Double.compare(pos.getLat(),o.pos.getLat());
        if (pos.getLon() != o.pos.getLon()) return Double.compare(pos.getLon(),o.pos.getLon());
        return resultText.compareTo(o.resultText);
    }
}
