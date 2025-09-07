package mg.mgmap.application.util;

import android.util.LruCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import mg.mgmap.generic.util.basic.MGLog;

public class HgtProvider2 {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private HashMap<String, byte[]> hgtCache;

    public HgtProvider2(){
        hgtCache = new HashMap<>();
    }

    public byte[] getHgtBuf(String hgtName){
        byte[] buf;
        synchronized (this){
            buf = hgtCache.get(hgtName);
            if (buf == null){
                try {
                    File hgtFile = new File("src/test/assets/map_local/"+hgtName+".SRTMGL1.hgt.zip");
                    System.out.println("load "+hgtFile.getAbsolutePath());
                    if (hgtFile.exists()){
                        if (hgtFile.length()>0){
                            if (hgtFile.getName().endsWith("zip")){
                                try (ZipFile zipFile = new ZipFile(hgtFile)){
                                    ZipEntry zipEntry = zipFile.getEntry(hgtName+".hgt");
                                    buf = readHgtData( zipFile.getInputStream(zipEntry) );
                                }
                            } else {
                                try (FileInputStream is = new FileInputStream(hgtFile)){
                                    buf = readHgtData( is );
                                }
                            }
                        } else { // is dummy hgt file
                            buf = new byte[1];
                        }
                    }
                } catch (IOException e) { // should not happen
                    mgLog.e(e);
                    buf = new byte[0]; // but if so, prevent accessing inconsistent data
                }
                hgtCache.put(hgtName, buf);
            }
        }
        return buf;
    }

    private byte[] readHgtData(InputStream is) throws IOException{
        int todo = is.available();
        byte[] buf = new byte[todo];
        int done = 0;
        while (todo > 0) {
            int step = is.read(buf, done, todo);
            todo -= step;
            done += step;
        }
        return buf;
    }

}
