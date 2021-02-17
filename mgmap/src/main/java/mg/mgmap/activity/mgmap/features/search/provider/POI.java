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

import android.database.Cursor;
import android.util.Log;

import org.sqlite.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeSet;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.activity.mgmap.features.search.SearchProvider;
import mg.mgmap.activity.mgmap.features.search.SearchRequest;
import mg.mgmap.activity.mgmap.features.search.SearchResult;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.application.util.PersistenceManager;

public class POI extends SearchProvider {

    File poiFile = null;

    static {
        try {
            System.loadLibrary("sqliteX");
        } catch (Throwable t) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), t);
        }
    }





    private SearchRequest lastSearchRequest = new SearchRequest("", 0, 0, new PointModelImpl(), 0);
    private ArrayList<SearchResult> lastSearchResults = new ArrayList<>();

    @Override
    public void doSearch(SearchRequest request) {

        if (request.actionId < 0) return;

        if (request.text.equals(lastSearchRequest.text) ){
            if (request.pos.equals(lastSearchRequest.pos)){
                publishResult(request, lastSearchResults);
                return;
            }
        }


        PointModel pm = request.pos;
        BBox bBox;

        if (request.text.equals("")){ // reverse search
            bBox = new BBox().extend(pm).extend(100);
        } else {
            int radius = 10 << Math.max(0,  12 - request.zoom);
            bBox = new BBox().extend(pm).extend(radius*1000);
        }



        File mapsDir = PersistenceManager.getInstance().getMapsDir();
        File mapsforgeDir = new File (mapsDir, "mapsforge");
        poiFile = null;
        for (String prefKey : activity.getMapLayerKeys()){
            String key =  preferences.getString(prefKey, "");
            if (key.startsWith("MAPSFORGE:")){
                File poi = new File(mapsforgeDir, key.replaceAll("MAPSFORGE: ", "").replaceAll("map$","poi"));
                if (poi.exists()){
                    poiFile = poi;
                    break;
                }
            }
        }


        new Thread(){
            @Override
            public void run() {
                SQLiteDatabase db = null;
                try {
                    TreeSet<SearchResult> resList = new TreeSet<>();
                    HashMap<String, String> subMap = new HashMap<>();

                    db = SQLiteDatabase.openDatabase(poiFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);

                    String select = "SELECT poi_data.id,poi_data.data,poi_index.id,poi_index.minLat,poi_index.maxLat,poi_index.minLon,poi_index.maxLon FROM poi_data INNER JOIN poi_index ON poi_data.id=poi_index.id  ";
                    String posMatch = String.format(Locale.ENGLISH, " ( (minLat>%.6f) AND (minLon>%.6f) AND (maxLat<%.6f) AND (maxLon<%.6f) )",
                            bBox.minLatitude,bBox.minLongitude,bBox.maxLatitude,bBox.maxLongitude);
                    String textPart[] = request.text.split(" ");
                    String textMatch = " AND ";
                    for (String part : textPart){
                        textMatch += " instr(lower(data),lower(\""+part+"\")) AND";
                    }
                    textMatch = textMatch.replaceAll("AND$", "");
                    if (request.text.equals("")){
                        textMatch = "";
                    }

                    String SELECT_STATEMENT = select + " WHERE ( " +posMatch+textMatch +");";
//                    String SELECT_STATEMENT = "SELECT * FROM poi_data INNER JOIN poi_index ON poi_data.id=poi_index.id WHERE  (instr(data,\""+request.text+"\"))";
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+SELECT_STATEMENT);

                    Cursor cursor = null;
                    cursor = db.rawQuery(SELECT_STATEMENT, null);
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" cursor.count="+cursor.getCount());
                    while (cursor.moveToNext()) {
                        // Column values
                        int id = cursor.getInt(0);
                        String text = cursor.getString(1);
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+id+" "+text);

                        subMap.clear();
                        for (String resPart : text.split("\\r")){
//                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+resPart);
                            String[] sub = resPart.split("=");
                            if (sub.length == 2){
                                subMap.put(sub[0],sub[1]);
                            }
                        }
                        String res = "";
                        String key;

                        if (("yes".equals(subMap.get("bus"))) || ("bus_stop".equals(subMap.get("highway")))){
                            res += "Bus ";
                        }
                        if (("yes".equals(subMap.get("tram"))) || ("tram_stop".equals(subMap.get("railway")))){
                            res += "Tram ";
                        }
                        if ("platform".equals(subMap.get("railway"))){
                            continue;
                        }


                        key="name";
                        if (subMap.containsKey(key)){
                            res += subMap.get(key);
                        } else {
                            key="brand";
                            if (subMap.containsKey(key)){
                                res += subMap.get(key);
                            } else {
                                continue;
                            }
                        }

                        key="addr:street";
                        res += (!subMap.containsKey(key))?"":", "+subMap.get(key);

                        key="addr:housenumber";
                        res += (!subMap.containsKey(key))?"":" "+subMap.get(key);

                        res += ",";
                        key="addr:postcode";
                        res += (!subMap.containsKey(key))?"":" "+subMap.get(key);

                        key="addr:city";
                        res += (!subMap.containsKey(key))?"":" "+subMap.get(key);

                        if (res.endsWith(",")){
                            res = res.substring(0,res.length()-1);
                        }
                        if (res.length() < 5) continue;




                        double lat = cursor.getDouble(3);
                        double lon = cursor.getDouble(5);
                        PointModel pos = new PointModelImpl(lat,lon);
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+id+" "+pos+" "+res);

                        SearchResult sr = new SearchResult(request, res, pos);
                        sr.longResultText = text.replaceAll("\\r"," ");
                        resList.add( sr );
                    }

                    ArrayList<SearchResult> res = new ArrayList<>();
                    for (SearchResult sr: resList) {
                        res.add(sr);
                        if (res.size() >= 5) break;
                    }
                    publishResult(request, res);
                } catch (Exception e) {
                    Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
                } finally {
                    if (db != null){
                        db.close();
                    }
                }

            }
        }.start();
    }


    private void publishResult(SearchRequest request, ArrayList<SearchResult> results){
        if (request.timestamp > lastSearchRequest.timestamp){
            lastSearchRequest = request;
            lastSearchResults = results;
            searchView.setResList(results);
        }
    }

}
