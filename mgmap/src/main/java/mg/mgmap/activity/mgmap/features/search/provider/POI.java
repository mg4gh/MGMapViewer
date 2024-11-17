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
import android.database.Cursor;

import org.sqlite.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.TreeSet;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.features.search.FSSearch;
import mg.mgmap.activity.mgmap.features.search.SearchView;
import mg.mgmap.activity.mgmap.features.search.SearchProvider;
import mg.mgmap.activity.mgmap.features.search.SearchRequest;
import mg.mgmap.activity.mgmap.features.search.SearchResult;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.application.util.PersistenceManager;

@SuppressWarnings("unused") // usage is via reflection
public class POI extends SearchProvider {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    File poiFile = null;

    static {
        try {
            System.loadLibrary("sqliteX");
        } catch (Throwable t) {
            mgLog.e(t);
        }
    }


    private PersistenceManager persistenceManager = null;

    @Override
    protected void init(MGMapActivity activity, FSSearch fsSearch, SearchView searchView, SharedPreferences preferences) {
        super.init(activity, fsSearch, searchView, preferences);
        persistenceManager = fsSearch.getApplication().getPersistenceManager();
    }


    @Override
    public void doSearch(SearchRequest request) {

        if (request.actionId < 0) return;

        PointModel pm = request.pos;
        BBox bBox;

        if (request.text.isEmpty()){ // reverse search
            bBox = new BBox().extend(pm).extend(100);
        } else {
            int radius = 10 << Math.max(0,  12 - request.zoom);
            bBox = new BBox().extend(pm).extend(radius*1000);
        }



        File mapsDir = persistenceManager.getMapsDir();
        File mapsforgeDir = new File (mapsDir, "mapsforge");
        poiFile = null;
        for (String prefKey : activity.getMapLayerFactory().getMapLayerKeys()){
            String key =  preferences.getString(prefKey, "");
            if (key.startsWith("MAPSFORGE:")){
                File poi = new File(mapsforgeDir, key.replaceAll("MAPSFORGE: ", "").replaceAll("map$","poi"));
                if (poi.exists()){
                    poiFile = poi;
                    break;
                }
            }
        }


        new Thread(() -> {
            SQLiteDatabase db = null;
            try {
                TreeSet<SearchResult> resList = new TreeSet<>();
                HashMap<String, String> subMap = new HashMap<>();

                db = SQLiteDatabase.openDatabase(poiFile.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                String version;
                {
                    Cursor vcursor = db.rawQuery("select value from metadata where (metadata.name  == \"version\");", null);
                    mgLog.d("vcursor "+vcursor.getCount());
                    vcursor.moveToNext();
                    version = vcursor.getString(0);
                    mgLog.d("version="+version);
                }


                String select, posMatch;
                int latIdx, lonIdx;
                if ("2".equals(version)){
                    select = "SELECT poi_data.id,poi_data.data,poi_index.id,poi_index.minLat,poi_index.maxLat,poi_index.minLon,poi_index.maxLon FROM poi_data INNER JOIN poi_index ON poi_data.id=poi_index.id  ";
                    posMatch = String.format(Locale.ENGLISH, " ( (minLat>%.6f) AND (minLon>%.6f) AND (maxLat<%.6f) AND (maxLon<%.6f) )",
                            bBox.minLatitude,bBox.minLongitude,bBox.maxLatitude,bBox.maxLongitude);
                    latIdx = 3;
                    lonIdx = 5;
                } else if ("3".equals(version)){
                    select = "SELECT poi_data.id,poi_data.data,poi_index.id,poi_index.lat,poi_index.lon FROM poi_data INNER JOIN poi_index ON poi_data.id=poi_index.id  ";
                    posMatch = String.format(Locale.ENGLISH, " ( (lat>%.6f) AND (lon>%.6f) AND (lat<%.6f) AND (lon<%.6f) )",
                            bBox.minLatitude,bBox.minLongitude,bBox.maxLatitude,bBox.maxLongitude);
                    latIdx = 3;
                    lonIdx = 4;
                } else {
                    mgLog.e("unknown database version="+version);
                    return;
                }

                String[] textPart = request.text.split(" ");
                StringBuilder textMatch = new StringBuilder(" AND ");
                for (String part : textPart){
                    textMatch.append(" instr(lower(data),lower(\"").append(part).append("\")) AND");
                }
                textMatch = new StringBuilder(textMatch.toString().replaceAll("AND$", ""));
                if (request.text.isEmpty()){
                    textMatch = new StringBuilder();
                }

                String SELECT_STATEMENT = select + " WHERE ( " +posMatch+textMatch +");";
                mgLog.i(SELECT_STATEMENT);

                Cursor cursor = db.rawQuery(SELECT_STATEMENT, null);
                mgLog.i("cursor.count="+cursor.getCount());
                while (cursor.moveToNext()) {
                    // Column values
                    int id = cursor.getInt(0);
                    String text = cursor.getString(1);
                    mgLog.i(id+" "+text);

                    subMap.clear();
                    for (String resPart : text.split("\\r")){
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




                    double lat = cursor.getDouble(latIdx);
                    double lon = cursor.getDouble(lonIdx);
                    PointModel pos = new PointModelImpl(lat,lon);
                    mgLog.i(id+" "+pos+" "+res);

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
                mgLog.e(e);
            } finally {
                if (db != null){
                    db.close();
                }
            }

        }).start();
    }

}
