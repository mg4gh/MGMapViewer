package mg.mapviewer.features.search.provider;

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

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.features.search.SearchProvider;
import mg.mapviewer.features.search.SearchRequest;
import mg.mapviewer.features.search.SearchResult;
import mg.mapviewer.model.BBox;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PointModelUtil;

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
                        sUrl = String.format(Locale.ENGLISH, "%ssearch?q=%s&%s&bounded=1&format=geojson",
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