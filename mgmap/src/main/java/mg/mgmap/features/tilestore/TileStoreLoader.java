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
package mg.mgmap.features.tilestore;

import android.app.AlertDialog;
import android.content.DialogInterface;

import android.util.Log;

import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.queue.Job;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import mg.mgmap.MGMapActivity;
import mg.mgmap.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.model.BBox;
import mg.mgmap.util.BgJob;
import mg.mgmap.util.BgJobUtil;
import mg.mgmap.util.FullscreenUtil;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.Pref;
import mg.mgmap.util.TermuxUtil;

public class TileStoreLoader {

    public File storeDir;
    MGMapApplication application;
    MGMapActivity activity;
    MGTileStore mgTileStore;
    int jobCounter = 0;
    int errorCounter = 0;
    int successCounter = 0;

    public XmlTileSource xmlTileSource;

    public TileStoreLoader(MGMapActivity activity, MGMapApplication application, MGTileStore mgTileStore) throws Exception {
        this.activity = activity;
        this.application = application;
        this.mgTileStore = mgTileStore;
        this.storeDir = mgTileStore.getStoreDir();
        init();
    }

    private void init() throws Exception {
        XmlTileSourceConfig config = new XmlTileSourceConfigReader().parseXmlTileSourceConfig(storeDir.getName(), new FileInputStream(new File(storeDir, "config.xml")));
        xmlTileSource = new XmlTileSource(config);
        try {
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
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
        errorCounter = 0;
        successCounter = 0;
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

        String title = "Load Tiles for \""+storeDir.getName()+"\"";
        String message = "Load "+jobs.size()+" tiles?";
        jobCounter = jobs.size();
        new BgJobUtil(activity, application).processConfirmDialog(title, message, jobs);
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

        String title = "Drop Tiles for \""+storeDir.getName()+"\"";
        String message = "Drop "+numDrops+" tiles?";
        jobCounter = jobs.size();
        new BgJobUtil(activity, application).processConfirmDialog(title, message, jobs);
    }

    synchronized void jobFinished(boolean success, Exception e){
        if (success){
            successCounter++;
        } else {
            errorCounter++;
        }
        String message = "successCounter="+successCounter+"  errorCounter="+errorCounter+"  jobCounter="+jobCounter;
        Log.d(MGMapApplication.LABEL, NameUtil.context()+" "+message);
        if (successCounter + errorCounter == jobCounter){
            new Thread(){
                @Override
                public void run() {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            reportResult();
                        }
                    });
                }
            }.start();
        }
    }

    void reportResult(){
        if (successCounter > 0){
            activity.getPrefCache().get(R.string.FSPosition_pref_RefreshMapView, false).toggle(); //after TileDownloads this helps to make downloaded tiles visible
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle("Job report for \""+storeDir.getName()+"\"");
        String message = "Number of jobs: "+jobCounter+"\nSuccessful finished: "+successCounter+"\nUnsuccessful finished:"+errorCounter;
        builder.setMessage(message);

        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " ok" );
        });

        if ((errorCounter == jobCounter) && (new File(storeDir, "cookies.json").exists()) ){
            if ((jobs.size()>0) && (jobs.get(0) instanceof MGTileStoreLoaderJob)){
                builder.setNegativeButton("Retry", (dialog, which) -> {
                    Log.i(MGMapApplication.LABEL, NameUtil.context() + " Retry" );
                    new TermuxUtil(activity).runCommand("cookies.sh");
                    Pref<String> ps = activity.getPrefCache().get(R.string.activity_param_key, "");
                    ps.deleteObservers();
                    ps.addObserver(new Observer() {
                        @Override
                        public void update(Observable o, Object arg) {
                            try {
                                Log.i(MGMapApplication.LABEL, NameUtil.context() + " process termux callback" );
                                handleTermuxCallback(ps.getValue());
                                init();
                                application.addBgJobs(jobs);
                            } catch (Exception e) {
                                Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
                            }
                        }
                    });

                    dialog.dismiss();
                });
            }
        }

        AlertDialog alert = builder.create();
        alert.show();
    }

    void handleTermuxCallback(String argss) throws Exception{
        if ((argss != null) && (argss.length() > 10)){
            String[] args = argss.split(":");
            String nl = System.lineSeparator();
            boolean checkOk = true;
            StringBuilder sb = new StringBuilder("[").append(nl);
            for (String arg : args){
                String[] argt = arg.split("=");
                if (argt.length == 2){
                    if ((argt[0].length() > 0)  && (argt[1].length() > 0)){
                        sb.append("{").append(nl);
                        sb.append("    \"Name raw\": \"").append(argt[0]).append("\",").append(nl);
                        sb.append("    \"Content raw\": \"").append(argt[1]).append("\"").append(nl);
                        sb.append("}");
                        if (!(args[args.length-1].equals(arg))) sb.append(","); // if not the last
                        sb.append(nl);
                    } else{
                        checkOk = false;
                    }
                } else {
                    checkOk = false;
                }
            }
            sb.append("]").append(nl);

            if (checkOk){
                PrintWriter pw = new PrintWriter(new File(storeDir, "cookies.json"));
                pw.println(sb.toString());
                pw.close();
            }
        }
    }
}
