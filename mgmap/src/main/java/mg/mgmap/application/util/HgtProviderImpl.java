package mg.mgmap.application.util;

import android.content.res.AssetManager;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.view.HgtGridView;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.BgJobGroup;
import mg.mgmap.generic.util.BgJobGroupCallback;
import mg.mgmap.generic.util.Pref;
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
    Pref<Boolean> useSB;

    /** This contains a list of all hgt files available from ESA or Sonny data */
    private final Properties hgtSize = new Properties();

    private final Handler timer;
//    private final LruCache<String, HgtBuffer> hgtCache;
    // Instead of using a LruCache now each hgt buffer will be supervised and dropped out, if access time is above 30s
    private final ArrayList<HgtBuffer> hgtBuffers = new ArrayList<>();
    private final ArrayList<HgtBuffer> hgtBuffers2Delete = new ArrayList<>();

    private final Runnable ttCheckHgts = new Runnable() {
        @Override
        public void run() {
            if (hgtBuffers.isEmpty()){
                timer.postDelayed(ttCheckHgts, 35*1000L);
            } else {
                for (HgtBuffer hgtBuffer : hgtBuffers){
                    long now = System.nanoTime();
                    long access = hgtBuffer.getAccessNanos();
                    if ( (now-access) >= 30_000_000_000L){
                        hgtBuffers2Delete.add(hgtBuffer);
                    }
                }
                applyHgtBuffersToDelete();
                timer.postDelayed(ttCheckHgts, 5*1000L);
            }
        }
    };

    void applyHgtBuffersToDelete(){
        if (!hgtBuffers2Delete.isEmpty()){
            synchronized (this){
                hgtBuffers.removeAll(hgtBuffers2Delete);
                for (HgtBuffer hgtBuffer : hgtBuffers2Delete){
                    hgtBuffer.cleanup();
                }
            }
            hgtBuffers2Delete.clear();
        }
    }


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
        timer.postDelayed(ttCheckHgts, 60*1000L);
        useSB = application.getPrefCache().get("HgtProvider.useSB", false);
//        hgtCache = new LruCache<>(4){
//            @Override
//            protected void entryRemoved(boolean evicted, String key, HgtBuffer oldValue, HgtBuffer newValue) {
//                super.entryRemoved(evicted, key, oldValue, newValue);
//                mgLog.i("drop from LRUCache: key="+key);
//            }
//        };
    }

    public void loadHgt(MGMapActivity activity, boolean all, ArrayList<String> hgtNames, String layerName, HgtGridView hgtGridView){
        BgJobGroup jobGroup = new BgJobGroup(application, activity, "Download height data", new BgJobGroupCallback() {
            @Override
            public boolean groupFinished(BgJobGroup jobGroup, int total, int success, int fail) {
                if (hgtGridView != null){
                    hgtGridView.requestRedraw();
                }
                clearCache();
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
        hgtBuffers2Delete.addAll(hgtBuffers);
        applyHgtBuffersToDelete();
    }

    public synchronized void cleanup(){
        timer.removeCallbacks(ttCheckHgts);
        clearCache();
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

    public synchronized HgtBuffer getHgtBuf(String hgtName) { // assume hgt file exists
        HgtBuffer hgtBuffer;
        synchronized (this){
            for (HgtBuffer aHgtBuffer : hgtBuffers){
                if (hgtName.equals( aHgtBuffer.getName() )) return aHgtBuffer;
            }
            hgtBuffer = getHgtBufInternal(hgtName);
            hgtBuffers.add(hgtBuffer);
        }
        return hgtBuffer;
    }

    public HgtBuffer getHgtBufInternal(String hgtName) { // assume hgt file exists
        HgtBuffer buf = null;
        try {
            File hgtFile = persistenceManager.getHgtFile(hgtName);
            if (hgtFile.exists()){
                if (hgtFile.length()>0){
                    if (hgtFile.getName().endsWith("zip")){
                        try (ZipFile zipFile = new ZipFile(hgtFile)){
                            ZipEntry zipEntry = zipFile.getEntry(hgtName+".hgt");
                            InputStream is = zipFile.getInputStream(zipEntry);
                            buf = useSB.getValue()? new HgtBufferImplSB(hgtName,is) : new HgtBufferImplBA(hgtName,is);
                        }
                    } else {
                        try (FileInputStream is = new FileInputStream(hgtFile)){
                            buf = useSB.getValue()? new HgtBufferImplSB(hgtName,is) : new HgtBufferImplBA(hgtName,is);
                        }
                    }
                } else { // is dummy hgt file
                    buf = new HgtBufferImplSea(hgtName);
                }
            }
        } catch (IOException e) { // should not happen
            mgLog.e(e);
            buf = null; // but if so, prevent accessing inconsistent data
        }
        return buf;
    }


}
