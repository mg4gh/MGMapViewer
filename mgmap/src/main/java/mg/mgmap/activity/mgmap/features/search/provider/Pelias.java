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
public class Pelias extends SearchProvider {


    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static final String URL_ORS = "https://api.openrouteservice.org/geocode/";
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
        int radius = 50 << Math.max(0,  10 - request.zoom);

        new Thread(() -> {
            try {
                ArrayList<SearchResult> resList = new ArrayList<>();

                String sUrl;
                if (request.text.isEmpty()){
                    sUrl = String.format(Locale.ENGLISH, "%sreverse?api_key=%s&size=5&point.lon=%.6f&point.lat=%.6f",
                            URL_ORS, apiKey, pm.getLon(), pm.getLat());
                } else {
                    sUrl = String.format(Locale.ENGLISH, "%sautocomplete?api_key=%s&text=%s&size=5&focus.point.lon=%.6f&focus.point.lat=%.6f&boundary.circle.lon=%.6f&boundary.circle.lat=%.6f&boundary.circle.radius=%d",
                            URL_ORS, apiKey, request.text, pm.getLon(), pm.getLat(), pm.getLon(), pm.getLat(), radius);
                    if (!fsSearch.isPosBasedSearch()){
                        sUrl = sUrl.replaceFirst("&focus.point.*","");
                    }
                }
                mgLog.i("sUrl="+sUrl);
                URL url = new URL(sUrl);
                URLConnection conn = url.openConnection();
                InputStream is = conn.getInputStream();

                JsonReader jsonReader = Json.createReader(is);
                JsonObject oAll = jsonReader.readObject();

                JsonArray fAll = oAll.getJsonArray("features");
                for (JsonValue f : fAll) {

                    try {
                        JsonObject fo = f.asJsonObject();

                        JsonObject go = fo.getJsonObject("geometry");
                        JsonArray coos = go.getJsonArray("coordinates");

                        double lon = coos.getJsonNumber(0).doubleValue();
                        double lat = coos.getJsonNumber(1).doubleValue();
                        PointModel pm1 = new PointModelImpl(lat,lon);

                        JsonObject po = fo.getJsonObject("properties");
                        String res = String.format("%s", po.getString("label"));

                        SearchResult sr = new SearchResult(request, res, pm1);
                        sr.longResultText = fo.toString();
                        resList.add( sr );
                        mgLog.i("res="+res);
                    } catch (Exception e) {
                        mgLog.e(e);
                    }

                }

                publishResult(request, resList);
            } catch (IOException e) {
                mgLog.e(e);
            }

        }).start();
    }

}
