package mg.mapviewer.features.tilestore;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;

import android.util.Log;

import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.queue.Job;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.BBox;
import mg.mapviewer.util.BgJob;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.Permissions;

public class TileStoreLoader {





    public File storeDir;
    MGMapApplication application;
    MGMapActivity activity;
    MGTileStore mgTileStore;

    public XmlTileSource xmlTileSource;

    public TileStoreLoader(MGMapActivity activity, MGMapApplication application, MGTileStore mgTileStore) throws Exception {
        this.activity = activity;
        this.application = application;
        this.mgTileStore = mgTileStore;
        this.storeDir = mgTileStore.getStoreDir();

        XmlTileSourceConfig config = new XmlTileSourceConfigReader().parseXmlTileSourceConfig(storeDir.getName(), new FileInputStream(new File(storeDir, "config.xml")));
        xmlTileSource = new XmlTileSource(config);
        File sample = new File(storeDir, "sample.curl");
        if (sample.exists()){
            BufferedReader in = new BufferedReader(new FileReader(sample));
            String line = in.readLine();
            in.close();


            String[] parts = line.split(" -H ");
            for (int i=1; i<parts.length;i++){
                String[] subparts = parts[i].replaceAll("'$","").replaceAll("^'","").split(": ");
                if (subparts.length == 2){
                    config.setConnRequestProperty(subparts[0], subparts[1]);
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" \""+subparts[0]+"\"=\""+subparts[1]+"\"");
                }
            }
        }
        File cookiesRef = new File(storeDir, "cookies.ref");
        File cookies = new File(storeDir, "cookies.json");
        if (cookiesRef.exists()){
            activity.requestReadPermissions(); // check (and request if necessary the permissions)

            BufferedReader in = new BufferedReader(new FileReader(cookiesRef));
            String line = in.readLine();
            cookies = new File(line);
        }

        if (cookies.exists()) {
            Map<String, String> cookieMap = new HashMap<>();

            JsonArray cAll = null;
            try {
                FileReader jsonFile = new FileReader(cookies);
                JsonReader jsonReader = Json.createReader(jsonFile);
                JsonObject oAll = jsonReader.readObject();
                cAll = oAll.getJsonArray("cookies");
            } catch (Exception e){
                Log.w(MGMapApplication.LABEL, NameUtil.context()+" "+e.getMessage());
            }
            if (cAll == null){
                try {
                    FileReader jsonFile = new FileReader(cookies);
                    JsonReader jsonReader = Json.createReader(jsonFile);
                    cAll = jsonReader.readArray();
                } catch (Exception e){
                    Log.w(MGMapApplication.LABEL, NameUtil.context()+" "+e.getMessage());
                }
            }
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
            if (cAll != null){
                String separator = "; ";
                String cookieRes = "";
                String[] cookieParts = config.connRequestProperties.get("Cookie").split(separator); // from sample
                for (String cp : cookieParts){
                    String[] subCp = cp.split("=");
                    String subCpValue = cookieMap.get(subCp[0]);
                    cookieRes += subCp[0]+"="+ ((subCpValue==null)?subCp[1]:subCpValue) + separator;
                }
                config.connRequestProperties.put("Cookie", cookieRes.substring(0, cookieRes.length()-separator.length()));
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" cookies.json result: "+config.connRequestProperties.get("Cookie"));
            }

        }
    }



    ArrayList<BgJob> jobs = new ArrayList<>();

    public void loadFromBB(BBox bBox, boolean all){
        int tileSize = mgTileStore.getTileSize();
        for (byte zoomLevel=xmlTileSource.getZoomLevelMin(); zoomLevel<= xmlTileSource.getZoomLevelMax(); zoomLevel++) {
            long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
            int tileXMin = MercatorProjection.pixelXToTileX(MercatorProjection.longitudeToPixelX(bBox.minLongitude, mapSize), zoomLevel, tileSize);
            int tileXMax = MercatorProjection.pixelXToTileX(MercatorProjection.longitudeToPixelX(bBox.maxLongitude, mapSize), zoomLevel, tileSize);
            int tileYMin = MercatorProjection.pixelYToTileY(MercatorProjection.latitudeToPixelY(bBox.maxLatitude, mapSize), zoomLevel, tileSize); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY(MercatorProjection.latitudeToPixelY(bBox.minLatitude, mapSize), zoomLevel, tileSize);
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " " + String.format(Locale.ENGLISH, "dls %d %d %d %d %d", zoomLevel, tileXMin, tileXMax, tileYMin, tileYMax));

            for (int tileX = tileXMin; tileX<= tileXMax; tileX++){
                for (int tileY = tileYMin; tileY<= tileYMax; tileY++) {
                    Tile tile = new Tile(tileX, tileY, zoomLevel, tileSize);
                    if (all || !mgTileStore.containsKey(new Job(tile, false))){
                        jobs.add(  mgTileStore.getLoaderJob(this, tile) );
                    }
                }
            }
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle("Load Tiles for \""+storeDir.getName()+"\"");
        builder.setMessage("Load "+jobs.size()+" tiles?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Log.i(MGMapApplication.LABEL, NameUtil.context() + " do it." );
                application.addBgJobs(jobs);
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                dialog.dismiss();
                Log.i(MGMapApplication.LABEL, NameUtil.context() + " don't do it." );
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
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
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " " + String.format(Locale.ENGLISH, "dls %d %d %d %d %d", zoomLevel, tileXMin, tileXMax, tileYMin, tileYMax));

            if ( ((tileXMax-tileXMin) > 1) && ((tileYMax-tileYMin) >1 )){
                numDrops += (tileXMax-tileXMin-1)*(tileYMax-tileYMin-1);
                jobs.add(  mgTileStore.getDropJob(this, tileXMin, tileXMax, tileYMin, tileYMax, zoomLevel) );
            }
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle("Drop Tiles for \""+storeDir.getName()+"\"");
        builder.setMessage("Drop "+numDrops+" tiles? \nRestart app after finishing action.");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                Log.i(MGMapApplication.LABEL, NameUtil.context() + " do it." );

                if (jobs.size() > 0){
                    application.addBgJobs(jobs);
                }

            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
                dialog.dismiss();
                Log.i(MGMapApplication.LABEL, NameUtil.context() + " don't do it." );
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }


}
