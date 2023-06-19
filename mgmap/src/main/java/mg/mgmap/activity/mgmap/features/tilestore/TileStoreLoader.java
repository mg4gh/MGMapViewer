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
package mg.mgmap.activity.mgmap.features.tilestore;

import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.queue.Job;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.util.BgJobGroup;
import mg.mgmap.generic.util.BgJobGroupCallback;
import mg.mgmap.generic.util.basic.MGLog;

@SuppressWarnings("IOStreamConstructor")
public class TileStoreLoader {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public File storeDir;
    MGMapApplication application;
    MGMapActivity activity;
    MGTileStore mgTileStore;

    public XmlTileSource xmlTileSource;
    BgJobGroup jobGroup;

    public TileStoreLoader(MGMapActivity activity, MGMapApplication application, MGTileStore mgTileStore) throws Exception {
        this.activity = activity;
        this.application = application;
        this.mgTileStore = mgTileStore;
        this.storeDir = mgTileStore.getStoreDir();
        init();

        jobGroup = new BgJobGroup(application, activity, "", new BgJobGroupCallback() {

            boolean allowRetry = true;
            @Override
            public boolean groupFinished(BgJobGroup bgJobGroup, int total, int success, int fail) {
                if (success > 0){
                    mgTileStore.purgeCache();
                    activity.getPrefCache().get(R.string.FSPosition_pref_RefreshMapView, false).toggle(); //after Tile downloads/drops this helps to make downloaded tiles visible
                }
                mgLog.d("allowRetry="+allowRetry);
                return (fail == total) && (new File(storeDir, "retry.json").exists()) && allowRetry;
            }

            @Override
            public void retry(BgJobGroup jobGroup) {
                allowRetry = false;
                mgLog.d("allowRetry="+allowRetry);
                new Thread(() -> {
                    try {
                        FileInputStream is = new FileInputStream(new File(storeDir, "retry.json"));
                        FileOutputStream os = new FileOutputStream(new File(storeDir, "cookies.json"));
                        File fProps = new File(storeDir, "param.properties");
                        File fPropsTxt = new File(storeDir, "param.properties.txt"); // some apps add ".txt" manager during download - clean this up
                        if (!fProps.exists() && fPropsTxt.exists()) fPropsTxt.renameTo(fProps);
                        Properties props = new Properties();
                        props.load( new FileInputStream(fProps) );
                        if (new DynamicHandler(is, os, props).run()){
                            init();
                        }
                        jobGroup.doit(); // this is the real retry
                    } catch (Exception e) {
                        mgLog.e(e);
                    }
                }).start();

            }
        });
    }

    private void init() throws Exception {
        XmlTileSourceConfig config = new XmlTileSourceConfigReader().parseXmlTileSourceConfig(storeDir.getName(), new FileInputStream(new File(storeDir, "config.xml")));
        xmlTileSource = new XmlTileSource(config);

        File cookies = new File(storeDir, "cookies.json");
        if (cookies.exists() && (cookies.length() > 0)) {
            Map<String, String> cookieMap = new HashMap<>();

            JsonArray cAll = null;
            try {
                FileReader jsonFile = new FileReader(cookies);
                JsonReader jsonReader = Json.createReader(jsonFile);
                JsonObject oAll = jsonReader.readObject();
                cAll = oAll.getJsonArray("cookies");
            } catch (Exception e){
                mgLog.w(e.getMessage());
            }
            if (cAll == null){
                try {
                    FileReader jsonFile = new FileReader(cookies);
                    JsonReader jsonReader = Json.createReader(jsonFile);
                    cAll = jsonReader.readArray();
                } catch (Exception e){
                    mgLog.w(e.getMessage());
                }
            }
            if (cAll != null){
                for (JsonValue i : cAll) {
                    JsonObject io = i.asJsonObject();
                    if (io != null){
                        if ((io.get("name") != null) && (io.get("value") != null)){
                            cookieMap.put(io.getString("name"), io.getString("value"));
                        }
                        if ((io.get("Name raw") != null) && (io.get("Content raw") != null)){
                            cookieMap.put(io.getString("Name raw"), io.getString("Content raw"));
                        }
                    }
                }

                String separator = "; ";
                StringBuilder cookieRes = new StringBuilder();
                for (Map.Entry<String, String> entry : cookieMap.entrySet() ){
                    cookieRes.append(entry.getKey()).append("=").append(entry.getValue()).append(separator);
                }
                config.setConnRequestProperty("Cookie", cookieRes.substring(0, cookieRes.length()-separator.length()));
                mgLog.i("cookies.json result: "+config.connRequestProperties.get("Cookie"));
            }
        }
    }


    public void loadFromBB(BBox bBox, boolean all){
        int tileSize = mgTileStore.getTileSize();
        for (byte zoomLevel=xmlTileSource.getZoomLevelMin(); zoomLevel<= xmlTileSource.getZoomLevelMax(); zoomLevel++) {
            long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
            int tileXMin = MercatorProjection.pixelXToTileX(MercatorProjection.longitudeToPixelX(bBox.minLongitude, mapSize), zoomLevel, tileSize);
            int tileXMax = MercatorProjection.pixelXToTileX(MercatorProjection.longitudeToPixelX(bBox.maxLongitude, mapSize), zoomLevel, tileSize);
            int tileYMin = MercatorProjection.pixelYToTileY(MercatorProjection.latitudeToPixelY(bBox.maxLatitude, mapSize), zoomLevel, tileSize); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY(MercatorProjection.latitudeToPixelY(bBox.minLatitude, mapSize), zoomLevel, tileSize);
            mgLog.i(String.format(Locale.ENGLISH, "dls %d %d %d %d %d", zoomLevel, tileXMin, tileXMax, tileYMin, tileYMax));

            for (int tileX = tileXMin; tileX<= tileXMax; tileX++){
                for (int tileY = tileYMin; tileY<= tileYMax; tileY++) {
                    Tile tile = new Tile(tileX, tileY, zoomLevel, tileSize);
                    boolean bOld = mgTileStore.containsKey(new Job(tile, false));
                    if (all || !bOld){
                        jobGroup.addJob(  mgTileStore.getLoaderJob(this, tile, bOld) );
                    }
                }
            }
        }

        jobGroup.setTitle("Load Tiles for \""+storeDir.getName()+"\"");
        jobGroup.setConstructed("Load "+jobGroup.size()+" tiles?");
    }

    public void dropFromBB(BBox bBox){
        int tileSize = mgTileStore.getTileSize();
        int numDrops = 0;
        for (byte zoomLevel=xmlTileSource.getZoomLevelMin(); zoomLevel<= xmlTileSource.getZoomLevelMax(); zoomLevel++) {
            long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
            int tileXMin = MercatorProjection.pixelXToTileX(MercatorProjection.longitudeToPixelX(bBox.minLongitude, mapSize), zoomLevel, tileSize);
            int tileXMax = MercatorProjection.pixelXToTileX(MercatorProjection.longitudeToPixelX(bBox.maxLongitude, mapSize), zoomLevel, tileSize);
            int tileYMin = MercatorProjection.pixelYToTileY(MercatorProjection.latitudeToPixelY(bBox.maxLatitude, mapSize), zoomLevel, tileSize); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY(MercatorProjection.latitudeToPixelY(bBox.minLatitude, mapSize), zoomLevel, tileSize);
            mgLog.i(String.format(Locale.ENGLISH, "dls %d %d %d %d %d", zoomLevel, tileXMin, tileXMax, tileYMin, tileYMax));

            if ( ((tileXMax-tileXMin) > 1) && ((tileYMax-tileYMin) >1 )){
                numDrops += (tileXMax-tileXMin-1)*(tileYMax-tileYMin-1);
                jobGroup.addJob(  mgTileStore.getDropJob(this, tileXMin, tileXMax, tileYMin, tileYMax, zoomLevel) );
            }
        }

        jobGroup.setTitle("Drop Tiles for \""+storeDir.getName()+"\"");
        mgLog.i("Drop "+numDrops+" tiles in "+jobGroup.size()+" jobs?");
        jobGroup.setConstructed("Drop "+numDrops+" tiles in "+jobGroup.size()+" jobs?");
    }
}
