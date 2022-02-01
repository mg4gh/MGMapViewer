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
package mg.mgmap.activity.mgmap.features.search.provider;

import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.features.search.FSSearch;
import mg.mgmap.activity.mgmap.features.search.DegreeUtil;
import mg.mgmap.activity.mgmap.features.search.SearchProvider;
import mg.mgmap.activity.mgmap.features.search.SearchRequest;
import mg.mgmap.activity.mgmap.features.search.SearchResult;
import mg.mgmap.activity.mgmap.features.search.SearchView;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.NameUtil;

public class GeoLatLong extends SearchProvider {

    private SearchRequest searchRequest = new SearchRequest("", 0, 0, new PointModelImpl(), 0);
    private ArrayList<SearchResult> searchResults = new ArrayList<>();

    @Override
    protected void init(MGMapActivity activity, FSSearch fsSearch, SearchView searchView, SharedPreferences preferences) {
        super.init(activity, fsSearch, searchView, preferences);
    }

    @Override
    public void doSearch(SearchRequest request) {

        ArrayList<SearchResult> resList = new ArrayList<>();

        if (request.text.equals("")){ // reverse request
            setResults(request, request.pos, resList);
        } else { // forward request
            String[] words = request.text.split("[\\s,]");
            int idx = 0;
            if (words[idx].length() == 0){
                idx++;
            }
            double lat = 0, lon = 0;
            try {
                if (words.length > idx){
                    lat = DegreeUtil.degree2double(true, words[idx]);
                }
            } catch (NumberFormatException e) {
                try {
                    lat = Double.parseDouble(words[idx]);
                } catch (NumberFormatException e1) { }
            }
            idx++;
            try {
                if (words.length > idx){
                    lon = DegreeUtil.degree2double(false, words[idx]);
                }
            } catch (NumberFormatException e) {
                try {
                    lon = Double.parseDouble(words[idx]);
                } catch (NumberFormatException e1) { }
            }
            PointModel pm = new PointModelImpl(lat,lon);
            setResults(request, pm, resList);
        }
        publishResult(request, resList);
    }

    private void setResults(SearchRequest request, PointModel pm, ArrayList<SearchResult> results){
        String res = String.format(Locale.ENGLISH,"Lat=%2.6f, Long=%2.6f", pm.getLat(), pm.getLon());
        results.add( new SearchResult(request, res, pm));
        String res2 = String.format(Locale.ENGLISH,"Lat=%s, Long=%s", DegreeUtil.double2Degree(true, pm.getLat()), DegreeUtil.double2Degree(false, pm.getLon()));
        results.add( new SearchResult(request, res2, pm));
    }


    private void publishResult(SearchRequest request, ArrayList<SearchResult> results){
        if (request.timestamp > searchRequest.timestamp){
            searchRequest = request;
            searchResults = results;
            searchView.setResList(results);
        }
    }

}
