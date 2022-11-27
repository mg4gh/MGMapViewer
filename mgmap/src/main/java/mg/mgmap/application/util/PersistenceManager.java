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
package mg.mgmap.application.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.NameUtil;

/**
 * Covers all handling towards the file system.
 */
public class PersistenceManager {

    private final MGMapApplication application;
    private final Context context;


    private File baseDir;
    private final File appDir;

    private final File trackMetaDir;
    private final File trackGpxDir;
    private final File mapsDir;
    private final File mapsMapsforgeDir;

    private final File themesDir;
    private final File hgtDir;
    private final File logDir;
    private final File configDir;
    private final File searchConfigDir;
    private final File apkDir;

    private final File fRaw;
    private FileOutputStream fosRaw = null;


    public PersistenceManager(MGMapApplication application) {
        this.application = application;
        this.context = application;

        baseDir = context.getExternalFilesDir(null);
        Log.i(MGMapApplication.LABEL, NameUtil.context() + " Default Storage: "+baseDir.getAbsolutePath());

        if (! new File(baseDir, "MGMapViewer").exists()){
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " Default Storage not found - check alternatives");
            for (File f : context.getExternalFilesDirs(null)){
                boolean exists = new File(f, "MGMapViewer").exists();
                Log.i(MGMapApplication.LABEL, NameUtil.context() + "check Storage: "+baseDir.getAbsolutePath()+" ->exists: "+exists);
                if (exists){
                    baseDir = f;
                }
                Log.i(MGMapApplication.LABEL, NameUtil.context() + " f= "+f.getAbsolutePath());
            }
        }

        Log.i(MGMapApplication.LABEL, NameUtil.context() + " Storage: "+baseDir.getAbsolutePath());

        appDir = createIfNotExists(baseDir, "MGMapViewer");
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
        createGraphhopperCfgIfNotExists("Graphhopper.cfg");
        apkDir = createIfNotExists(appDir, "apk");
    }

    public File getBaseDir(){
        return baseDir;
    }
    public File getAppDir(){
        return appDir;
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
            File[] files  = apkDir.listFiles();
            if (files != null){
                for (File file : files){
                    if (!file.delete())
                        Log.w(MGMapApplication.LABEL, NameUtil.context() +"Failed to delete: "+file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            Log.w(MGMapApplication.LABEL, NameUtil.context() + e.getMessage());
        }
    }
    public File getApkFile(){
        String[] apkNames = apkDir.list((dir, name) -> name.endsWith(".apk"));
        if ((apkNames != null) && (apkNames.length > 0)){
            return new File(apkDir, apkNames[0]);
        }
        return null;
    }

    public File createIfNotExists(File parent, String subDir) {
        File f = new File(parent, subDir);
        synchronized (PersistenceManager.class) {
            if (!f.exists()) {
                if (!f.mkdirs())
                    Log.w(MGMapApplication.LABEL, NameUtil.context() +"Failed to create: "+f.getAbsolutePath());
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
            synchronized (PersistenceManager.class) {
                if (!f.exists()) {
                    if (!f.createNewFile())
                        Log.w(MGMapApplication.LABEL, NameUtil.context() +"Failed to create: "+f.getAbsolutePath());
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
            if (!fRaw.delete())
                Log.w(MGMapApplication.LABEL, NameUtil.context() +"Failed to delete: "+fRaw.getAbsolutePath());

        }
    }

    public byte[] getRawData() {
        try {
            if (fRaw.exists()) {
                FileInputStream fis = new FileInputStream(fRaw);
                byte[] b = new byte[fis.available()];
                if (fis.read(b) != b.length)
                    Log.w(MGMapApplication.LABEL, NameUtil.context() +"Failed to read "+b.length+" bytes");

                return b;
            }
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
        return null;
    }

    // extension should include a ".", e.g. ".gpx"
    private File getAbsoluteFile(File baseDir, String filename, String extension){
        return new File(baseDir.getAbsoluteFile()+File.separator+filename+extension);
    }
    private void checkCreatePath(File file){
        File fParent = file.getParentFile();
        assert fParent != null;
        if (!fParent.exists()){
            if (!fParent.mkdirs())
                Log.w(MGMapApplication.LABEL, NameUtil.context() +"Failed to create: "+fParent.getAbsolutePath());
        }
    }

    public boolean existsGpx(String filename) {
        File file = getAbsoluteFile(trackGpxDir, filename, ".gpx");
        return file.exists();
    }
    public PrintWriter openGpxOutput(String filename) {
        try {
            File file = getAbsoluteFile(trackGpxDir, filename, ".gpx");
            checkCreatePath(file);
            return new PrintWriter(file);
        } catch (FileNotFoundException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }
    public InputStream openGpxInput(String filename) {
        try {
            File file = getAbsoluteFile(trackGpxDir, filename, ".gpx");
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }
    public Uri getGpxUri(String filename) {
        try {
            File file = getAbsoluteFile(trackGpxDir, filename, ".gpx");
            return FileProvider.getUriForFile(context, "mg.mgmap.provider", file);
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }

    public FileOutputStream openMetaOutput(String filename) {
        try {
            File file = getAbsoluteFile(trackMetaDir, filename, ".meta");
            checkCreatePath(file);
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }

    public FileInputStream openMetaInput(String filename) {
        try {
            File file = getAbsoluteFile(trackMetaDir, filename, ".meta");
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }

    /** Retruns a list of relative path name, but without extension */
    public ArrayList<String> getNames(File baseDir, File dir, String endsWith, ArrayList<String> matchedList) {
        File[] entries = dir.listFiles();
        if (entries != null){
            for (File entry : entries) {
                if (entry.isDirectory()) {
                    getNames(baseDir, entry, endsWith, matchedList);
                } else {
                    if (entry.getName().endsWith(endsWith)) {
                        String sPath = entry.getAbsolutePath();
                        matchedList.add(sPath.substring((baseDir.getAbsolutePath()+File.pathSeparator ).length(), sPath.length() - endsWith.length()));
                    }
                }
            }
        }
        return matchedList;
    }

    public ArrayList<String> getGpxNames() {
        return getNames(trackGpxDir, trackGpxDir, ".gpx", new ArrayList<>());
    }
    public ArrayList<String> getMetaNames() {
        return getNames(trackMetaDir, trackMetaDir, ".meta", new ArrayList<>());
    }

    public File getThemesDir(){
        return themesDir;
    }
    public String[] getThemeNames() {
        return themesDir.list((file, s) -> {
            if (!s.endsWith(".xml")) return false;
            return !(new File(file,s).isDirectory());
        });
    }


    public File getHgtFile(String hgtName){
        return new File(hgtDir, getHgtFilename(hgtName));
    }

    public void dropHgt(String hgtName){
        deleteFile(getHgtFile(hgtName));
    }


    public String getHgtFilename(String hgtName) {
        return hgtName+".SRTMGL1.hgt.zip";
    }

    public FileOutputStream openHgtOutput(String hgtName){
        try {
            return new FileOutputStream(getHgtFile(hgtName));
        } catch (FileNotFoundException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }

    public boolean hgtIsAvailable(String hgtName){
        return getHgtFile(hgtName).exists();
    }

    public byte[] getHgtBuf(String hgtName){
        byte[] buf = null;
        try {
            File hgtFile = getHgtFile(hgtName);
            if (hgtFile.exists()){
                if (hgtFile.length()>0){
                    ZipFile zipFile = new ZipFile(hgtFile);
                    ZipEntry zipEntry = zipFile.getEntry(hgtName+".hgt");
                    InputStream zis = zipFile.getInputStream(zipEntry);
                    int todo = zis.available();
                    buf = new byte[todo];
                    int done = 0;
                    while (todo > 0) {
                        int step = zis.read(buf, done, todo);
                        todo -= step;
                        done += step;
                    }
                    zipFile.close();
                } else { // is dummy hgt file
                    buf = new byte[0];
                }
            }
        } catch (IOException e) { // should not happen
            e.printStackTrace();
            buf = null; // but if so, prevent accessing inconsistent data
        }
        return buf;
    }

    public void createTrackPath(String trackPath){
        createIfNotExists(trackGpxDir, trackPath);
        createIfNotExists(trackMetaDir, trackPath);
    }

    public boolean existsTrack(String filename){
        return ( getAbsoluteFile(trackGpxDir, filename, ".gpx").exists() );
    }

    public void renameTrack(String oldFilename, String newFilename) {
        deleteTrack(newFilename); // should never be the case - but just to be sure
        if (!getAbsoluteFile(trackGpxDir, oldFilename, ".gpx").renameTo( getAbsoluteFile(trackGpxDir, newFilename, ".gpx") ))
            Log.w(MGMapApplication.LABEL, NameUtil.context() +"Failed to rename gpx: "+oldFilename+" to "+newFilename);
        if (!getAbsoluteFile(trackMetaDir, oldFilename, ".meta").renameTo( getAbsoluteFile(trackMetaDir, newFilename, ".meta") ))
            Log.w(MGMapApplication.LABEL, NameUtil.context() +"Failed to rename meta: "+oldFilename+" to "+newFilename);
    }


    public void deleteTrack(String filename) {
        deleteFile( getAbsoluteFile(trackGpxDir, filename, ".gpx") );
        deleteFile( getAbsoluteFile(trackMetaDir, filename, ".meta") );
    }

    @SuppressWarnings("all")
    public void deleteFile(File file) {
        if (file.exists()) {
            file.delete();
        }
    }

    public String[] getSearchConfigNames() {
        return searchConfigDir.list((file, s) -> {
            if (!s.endsWith(".cfg")) return false;
            return !(new File(file,s).isDirectory());
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
                FileInputStream fis = new FileInputStream(configFile);
                props.load( fis );
                fis.close();
            } else {
                Log.w(MGMapApplication.LABEL, NameUtil.context() +" configFile not found: "+configFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return props;

    }

    @SuppressWarnings("SameParameterValue")
    void createGraphhopperCfgIfNotExists(String ghCfg){
        try {
            IOUtil.copyStreams( application.getAssets().open("graphhopper.log"), new FileOutputStream(new File(searchConfigDir, ghCfg)) );
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
    }
}