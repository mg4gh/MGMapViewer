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

import mg.mgmap.activity.mgmap.features.search.SearchProvider;
import mg.mgmap.activity.mgmap.features.search.SearchRequest;
import mg.mgmap.activity.mgmap.features.search.SearchResult;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.model.PointModelUtil;

@SuppressWarnings("unused") // usage is via reflection
public class Nominatim extends SearchProvider {


    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static final String URL_BASE = "https://nominatim.openstreetmap.org/";

    @Override
    public void doSearch(SearchRequest request) {

        if (request.actionId < 0) return;

        PointModel pm = request.pos;
        int radius = 10 << Math.max(0,  12 - request.zoom);
        BBox bBox = new BBox().extend(pm).extend(radius*1000);
        double d1 = PointModelUtil.distance(bBox.maxLatitude, bBox.maxLongitude, bBox.minLatitude, bBox.minLongitude);
        double d2 = PointModelUtil.distance(bBox.maxLatitude, bBox.maxLongitude, bBox.maxLatitude, bBox.minLongitude);
        mgLog.i("r="+radius+" d1="+d1+" d2="+d2);

        new Thread(() -> {
            try {
                ArrayList<SearchResult> resList = new ArrayList<>();

                String sUrl;
                if (request.text.isEmpty()){
                    sUrl = String.format(Locale.ENGLISH, "%sreverse?lon=%.6f&lat=%.6f&zoom=%d&format=geojson",
                            URL_BASE, pm.getLon(), pm.getLat(),request.zoom);
                } else {
                    String viewbox= String.format(Locale.ENGLISH, "viewbox=%.6f,%.6f,%.6f,%.6f",bBox.minLongitude,bBox.minLatitude,bBox.maxLongitude, bBox.maxLatitude);
                    sUrl = String.format(Locale.ENGLISH, "%ssearch?q=%s&%s&bounded=1&limit=5&format=geojson",
                            URL_BASE, request.text, viewbox);
                    if (!fsSearch.isPosBasedSearch()){
                        sUrl = sUrl.replaceFirst("viewbox[^&]*&","");
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
                        String res = String.format("%s", po.getString("display_name"));

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
