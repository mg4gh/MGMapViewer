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

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
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
import mg.mgmap.activity.mgmap.features.search.FSSearch;
import mg.mgmap.activity.mgmap.features.search.SearchProvider;
import mg.mgmap.activity.mgmap.features.search.SearchRequest;
import mg.mgmap.activity.mgmap.features.search.SearchResult;
import mg.mgmap.activity.mgmap.features.search.SearchView;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;

@SuppressWarnings("unused") // usage is via reflection
public class Graphhopper extends SearchProvider {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static final String URL_ORS = "https://graphhopper.com/api/1/geocode?locale=de";
    private String apiKey = "";

    @Override
    protected void init(MGMapActivity activity, FSSearch fsSearch, SearchView searchView, SharedPreferences preferences) {
        super.init(activity, fsSearch, searchView, preferences);
        apiKey = getSearchConfig().getProperty("API_KEY");
    }

    @Override
    public void doSearch(SearchRequest request) {

        if ((request.actionId < 0) && (request.text.length() <=5)) return;

        PointModel pm = request.pos;

        new Thread(() -> {
            try {
                ArrayList<SearchResult> resList = new ArrayList<>();

                String sUrl = null;
                if (request.text.isEmpty()){
                    if (activity.getMapViewUtility().getZoomLevel() > 20) { // assume GeoLatLong request
                        GeoLatLong.setResults(request, request.pos, resList);
                        setSearchText(String.format(Locale.ENGLISH,"%f, %f", request.pos.getLat(), request.pos.getLon()));
                    } else {
                        sUrl = String.format(Locale.ENGLISH, "%s&key=%s&point=%.6f,%.6f&limit=5&reverse=true",
                                URL_ORS, apiKey, pm.getLat(), pm.getLon());
                    }
                } else {
                    PointModel tpm = GeoLatLong.tryForwardSearch(request);
                    if (GeoLatLong.validate(tpm)){ // seems to be a term for GeoLatLong
                        GeoLatLong.setResults(request, tpm, resList);
                    } else {
                        sUrl = String.format(Locale.ENGLISH, "%s&key=%s&point=%.6f,%.6f&limit=5&q=%s",
                                URL_ORS, apiKey, pm.getLat(), pm.getLon(), request.text);
                        if (!fsSearch.isPosBasedSearch()){
                            sUrl = sUrl.replaceFirst("point[^&]*&","");
                        }
                    }
                }
                if (sUrl != null){
                    mgLog.i("sUrl="+sUrl);

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
                            key="countrycode";
                            res += (!fo.containsKey(key))?"":(", "+fo.getString(key));

                            JsonObject jpo =  fo.getJsonObject("point");

                            double lon = jpo.getJsonNumber("lng").doubleValue();
                            double lat = jpo.getJsonNumber("lat").doubleValue();
                            PointModel pm1 = new PointModelImpl(lat,lon);

                            SearchResult sr = new SearchResult(request, res, pm1);
                            sr.longResultText = fo.toString();
                            resList.add( sr );
                            mgLog.i("res="+res);
                        } catch (Exception e) {
                            mgLog.e(e);
                        }

                    }
                }

                publishResult(request, resList);
            } catch (IOException e) {
                mgLog.e(e);
            }

        }).start();
    }

}
