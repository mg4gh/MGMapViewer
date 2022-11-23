package mg.mgmap.application.util;

import android.content.res.AssetManager;
import android.os.Handler;
import android.util.Log;
import android.util.LruCache;

import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.NameUtil;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HgtProvider {

    public static final String HGT_URL = "http://step.esa.int/auxdata/dem/SRTMGL1/";


    private final PersistenceManager persistenceManager;
    AssetManager assetManager;

    private final Properties hgtSize = new Properties();

    private final Handler timer = new Handler();
    private final LruCache<String, byte[]> hgtCache = new LruCache<String, byte[]>(4){
        @Override
        protected void entryRemoved(boolean evicted, String key, byte[] oldValue, byte[] newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" drop from LRUCache: key="+key);
        }
    };
    private final Runnable ttCheckHgts = new Runnable() {
        @Override
        public void run() {
            hgtCache.put(UUID.randomUUID().toString(), new byte[0]); // dummy value with "no" space will kick out large buffer, so its reducing memory usage
            timer.postDelayed(ttCheckHgts, 5*60*1000L);
        }
    };


    public HgtProvider(PersistenceManager persistenceManager, AssetManager assetManager){
        this.persistenceManager = persistenceManager;
        this.assetManager = assetManager;
        try {
            hgtSize.load(assetManager.open("hgt.properties"));
        } catch (Exception e){
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }

    }

    private boolean hgtExists(String hgtName){
        return hgtSize.containsKey(hgtName);
    }
    public boolean hgtIsAvailable(String hgtName){
        return persistenceManager.hgtIsAvailable(hgtName);
    }


    public void downloadHgt(String hgtName){ // assume it exists
        try {
            if (hgtExists(hgtName)){
                String url = HGT_URL + persistenceManager.getHgtFilename(hgtName);
                OkHttpClient client = new OkHttpClient().newBuilder()
                        .build();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    Log.w (MGMapApplication.LABEL, NameUtil.context()+" empty response body for download!");
                } else {
                    if (responseBody.contentLength() < 1000) throw new Exception("Invalid hgt size: "+responseBody.contentLength());
                    IOUtil.copyStreams(responseBody.byteStream(), persistenceManager.openHgtOutput(hgtName));
                }
            } else {
                IOUtil.copyStreams(assetManager.open("empty.hgt"), persistenceManager.openHgtOutput(hgtName));
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
    }

    public void dropHgt(String hgtName){
        persistenceManager.dropHgt(hgtName);
    }

    public String getHgtName(int iLat, int iLon) {
        return String.format(Locale.GERMANY, "%s%02d%S%03d", (iLat > 0) ? "N" : "S", iLat, (iLon > 0) ? "E" : "W", iLon);
    }


    public synchronized byte[] getHgtBuf(String hgtName) { // assume hgt file exists
        timer.removeCallbacks(ttCheckHgts);
        timer.postDelayed(ttCheckHgts, 5*60*1000L);
        byte[] buf;
        synchronized (this){
            buf = hgtCache.get(hgtName);
            if (buf == null){
                buf = persistenceManager.getHgtBuf(hgtName);
                hgtCache.put(hgtName, buf);
            }
        }
        return buf;
    }

}
