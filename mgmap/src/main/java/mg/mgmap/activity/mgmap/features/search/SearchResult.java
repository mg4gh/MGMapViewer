/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.mgmap.features.search;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;

public class SearchResult implements Comparable<SearchResult>{
    public final SearchRequest searchRequest;
    public final String resultText;
    public String longResultText = null;
    public final PointModel pos;
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
