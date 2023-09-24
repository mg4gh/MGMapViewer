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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Locale;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.activity.mgmap.features.search.FSSearch;
import mg.mgmap.activity.mgmap.features.search.SearchProvider;
import mg.mgmap.activity.mgmap.features.search.SearchRequest;
import mg.mgmap.activity.mgmap.features.search.SearchResult;
import mg.mgmap.activity.mgmap.features.search.SearchView;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.NameUtil;

public class Graphhopper extends SearchProvider {


    private static final String URL_ORS = "https://graphhopper.com/api/1/geocode?locale=de";
    private String apiKey = "";

    private SearchRequest searchRequest = new SearchRequest("", 0, 0, new PointModelImpl(), 0);
    private ArrayList<SearchResult> searchResults = new ArrayList<>();

    @Override
    protected void init(MGMapActivity activity, FSSearch fsSearch, SearchView searchView, SharedPreferences preferences) {
        super.init(activity, fsSearch, searchView, preferences);
        apiKey = getSearchConfig().getProperty("API_KEY");
    }

    @Override
    public void doSearch(SearchRequest request) {

        if ((request.actionId < 0) && (request.text.length() <=5)) return;

        if (request.text.equals(searchRequest.text) ){
            if (request.pos.equals(searchRequest.pos)){
                publishResult(request, searchResults);
                return;
            }
        }

        PointModel pm = request.pos;

        new Thread(() -> {
            try {
                ArrayList<SearchResult> resList = new ArrayList<>();

                String sUrl;
                if (request.text.equals("")){
                    sUrl = String.format(Locale.ENGLISH, "%s&key=%s&point=%.6f,%.6f&limit=5&reverse=true",
                            URL_ORS, apiKey, pm.getLat(), pm.getLon());
                } else {
                    sUrl = String.format(Locale.ENGLISH, "%s&key=%s&point=%.6f,%.6f&limit=5&q=%s",
                            URL_ORS, apiKey, pm.getLat(), pm.getLon(), request.text);
                    if (!fsSearch.isPosBasedSearch()){
                        sUrl = sUrl.replaceFirst("point[^&]*&","");
                    }
                }
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+sUrl);

                URL url = new URL(sUrl);
                URLConnection conn = url.openConnection();
                InputStream is = conn.getInputStream();

                JsonReader jsonReader = Json.createReader(is);
                JsonObject oAll = jsonReader.readObject();

                JsonArray fAll = oAll.getJsonArray("hits");
                for (JsonValue f : fAll) {

                    try {
                        JsonObject fo = f.asJsonObject();

                        String res = "";
                        String key;

                        key="name";
                        res += (!fo.containsKey(key))?"":fo.getString(key);

                        key="street";
                        res += (!fo.containsKey(key))?"":(", "+fo.getString(key));

                        key="housenumber";
                        res += (!fo.containsKey(key))?"":(" "+fo.getString(key));

                        res += ",";
                        key="postcode";
                        res += (!fo.containsKey(key))?"":(" "+fo.getString(key));

                        key="city";
                        res += (!fo.containsKey(key))?"":(" "+fo.getString(key));

                        if (res.endsWith(",")){
                            res = res.substring(0,res.length()-1);
                        }

                        JsonObject jpo =  fo.getJsonObject("point");

                        double lon = jpo.getJsonNumber("lng").doubleValue();
                        double lat = jpo.getJsonNumber("lat").doubleValue();
                        PointModel pm1 = new PointModelImpl(lat,lon);

                        resList.add( new SearchResult(request, res, pm1));
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+res);
                    } catch (Exception e) {
                        Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
                    }

                }

                publishResult(request, resList);
            } catch (IOException e) {
                Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
            }

        }).start();
    }


    private void publishResult(SearchRequest request, ArrayList<SearchResult> results){
        if (request.timestamp > searchRequest.timestamp){
            searchRequest = request;
            searchResults = results;
            searchView.setResList(results);
        }
    }

}
