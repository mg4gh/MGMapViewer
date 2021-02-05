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
package mg.mgmap.features.search.provider;

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

import mg.mgmap.MGMapApplication;
import mg.mgmap.features.search.SearchProvider;
import mg.mgmap.features.search.SearchRequest;
import mg.mgmap.features.search.SearchResult;
import mg.mgmap.model.BBox;
import mg.mgmap.model.PointModel;
import mg.mgmap.model.PointModelImpl;
import mg.mgmap.util.NameUtil;
import mg.mgmap.model.PointModelUtil;

public class Nominatim extends SearchProvider {


    private static String URL_BASE = "https://nominatim.openstreetmap.org/";

    private SearchRequest searchRequest = new SearchRequest("", 0, 0, new PointModelImpl(), 0);
    private ArrayList<SearchResult> searchResults = new ArrayList<>();

    @Override
    public void doSearch(SearchRequest request) {

        if (request.actionId < 0) return;

        if (request.text.equals(searchRequest.text) ){
            if (request.pos.equals(searchRequest.pos)){
                publishResult(request, searchResults);
                return;
            }
        }

        PointModel pm = request.pos;
        int radius = 10 << Math.max(0,  12 - request.zoom);
        BBox bBox = new BBox().extend(pm).extend(radius*1000);
        double d1 = PointModelUtil.distance(bBox.maxLatitude, bBox.maxLongitude, bBox.minLatitude, bBox.minLongitude);
        double d2 = PointModelUtil.distance(bBox.maxLatitude, bBox.maxLongitude, bBox.maxLatitude, bBox.minLongitude);
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" r="+radius+" d1="+d1+" d2="+d2);

        new Thread(){
            @Override
            public void run() {
                try {
                    ArrayList<SearchResult> resList = new ArrayList<>();

                    String sUrl;
                    if (request.text.equals("")){
                        sUrl = String.format(Locale.ENGLISH, "%sreverse?lon=%.6f&lat=%.6f&zoom=%d&format=geojson",
                                URL_BASE, pm.getLon(), pm.getLat(),request.zoom);
                    } else {
                        String viewbox= String.format(Locale.ENGLISH, "viewbox=%.6f,%.6f,%.6f,%.6f",bBox.minLongitude,bBox.minLatitude,bBox.maxLongitude, bBox.maxLatitude);
                        sUrl = String.format(Locale.ENGLISH, "%ssearch?q=%s&%s&bounded=1&limit=5&format=geojson",
                                URL_BASE, request.text, viewbox);
                    }
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+sUrl);

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
                            PointModel pm = new PointModelImpl(lat,lon);

                            JsonObject po = fo.getJsonObject("properties");
                            String resText = String.format("%s", po.getString("display_name"));

                            resList.add( new SearchResult(request, resText, pm));
                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+resText);
                        } catch (Exception e) {
                            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
                        }

                    }

                    publishResult(request, resList);
                } catch (IOException e) {
                    Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
                }

            }
        }.start();
    }


    private void publishResult(SearchRequest request, ArrayList<SearchResult> results){
        if (request.timestamp > searchRequest.timestamp){
            searchRequest = request;
            searchResults = results;
            searchView.setResList(results);
        }
    }

}
