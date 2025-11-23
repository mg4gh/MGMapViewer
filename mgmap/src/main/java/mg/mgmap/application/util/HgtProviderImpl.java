package mg.mgmap.application.util;

import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.view.HgtGridView;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.BgJobGroup;
import mg.mgmap.generic.util.BgJobGroupCallback;
import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.MGLog;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class HgtProviderImpl implements HgtProvider{

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static final String[] HGT_URLS = new String[]{"https://raw.githubusercontent.com/mg4gh/hgtdata/main/docs/%s.zip", "https://step.esa.int/auxdata/dem/SRTMGL1/%s.SRTMGL1.hgt.zip", "http://step.esa.int/auxdata/dem/SRTMGL1/%s.SRTMGL1.hgt.zip"};


    private final MGMapApplication application;
    private final PersistenceManager persistenceManager;
    final AssetManager assetManager;

    /** This contains a list of all hgt files available from ESA or Sonny data */
    private final Properties hgtSize = new Properties();

    private final Handler timer;
    private final LruCache<String, byte[]> hgtCache = new LruCache<>(4){
        @Override
        protected void entryRemoved(boolean evicted, String key, byte[] oldValue, byte[] newValue) {
            super.entryRemoved(evicted, key, oldValue, newValue);
            mgLog.i("drop from LRUCache: key="+key);
        }
    };
    private final Runnable ttCheckHgts = new Runnable() {
        @Override
        public void run() {
            hgtCache.put(UUID.randomUUID().toString(), new byte[0]); // dummy value with "no" space will kick out large buffer, so its reducing memory usage
            timer.postDelayed(ttCheckHgts, 5*60*1000L);
        }
    };


    public HgtProviderImpl(MGMapApplication application, PersistenceManager persistenceManager, AssetManager assetManager){
        this.application = application;
        this.persistenceManager = persistenceManager;
        this.assetManager = assetManager;
        try {
            hgtSize.load(assetManager.open("hgt.properties"));
        } catch (Exception e){
            mgLog.e(e);
        }
        timer = new Handler(Looper.getMainLooper());

    }

    public void loadHgt(MGMapActivity activity, boolean all, ArrayList<String> hgtNames, String layerName, HgtGridView hgtGridView){
        BgJobGroup jobGroup = new BgJobGroup(application, activity, "Download height data", new BgJobGroupCallback() {
            @Override
            public boolean groupFinished(BgJobGroup jobGroup, int total, int success, int fail) {
                if (hgtGridView != null){
                    hgtGridView.requestRedraw();
                }
                hgtCache.evictAll();
                activity.getGraphFactory().clearCache();
                TrackLog mtl = application.markerTrackLogObservable.getTrackLog();
                if (mtl != null){
                    mgLog.d("set mtl changed");
                    mtl.setRoutingProfileId("refresh"); // trigger a recalculation of the whole route
                    application.markerTrackLogObservable.changed();
                }
                return false;
            }
        });
        long downloadSize = 0;
        for (String hgtName : hgtNames){
            if (all || !hgtIsAvailable(hgtName)) {
                BgJob bgJob = new BgJob(){
                    @Override
                    protected void doJob() throws Exception{
                        downloadHgt(hgtName);
                        this.setText("Download "+ hgtName);
                        if (hgtGridView != null){
                            hgtGridView.requestRedraw();
                        }
                    }
                };
                jobGroup.addJob(bgJob);
                downloadSize += hgtSize(hgtName);
            }
        }
        if (jobGroup.size() > 0){
            String msg = String.format(Locale.ENGLISH,"Download %d hgt files [%.1fMB] %s?", jobGroup.size(),downloadSize/1000000.0f,(layerName==null)?"":" for "+layerName);
            jobGroup.setConstructed(msg);
        }
    }
    public void dropHgt(AppCompatActivity activity, ArrayList<String> hgtNames, HgtGridView hgtGridView){
        if (hgtGridView != null) {
            BgJobGroup jobGroup = new BgJobGroup(application, activity,"Drop hgt files", new BgJobGroupCallback() {
                @Override
                public boolean groupFinished(BgJobGroup jobGroup, int total, int success, int fail) {
                    hgtGridView.requestRedraw();
                    return false;
                }
            });
            for (String hgtName : hgtNames){
                if (hgtIsAvailable(hgtName)){
                    jobGroup.addJob(new BgJob(){
                        @Override
                        protected void doJob(){
                            dropHgt(hgtName);
                        }
                    });
                }
            }
            jobGroup.setConstructed("Drop "+jobGroup.size()+" hgt files?");
        }
    }

    public void clearCache(){
        hgtCache.evictAll();
    }
    public void cleanup(){
        clearCache();
        timer.removeCallbacks(ttCheckHgts);
    }

    private boolean hgtExists(String hgtName){
        return hgtSize.containsKey(hgtName);
    }

    public long hgtSize(String hgtName){
        Object oHgtSize = hgtSize.get(hgtName);
        if (oHgtSize == null){
            return 0;
        }
        String sHgtSize = oHgtSize.toString();
        float fValue = Float.parseFloat(sHgtSize.substring(0,sHgtSize.length()-1));
        if (sHgtSize.endsWith("K")){
            fValue *= 1000;
        }
        if (sHgtSize.endsWith("M")){
            fValue *= 1000 * 1000;
        }
        return (long)fValue;
    }
    public boolean hgtIsAvailable(String hgtName){
        return persistenceManager.getHgtFile(hgtName).exists();
    }


    public void downloadHgt(String hgtName) throws Exception{ // assume it exists
        if (hgtExists(hgtName)){
            boolean success = false;
            for (String urlPattern : HGT_URLS){
                String url = String.format(urlPattern,hgtName);
                if (downloadHgtFromUrl(url, hgtName)){
                    mgLog.i("Successful download from "+url);
                    success = true;
                    break;
                } else {
                    mgLog.i("Unsuccessful download from "+url);
                }
            }
            if (!success){
                throw new Exception("Download of hgt failed (see reason above).");
            }
        } else {
            IOUtil.copyStreams(assetManager.open("empty.hgt"), persistenceManager.openHgtOutput(hgtName));
        }
    }

    private boolean downloadHgtFromUrl(String url, String hgtName){
        try {
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .build();
            Request request = new Request.Builder().url(url).build();
            try (Response response = client.newCall(request).execute()){
                ResponseBody responseBody = response.body();
                if (responseBody.contentLength() < 1000) return false;
                IOUtil.copyStreams(responseBody.byteStream(), persistenceManager.openHgtOutput(hgtName));
                return true;
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
        return false;
    }

    public void dropHgt(String hgtName){
        persistenceManager.dropHgt(hgtName);
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
