/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mapviewer.util;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;

/**
 * Covers all handling towards the file system.
 */
public class PersistenceManager {

    private static PersistenceManager persistenceManager = null;
    private static File baseDir = null;


    public static PersistenceManager getInstance(Context context) {
        init(context);
        synchronized (PersistenceManager.class) {
            if (persistenceManager == null) {
                if (baseDir == null) throw new InvalidParameterException();
                persistenceManager = new PersistenceManager();
            }
        }
        return persistenceManager;
    }

    public static PersistenceManager getInstance() {
        Assert.check (persistenceManager != null);
        Assert.check (baseDir != null);
        return persistenceManager;
    }


    private File trackMetaDir;
    private File trackGpxDir;
    private File mapsDir;
    private File mapsMapsforgeDir;

    private File themesDir;
    private File hgtDir;
    private File logDir;
    private File configDir;
    private File searchConfigDir;
    private File apkDir;

    private File fRaw;
    private FileOutputStream fosRaw = null;


    synchronized private static void init(Context context){
        if (baseDir == null){
//            boolean bPrefStorage = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preferences_storage_key), false);
//            baseDir = Environment.getExternalStorageDirectory();
//            if (baseDir.canWrite() && bPrefStorage){
//            } else {
            baseDir = context.getExternalFilesDir(null);

//            }
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " Storage: "+baseDir.getAbsolutePath());
        }
    }

    private PersistenceManager() {
        File appDir = createIfNotExists(baseDir, "MGMapViewer");
        File trackDir = createIfNotExists(appDir, "track");
        trackMetaDir = createIfNotExists(trackDir, "meta");
        trackGpxDir = createIfNotExists(trackDir, "gpx");
        File trackRecDir = createIfNotExists(trackDir, "recording");
        fRaw = new File(trackRecDir, "raw.dat");

        mapsDir = createIfNotExists(appDir, "maps");
        mapsMapsforgeDir = createIfNotExists(mapsDir, "mapsforge");
        createIfNotExists(mapsDir, "mapstores");
        createIfNotExists(mapsDir, "maponline");
        createIfNotExists(mapsDir, "mapgrid");
        themesDir = createIfNotExists(appDir, "themes");
        hgtDir = createIfNotExists(appDir, "hgt");
        logDir = createIfNotExists(appDir, "log");
        configDir = createIfNotExists(appDir, "config");
        searchConfigDir = createIfNotExists(configDir, "search");
        createFileIfNotExists(searchConfigDir, "POI.cfg");
        createFileIfNotExists(searchConfigDir, "Nominatim.cfg");
        apkDir = createIfNotExists(appDir, "apk");
    }

    public File getLogDir(){
        return logDir;
    }
    public File getMapsDir(){
        return mapsDir;
    }
    public File getMapsforgeDir(){
        return mapsMapsforgeDir;
    }
    public File getConfigDir(){
        return configDir;
    }
    public File getTrackGpxDir(){
        return trackGpxDir;
    }

    public File getApkDir(){
        return apkDir;
    }
    public void cleanApkDir(){
        try {
            for (File file : apkDir.listFiles()){
                file.delete();
            }
        } catch (Exception e) { }
    }
    public File getApkFile(){
        String[] apkNames = apkDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".apk");
            }
        });
        if (apkNames.length > 0){
            return new File(apkDir, apkNames[0]);
        }
        return null;
    }

    public File createIfNotExists(File parent, String subDir) {
        File f = new File(parent, subDir);
        synchronized (parent) {
            if (!f.exists()) {
                f.mkdir();
            }
        }
        if (!f.exists()) {
            Log.e(MGMapApplication.LABEL, NameUtil.context()+" Failed to create directory: " + f.getAbsolutePath());
            throw new RuntimeException("Failed to create directory: " + f.getAbsolutePath());
        }
        return f;
    }
    public void createFileIfNotExists(File parent, String file) {
        File f = new File(parent, file);
        try {
            synchronized (parent) {
                if (!f.exists()) {
                    f.createNewFile();
                }
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
    }

    public void recordRaw(byte[] b, int offset, int length) {
        try {
            if (fosRaw == null) {
                fosRaw = new FileOutputStream(fRaw, true);
            }
            fosRaw.write(b, offset, length);
            fosRaw.flush();
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
    }

    public void clearRaw() {
        try {
            if (fosRaw != null) {
                fosRaw.close();
                fosRaw = null;
            }
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
        if (fRaw.exists()) {
            fRaw.delete();
        }
    }

    public byte[] getRawData() {
        try {
            if (fRaw.exists()) {
                FileInputStream fis = new FileInputStream(fRaw);
                byte[] b = new byte[fis.available()];
                fis.read(b);
                return b;
            }
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
        return null;
    }


    public boolean existsGpx(String filename) {
        File file = new File(trackGpxDir, filename + ".gpx");
        return file.exists();
    }
    public PrintWriter openGpxOutput(String filename) {
        try {
            File file = new File(trackGpxDir, filename + ".gpx");
            return new PrintWriter(file);
        } catch (FileNotFoundException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }
    public InputStream openGpxInput(String filename) {
        try {
            File file = new File(trackGpxDir, filename + ".gpx");
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }
    public Uri getGpxUri(String filename) {
        try {
            File file = new File(trackGpxDir, filename + ".gpx");
            return Uri.fromFile(file);
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }

    public FileOutputStream openMetaOutput(String filename) {
        try {
            File file = new File(trackMetaDir, filename + ".meta");
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }

    public FileInputStream openMetaInput(String filename) {
        try {
            File file = new File(trackMetaDir, filename + ".meta");
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }

    public ArrayList<String> getGpxNames() {
        ArrayList<String> names = new ArrayList<>();
        for (String name : trackGpxDir.list()){
            names.add( name.replace(".gpx","") );
        }
        return names;
    }
    public ArrayList<String> getMetaNames() {
        ArrayList<String> names = new ArrayList<>();
        for (String name : trackMetaDir.list()){
            names.add( name.replace(".meta","") );
        }
        return names;
    }

    public File getThemesDir(){
        return themesDir;
    }
    public String[] getThemeNames() {
        return themesDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                if (!s.endsWith(".xml")) return false;
                return !(new File(file,s).isDirectory());
            }
        });
    }


    TreeSet<HgtBuf> hgtBufs = new TreeSet<>();
    long hgtBufTimeout = 30000; // cleanup hgtBufs, if they are not accessed for that time (since buffers are rather large)
    Handler timer = new Handler();
    Runnable ttCheckHgts = new Runnable() {
        @Override
        public void run() {
            long now = System.currentTimeMillis();
            for (HgtBuf hgtBuf : new TreeSet<>(hgtBufs)){
                if ( (now - hgtBuf.lastAccess) > hgtBufTimeout ){ // drop, if last access is over a given threshold
                    hgtBufs.remove(hgtBuf);
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" drop "+hgtBuf.name+" remaining hgtBufs.size="+hgtBufs.size());
                }
            }
        }
    };

    public byte[] getHgtBuf(int iLat, int iLon){
        byte[] buf = null;
        String file = String.format(Locale.GERMANY, "N%02dE%03d",iLat,iLon);
        HgtBuf hgtBuf = getHgtBuf(file);

        if (hgtBuf != null) { // ok, exists already
            buf = hgtBuf.buf;
            hgtBufs.remove(hgtBuf);
            hgtBuf.accessNow();
        } else {
            if (hgtBufs.size() >= 4){ // if cache contains already 4 bufs, remove last one
                hgtBufs.pollLast();
            }

            hgtBuf = new HgtBuf(file);
            File hgtFile = new File(hgtDir, file+".SRTMGL1.hgt.zip");
            if (hgtFile.exists()){
                try {
                    ZipFile zipFile = new ZipFile(hgtFile);
                    ZipEntry zipEntry = zipFile.getEntry(file+".hgt");
                    InputStream zis = zipFile.getInputStream(zipEntry);
                    int todo = zis.available();
                    hgtBuf.buf = new byte[todo];
                    int done = 0;
                    while (todo > 0) {
                        int step = zis.read(hgtBuf.buf, done, todo);
                        todo -= step;
                        done += step;
                    }
                    zipFile.close();
                } catch (IOException e) { // should not happen
                    e.printStackTrace();
                    hgtBuf.buf = null; // but if so, prevent accessing inconsistent data
                }
            }
        }
        hgtBufs.add(hgtBuf);
        return buf;
    }

    private HgtBuf getHgtBuf(String name){
        for (HgtBuf hgtBuf : hgtBufs){
            if (hgtBuf.getName().equals(name)){
                return hgtBuf;
            }
        }
        return null;
    }

    private class HgtBuf implements Comparable<HgtBuf>{
        String name;
        byte[] buf = null;
        long lastAccess;

        private HgtBuf(String name){
            this.name = name;
            accessNow();
        }
        private void accessNow(){
            lastAccess = System.currentTimeMillis();
            timer.removeCallbacks(ttCheckHgts);
            timer.postDelayed(ttCheckHgts, hgtBufTimeout);
        }
        private String getName(){
            return name;
        }

        @Override
        public int compareTo(HgtBuf o) {
            int res = Long.compare(lastAccess,o.lastAccess);
            if (res == 0){
                res = name.compareTo(o.name);
            }
            return res;
        }
    }



    public void deleteTrack(String name) {
        deleteFile( new File(trackGpxDir, name + ".gpx") );
        deleteFile( new File(trackMetaDir, name + ".meta") );
    }

    @SuppressWarnings("all")
    private void deleteFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    public InputStream openSearchConfigInput(String filename) {
        try {
            File file = new File(searchConfigDir, filename );
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }
    public String[] getSearchConfigNames() {
        return searchConfigDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                if (!s.endsWith(".cfg")) return false;
                return !(new File(file,s).isDirectory());
            }
        });
    }

    public Properties getConfigProperties(String group, String name){
        Properties props = new Properties();
        try {
            File configDir = getConfigDir();
            if (group != null){
                configDir = new File(configDir, group);
                if (!configDir.exists()){
                    Log.w(MGMapApplication.LABEL, NameUtil.context() +" configDir not found: "+configDir.getAbsolutePath());
                }
            }
            File configFile = new File(configDir, name );
            if (configFile.exists()){
                props.load( new FileInputStream(configFile) );
            } else {
                Log.w(MGMapApplication.LABEL, NameUtil.context() +" configFile not found: "+configFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return props;

    }

}