package mg.mgmap.application.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;

public class HgtProvider2 implements HgtProvider{

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final HashMap<String, HgtBuffer> hgtCache;
    public Pref<Boolean> useSB = new Pref<>(false);

    public HgtProvider2(){
        hgtCache = new HashMap<>();
    }

    public synchronized HgtBuffer getHgtBuf(String hgtName) { // assume hgt file exists
        HgtBuffer hgtBuffer;
        synchronized (this){
            hgtBuffer = hgtCache.get(hgtName);
            if (hgtBuffer == null){
                hgtBuffer = getHgtBufInternal(hgtName);
                hgtCache.put(hgtName, hgtBuffer);
            }
        }
        return hgtBuffer;
    }

    public HgtBuffer getHgtBufInternal(String hgtName) { // assume hgt file exists
        HgtBuffer buf = null;
        try {
//            File hgtFile = persistenceManager.getHgtFile(hgtName);
            File hgtFile = new File("src/test/assets/map_local/"+hgtName+".SRTMGL1.hgt.zip");
            if (hgtFile.exists()){
                if (hgtFile.length()>0){
                    if (hgtFile.getName().endsWith("zip")){
                        try (ZipFile zipFile = new ZipFile(hgtFile)){
                            ZipEntry zipEntry = zipFile.getEntry(hgtName+".hgt");
                            InputStream is = zipFile.getInputStream(zipEntry);
                            buf = useSB.getValue()? new HgtBufferImplSB(hgtName, is) : new HgtBufferImplBA(hgtName,is);
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
