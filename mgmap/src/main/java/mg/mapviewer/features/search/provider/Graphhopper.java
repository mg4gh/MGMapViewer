package mg.mapviewer.features.search.provider;

import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

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

public class Graphhopper extends SearchProvider {


    private static final String URL_ORS = "https://graphhopper.com/api/1/geocode?locale=de";
    private String apiKey = "";

    private SearchRequest searchRequest = new SearchRequest("", 0, 0, new PointModelImpl(), 0);
    private ArrayList<SearchResult> searchResults = new ArrayList<>();

    @Override
    protected void init(MSSearch msSearch, SearchView searchView, SharedPreferences preferences) {
        super.init(msSearch, searchView, preferences);
        Properties props = PersistenceManager.getInstance().getConfigProperties("search",this.getClass().getSimpleName()+".cfg");
        apiKey = props.getProperty("API_KEY");
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
        int radius = 50 << Math.max(0,  10 - request.zoom);

        new Thread(){
            @Override
            public void run() {
                try {
                    ArrayList<SearchResult> resList = new ArrayList<>();

                    String sUrl;
                    if (request.text.equals("")){
                        sUrl = String.format(Locale.ENGLISH, "%s&key=%s&point=%.6f,%.6f&limit=5&reverse=true",
                                URL_ORS, apiKey, pm.getLat(), pm.getLon());
                    } else {
                        sUrl = String.format(Locale.ENGLISH, "%s&key=%s&point=%.6f,%.6f&limit=5&q=%s",
                                URL_ORS, apiKey, pm.getLat(), pm.getLon(), request.text);
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
                                res = res.substring(0,res.length()-2);
                            }

                            JsonObject jpo =  fo.getJsonObject("point");

                            double lon = jpo.getJsonNumber("lng").doubleValue();
                            double lat = jpo.getJsonNumber("lat").doubleValue();
                            PointModel pm = new PointModelImpl(lat,lon);

                            resList.add( new SearchResult(request, res, pm));
                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+res);
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

    String getAttribute(JsonObject jo, String key){
        try {
            if (jo.containsKey(key))
                return jo.getJsonObject(key).toString();
        } catch (Exception e){
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }


    private void publishResult(SearchRequest request, ArrayList<SearchResult> results){
        if (request.timestamp > searchRequest.timestamp){
            searchRequest = request;
            searchResults = results;
            searchView.setResList(results);
        }
    }

}
