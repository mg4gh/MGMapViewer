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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.layer.queue.Job;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import mg.mgmap.generic.util.KeyboardUtil;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.DialogView;

@SuppressWarnings("IOStreamConstructor")
public class TileStoreLoader {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public final File storeDir;
    final MGMapApplication application;
    final MGMapActivity activity;
    final MGTileStore mgTileStore;

    public XmlTileSource xmlTileSource;
    BgJobGroup jobGroup;

    private class TimerTaskEnableDone implements Runnable{
        final DialogView dialogView;
        final Handler timer;
        private TimerTaskEnableDone(DialogView dialogView, Handler timer){
            this.dialogView = dialogView;
            this.timer = timer;
        }
        @Override
        public void run() {
            String cookies = CookieManager.getInstance().getCookie(xmlTileSource.config.cookiesDomain);
            mgLog.d("cookies="+cookies);
            boolean check = checkRequiredCookies(cookies);
            boolean enableWorked = dialogView.setEnablePositive(check);
            if (!check && enableWorked){
                timer.postDelayed(this, 1000);
            }
        }
    }

    public TileStoreLoader(MGMapActivity activity, MGMapApplication application, MGTileStore mgTileStore, final Handler timer) throws Exception {
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
                return (fail == total) && (xmlTileSource.config.cookiesDomain != null) && allowRetry;
            }

            @SuppressLint("SetJavaScriptEnabled")
            @Override
            public void retry(BgJobGroup jobGroup) {
                allowRetry = false;
                try {
                    CookieManager.getInstance().removeAllCookies(null);
                    DialogView dialogView = activity.findViewById(R.id.dialog_parent);
                    WebView myWebView = new WebView(activity);
                    myWebView.getSettings().setJavaScriptEnabled(true);
                    myWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

                    myWebView.setWebViewClient(new WebViewClient(){
                        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                            if (request.getUrl().toString().contains(xmlTileSource.config.cookiesDomain)) {
                                return false; // stay within webView
                            }
                            // Otherwise, open default browser to handle URLs.
                            Intent intent = new Intent(Intent.ACTION_VIEW, request.getUrl());
                            activity.startActivity(intent);
                            return true;
                        }
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            super.onPageFinished(view, url);
                            // this might be used, if autofill fields are renamed
                            // view.saveWebArchive(new File(application.getPersistenceManager().getLogDir(),"abc_"+System.currentTimeMillis()+".war").getAbsolutePath());
                            mgLog.d(url);
                        }
                    });
                    myWebView.loadUrl(xmlTileSource.config.cookiesURL);
                    dialogView.lock(() -> dialogView
                            .setTitle("Create cookies")
                            .setContentView(myWebView)
                            .setLogPrefix("tsl")
                            .setPositive("Done", evt -> {
                                String cookies = CookieManager.getInstance().getCookie(xmlTileSource.config.cookiesDomain);
                                saveCookiesAndInit(cookies);
                                jobGroup.doit(); // this is the real retry
                            })
                            .setNegative( "Abort", null)
                            .setNeutral("Fill",evt ->{
                                // This approach did not work - due to recaptcha protection
//                                for (XmlTileSourceConfig.AutoFill autoFill : xmlTileSource.config.autoFills){
//                                    myWebView.loadUrl("javascript:(function() { document.getElementsByName('"+autoFill.id()+"')[0].value = '" + autoFill.value() + "'; ;})()");
//                                }

                                // This approach was working, but could easily detected as automation
//                                XmlTileSourceConfig.AutoFill autoFill = xmlTileSource.config.autoFills.get(cnt);
//                                myWebView.loadUrl("javascript:(function() { document.getElementsByName('"+autoFill.id()+"')[0].focus(); })()");
//
//                                KeyCharacterMap mKeyCharacterMap =
//                                        KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
//                                KeyEvent[] kea = mKeyCharacterMap.getEvents(autoFill.value().toCharArray());
//                                for (KeyEvent ke : kea){
//                                    activity.dispatchKeyEvent(ke);
//                                }

                                // better approach with typing each letter by its own
                                new AutoFiller(activity, myWebView, xmlTileSource.config.autoFills).start();
                            })
                            .setMaximize(true)
                            .show());

                    dialogView.setEnablePositive(false);

                    timer.postDelayed(new TimerTaskEnableDone(dialogView, timer), 200);
                } catch (Exception e) {
                    mgLog.e(e);
                }

            }
        });
    }

    Handler timer = new Handler();
    public class AutoFiller {
        private final Activity activity;
        private final WebView webView;
        ArrayList<XmlTileSourceConfig.AutoFill> autofills;
        private int cnt = 0;
        private String value = "";
        KeyCharacterMap mKeyCharacterMap;

        public AutoFiller(Activity activity, WebView webView, ArrayList<XmlTileSourceConfig.AutoFill> autofills) {
            this.activity = activity;
            this.webView = webView;
            this.autofills = autofills;
            mKeyCharacterMap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        }
        Runnable runnable = null;
        public void start(){
            runnable = () -> {
                if (value.isEmpty()) {
                    if (cnt < autofills.size()){
                        XmlTileSourceConfig.AutoFill autoFill = autofills.get(cnt++);
                        webView.loadUrl("javascript:(function() { document.getElementsByName('"+ autoFill.id()+"')[0].focus(); })()");
                        value = autoFill.value();
                        timer.postDelayed(runnable, 100);
                    } else {
                        KeyboardUtil.hideKeyboard(activity);
                        mgLog.d("AutoFill finished");
                    }
                } else {
                    String s = value.substring(0,1);
                    value = value.substring(1);
                    KeyEvent[] kea = mKeyCharacterMap.getEvents(s.toCharArray());
                    for (KeyEvent ke : kea){
                        activity.dispatchKeyEvent(ke);
                    }
                    timer.postDelayed(runnable, 50);
                }
            };
            timer.postDelayed(runnable, 10);
        }
    }

    private void saveCookiesAndInit(String cookies){
        mgLog.d( cookies);
        String nl = System.lineSeparator();
        StringBuilder sb = new StringBuilder("[").append(nl);
        String[] cookiesA = cookies.split("; ");
        for (int i=0; i<cookiesA.length; i++){
            String[] part = cookiesA[i].split("=");
            sb.append("{").append(nl);
            sb.append("    \"Name raw\": \"").append(part[0]).append("\",").append(nl);
            sb.append("    \"Content raw\": \"").append(part[1]).append("\"").append(nl);
            sb.append("}");
            if ( (i+1) < cookiesA.length ) sb.append(","); // if not the last
            sb.append(nl);
        }
        sb.append("]").append(nl);
        try (FileOutputStream fos = new FileOutputStream(new File(storeDir, "cookies.json"))){
            PrintWriter pw = new PrintWriter(fos);
            pw.println(sb);
            pw.close();
            init(); // rerun init to reflect content of new cookies in config.connRequestProperties
        } catch (Exception e){ mgLog.e(e);}
    }

    private boolean checkRequiredCookies(String cookies){
        if (xmlTileSource.config.cookiesRequired == null) return true;
        List<String> requiredList = new ArrayList<>( List.of(xmlTileSource.config.cookiesRequired));
        if (cookies != null){
            String[] cookiesA = cookies.split("; ");
            for (String s : cookiesA) {
                String[] part = s.split("=");
                requiredList.remove(part[0]);
            }
        }
        return requiredList.isEmpty();
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
