package mg.mapviewer.features.search.provider;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import mg.mapviewer.features.search.MSSearch;
import mg.mapviewer.features.search.SearchProvider;
import mg.mapviewer.features.search.SearchRequest;
import mg.mapviewer.features.search.SearchResult;
import mg.mapviewer.features.search.SearchView;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PersistenceManager;

public class Pelias extends SearchProvider {


    private static String URL_ORS = "https://api.openrouteservice.org/geocode/";
    private static String API_KEY = "";

    private SearchRequest searchRequest = new SearchRequest("", 0, 0, new PointModelImpl(), 0);
    private ArrayList<SearchResult> searchResults = new ArrayList<>();

    @Override
    protected void init(MSSearch msSearch, SearchView searchView) {
        super.init(msSearch, searchView);
        try {
            InputStream is = PersistenceManager.getInstance().openSearchConfigInput(this.getClass().getSimpleName()+".cfg");
            String line;
            String ak = "API_KEY=";
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            while ((line = in.readLine()) != null){
                if (line.startsWith(ak)){
                    API_KEY=line.replaceAll("API_KEY=", "");
                }
            }
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
    }

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
        int radius = 50 << Math.max(0,  10 - request.zoom);

        new Thread(){
            @Override
            public void run() {
                try {
                    ArrayList<SearchResult> resList = new ArrayList<>();

                    String sUrl;
                    if (request.text.equals("")){
                        sUrl = String.format(Locale.ENGLISH, "%sreverse?api_key=%s&point.lon=%.6f&point.lat=%.6f",
                                URL_ORS, API_KEY, pm.getLon(), pm.getLat());
                    } else {
                        sUrl = String.format(Locale.ENGLISH, "%sautocomplete?api_key=%s&text=%s&focus.point.lon=%.6f&focus.point.lat=%.6f&boundary.circle.lon=%.6f&boundary.circle.lat=%.6f&boundary.circle.radius=%d",
                                URL_ORS, API_KEY, request.text, pm.getLon(), pm.getLat(), pm.getLon(), pm.getLat(), radius);
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
                            String resText = String.format("%s", po.getString("label"));

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