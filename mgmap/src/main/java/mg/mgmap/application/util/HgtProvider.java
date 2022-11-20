package mg.mgmap.application.util;

import android.content.res.AssetManager;
import android.os.Handler;
import android.util.Log;

import java.util.Locale;
import java.util.Properties;
import java.util.TreeSet;

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

    Properties hgtSize = new Properties();

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


    TreeSet<CachedHgtBuf> cachedHgtBufs = new TreeSet<>();
    long hgtBufTimeout = 60000; // cleanup hgtBufs, if they are not accessed for that time (since buffers are rather large)
    Handler timer = new Handler();
    Runnable ttCheckHgts = () -> {
        long now = System.currentTimeMillis();
        for (CachedHgtBuf hgtBuf : new TreeSet<>(cachedHgtBufs)){
            if ( (now - hgtBuf.lastAccess) > hgtBufTimeout ){ // drop, if last access is over a given threshold
                cachedHgtBufs.remove(hgtBuf);
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" drop "+hgtBuf.hgtName +" remaining hgtBufs.size="+ cachedHgtBufs.size());
            }
        }
    };

    public String getHgtName(int iLat, int iLon) {
        return String.format(Locale.GERMANY, "%s%02d%S%03d", (iLat > 0) ? "N" : "S", iLat, (iLon > 0) ? "E" : "W", iLon);
    }



    public synchronized byte[] getHgtBuf(String hgtName){ // assume hgt file exists
        CachedHgtBuf cachedHgtBuf = getCachedHgtBuf(hgtName);

        if (cachedHgtBuf != null) { // ok, exists already
            cachedHgtBufs.remove(cachedHgtBuf);
            cachedHgtBuf.accessNow();
        } else {
            if (cachedHgtBufs.size() >= 4){ // if cache contains already 4 bufs, remove last one
                cachedHgtBufs.pollLast();
            }
            cachedHgtBuf = new CachedHgtBuf(hgtName);
            cachedHgtBuf.buf = persistenceManager.getHgtBuf(hgtName);
        }
        cachedHgtBufs.add(cachedHgtBuf);
        return cachedHgtBuf.buf;
    }

    private CachedHgtBuf getCachedHgtBuf(String name){
        for (CachedHgtBuf hgtBuf : cachedHgtBufs){
            if (hgtBuf.getHgtName().equals(name)){
                return hgtBuf;
            }
        }
        return null;
    }

    private class CachedHgtBuf implements Comparable<CachedHgtBuf>{
        String hgtName;
        byte[] buf = null;
        long lastAccess;

        private CachedHgtBuf(String hgtName){
            this.hgtName = hgtName;
            accessNow();
        }
        private void accessNow(){
            lastAccess = System.currentTimeMillis();
            timer.removeCallbacks(ttCheckHgts);
            timer.postDelayed(ttCheckHgts, hgtBufTimeout);
        }
        private String getHgtName(){
            return hgtName;
        }

        @Override
        public int compareTo(CachedHgtBuf o) {
            int res = Long.compare(lastAccess,o.lastAccess);
            if (res == 0){
                res = hgtName.compareTo(o.hgtName);
            }
            return res;
        }
    }

}
