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

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.MGLog;

/**
 * Covers all handling towards the file system.
 */
public class PersistenceManager {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static final String SUFFIX_GPX = ".gpx";
    public static final String SUFFIX_META = ".meta";


    public static ArrayList<File> getFilesRecursive(File dir, String nameEndsWith, ArrayList<File> matchedList){
        File[] entries = dir.listFiles();
        if (entries != null){
            for (File entry : entries) {
                if (entry.isDirectory()) {
                    getFilesRecursive(entry, nameEndsWith, matchedList);
                } else {
                    if (entry.getName().endsWith(nameEndsWith)) {
                        matchedList.add(entry);
                    }
                }
            }
        }
        return matchedList;
    }

    public static ArrayList<File> getFilesRecursive(File dir, String suffix){
        return getFilesRecursive(dir, suffix, new ArrayList<>());
    }

    public static  List<String> getNamesRecursive(File dir, String suffix){
        return getFilesRecursive(dir, suffix, new ArrayList<>())
                .stream().map(f->f.getAbsolutePath()
                        .substring(dir.getAbsolutePath().length() + File.separator.length(), f.getAbsolutePath().length()-suffix.length()) )
                .collect(Collectors.toList());
    }

    public static boolean forceDelete(File file){
        forceDeleteContent(file);
        return file.delete();
    }
    public static void forceDeleteContent(File file){
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    forceDelete(f);
                }
            }
        }
    }

    public static void forceDeleteEmptyDirs(File file){
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (f.isDirectory() && !Files.isSymbolicLink(f.toPath())) {
                    forceDeleteEmptyDirs(f);
                }
            }
            contents = file.listFiles(); // check contents after recursive walk through
            assert (contents != null);
            if (contents.length == 0){                                                          // Is it now empty?
                if (!file.delete()) mgLog.e("failed to delete "+file.getAbsolutePath());   // then delete it
            }
        }
    }



    public static void mergeDir(File mergeFrom, File mergeTo){ // recursive merge dir
        assert mergeFrom.exists();
        assert mergeTo.exists();
        File[] fromArray = mergeFrom.listFiles();
        if (fromArray != null){
            for (File file : fromArray){
                if (Files.isSymbolicLink(file.toPath())) continue;
                File toFile = new File(mergeTo, file.getName());

                if (toFile.exists() && (file.isFile() != toFile.isFile())){
                    mgLog.e("cannot merge "+file.getAbsolutePath()+" with type "+(file.isFile()?"file":"directory")+" to "+toFile.getAbsolutePath()+" with type "+(toFile.isFile()?"file":"directory"));
                    continue;
                }
                if (file.isFile()){
                    if (toFile.exists()){
                        if ( file.lastModified() > toFile.lastModified() ){
                            if (!toFile.delete()) mgLog.e("failed to delete "+toFile.getAbsolutePath());
                            if (!file.renameTo(toFile)) mgLog.e("failed to rename from "+file.getAbsolutePath()+" to "+toFile.getAbsolutePath());
                        }
                    } else {
                        if (!file.renameTo(toFile)) mgLog.e("failed to rename from "+file.getAbsolutePath()+" to "+toFile.getAbsolutePath());
                    }
                } else { // file is directory
                    if (!toFile.exists()){
                        if (!toFile.mkdir()) mgLog.e("failed to create "+toFile.getAbsolutePath());
                    }
                    mergeDir(file, toFile);
                }
            } // for (File file : fromArray){
        } // if (fromArray != null){
    }

    public static boolean checkFilesOlderThan(File dir, long timestamp){
        File[] entries = dir.listFiles();
        if (entries != null){
            for (File entry : entries) {
                if (entry.isDirectory()) {
                    checkFilesOlderThan(entry, timestamp);
                } else {
                    if (entry.lastModified() >= timestamp) {
                        return false;
                    }
                }
            }
        }
        return true;
    }





    private final MGMapApplication application;
    private final Context context;

    private File baseDir;
    private File baseDirExt = null;
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
    private final File backupDir;
    private final File restoreDir;
    private final File tempZipDir;

    private final File fRaw;
    private FileOutputStream fosRaw = null;


    public PersistenceManager(MGMapApplication application, String sAppDir) {
        this.application = application;
        this.context = application;

        baseDir = context.getExternalFilesDir(null);
        mgLog.i("Default Storage: "+getBaseDir().getAbsolutePath());

        File[] externalFilesDirs = context.getExternalFilesDirs(null);
        for (File f : externalFilesDirs) {
            mgLog.i("Storage check: "+f.getAbsolutePath()+" exists="+f.exists());
            if ((externalFilesDirs.length == 2) && (f != baseDir)){
                baseDirExt = f;
                mgLog.i("SDCard baseDir: "+f.getAbsolutePath()+" exists="+f.exists());
            }
        }

        File appDir2Check = new File(baseDir, sAppDir);
        if (! appDir2Check.exists()){
            mgLog.i("Default App Storage ("+appDir2Check.getAbsolutePath()+") not found - check alternatives");
            for (File f : context.getExternalFilesDirs(null)){
                appDir2Check = new File(f, sAppDir);
                boolean exists = appDir2Check.exists();
                mgLog.i("check App Storage: "+appDir2Check.getAbsolutePath()+" ->exists: "+exists);
                if (exists){
                    baseDir = f;
                    break;
                }
            }
        }

        mgLog.i("Storage baseDir="+baseDir.getAbsolutePath());

        appDir = createIfNotExists(baseDir, sAppDir);
        configDir = createIfNotExists(appDir, "config");
        // load extra configured properties before first usage of preferences
        MGMapApplication.loadPropertiesToPreferences(application.getSharedPreferences(), getConfigProperties("load", ".*.properties"));

        File trackDir = createIfNotExists(getAppDir(), "track");
        trackMetaDir = createIfNotExists(trackDir, "meta");
        trackGpxDir = createIfNotExists(trackDir, "gpx");
        File trackRecDir = createIfNotExists(trackDir, "recording");
        fRaw = new File(trackRecDir, "raw.dat");

        mapsDir = createIfNotExists(appDir, "maps");
        mapsMapsforgeDir = createIfNotExists(mapsDir, "mapsforge");
        createIfNotExists(mapsMapsforgeDir, "all");
        createIfNotExists(mapsDir, "mapstores");
        createIfNotExists(mapsDir, "maponline");
        createIfNotExists(mapsDir, "mapgrid");
        themesDir = createIfNotExists(appDir, "themes");
        hgtDir = createIfNotExists(appDir, "hgt");
        logDir = createIfNotExists(appDir, "log");
        searchConfigDir = createIfNotExists(configDir, "search");
        createFileIfNotExists(searchConfigDir, "POI.cfg");
        createFileIfNotExists(searchConfigDir, "Nominatim.cfg");
        createGraphhopperCfgIfNotExists("Graphhopper.cfg");
        createFileIfNotExists(searchConfigDir,"GeoLatLong.cfg");
        apkDir = createIfNotExists(appDir, "apk");
        File backupBaseDir = createIfNotExists(appDir, "backup");
        backupDir = createIfNotExists(backupBaseDir, "backup");
        restoreDir = createIfNotExists(backupBaseDir, "restore");
        tempZipDir = createIfNotExists(context.getFilesDir(), "tempZip");
        forceDeleteContent(tempZipDir);
    }

    public File getBaseDir(){
        return baseDir;
    }
    public File getBaseDirExt(){
        return baseDirExt;
    }
    public File getAppDir(){
        return appDir;
    }
    public File getHgtDir(){
        return hgtDir;
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
    public File getTrackMetaDir(){
        return trackMetaDir;
    }
    public File getApkDir(){
        return apkDir;
    }
    public File getBackupDir() {
        return backupDir;
    }
    public File getRestoreDir() {
        return restoreDir;
    }
    public File getTempZipDir() {
        return tempZipDir;
    }

    public void cleanApkDir(){
        try {
            File[] files  = apkDir.listFiles();
            if (files != null){
                for (File file : files){
                    if (!file.delete())
                        mgLog.w("Failed to delete: "+file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            mgLog.w(e.getMessage());
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
                    mgLog.w("Failed to create: "+f.getAbsolutePath());
            }
        }
        if (!f.exists()) {
            mgLog.e("Failed to create directory: " + f.getAbsolutePath());
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
                        mgLog.w("Failed to create: "+f.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            mgLog.e(e);
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
            mgLog.e(e);
        }
    }

    public void clearRaw() {
        try {
            if (fosRaw != null) {
                fosRaw.close();
                fosRaw = null;
            }
        } catch (IOException e) {
            mgLog.e(e);
        }
        if (fRaw.exists()) {
            if (!fRaw.delete())
                mgLog.w("Failed to delete: "+fRaw.getAbsolutePath());

        }
    }

    public byte[] getRawData() {
        try {
            if (fRaw.exists()) {
                try (FileInputStream fis = new FileInputStream(fRaw)){
                    byte[] b = new byte[fis.available()];
                    if (fis.read(b) != b.length)
                        mgLog.w("Failed to read "+b.length+" bytes");
                    return b;
                }
            }
        } catch (IOException e) {
            mgLog.e(e);
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
                mgLog.w("Failed to create: "+fParent.getAbsolutePath());
        }
    }

    public boolean existsGpx(String filename) {
        File file = getAbsoluteFile(trackGpxDir, filename, SUFFIX_GPX);
        return file.exists();
    }
    public boolean isGpxOlderThanMeta(String filename){
        File gpxFile = getAbsoluteFile(trackGpxDir, filename, SUFFIX_GPX);
        File metaFile = getAbsoluteFile(trackMetaDir, filename, SUFFIX_META);
        assert gpxFile.exists();
        if (metaFile.exists()){
            return gpxFile.lastModified() <= metaFile.lastModified();
        } else {
            return false;
        }
    }
    public PrintWriter openGpxOutput(String filename) {
        try {
            File file = getAbsoluteFile(trackGpxDir, filename, SUFFIX_GPX);
            checkCreatePath(file);
            return new PrintWriter(file);
        } catch (FileNotFoundException e) {
            mgLog.e(e);
        }
        return null;
    }
    public InputStream openGpxInput(String filename) {
        try {
            File file = getAbsoluteFile(trackGpxDir, filename, SUFFIX_GPX);
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            mgLog.e(e);
        }
        return null;
    }
    public Uri getGpxUri(String filename) {
        try {
            File file = getAbsoluteFile(trackGpxDir, filename, SUFFIX_GPX);
            return FileProvider.getUriForFile(context, context.getPackageName() + ".provider", file);
        } catch (Exception e) {
            mgLog.e(e);
        }
        return null;
    }

    public FileOutputStream openMetaOutput(String filename) {
        try {
            File file = getAbsoluteFile(trackMetaDir, filename, SUFFIX_META);
            checkCreatePath(file);
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            mgLog.e(e);
        }
        return null;
    }

    public FileInputStream openMetaInput(String filename) {
        try {
            File file = getAbsoluteFile(trackMetaDir, filename, SUFFIX_META);
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            mgLog.e(e);
        }
        return null;
    }


    public List<String> getGpxNames() {
        return  getNamesRecursive(trackGpxDir, SUFFIX_GPX);
    }
    public List<String> getMetaNames() {
        return  getNamesRecursive(trackMetaDir, SUFFIX_META);
    }

    public File getThemesDir(){
        return themesDir;
    }
    public String[] getThemeNames() {
        ArrayList<String> themeNames = new ArrayList<>();
        getThemeNames(themesDir, themeNames);
        return themeNames.toArray(new String[0]);
    }
    @SuppressWarnings("ConstantConditions")
    public void getThemeNames(File dir, ArrayList<String> resList) {
        for (File f : dir.listFiles()){
            if (f.isDirectory()){
                getThemeNames(f, resList);
            } else {
                if (f.getName().endsWith(".xml")){
                    String theme = f.getAbsolutePath().replace(themesDir.getAbsolutePath()+"/","");
                    mgLog.i("found theme "+theme);
                    resList.add(theme);
                }
            }
        }
    }

    public File getHgtFile(String hgtName){
        File hgtFile = new File(hgtDir, hgtName+".hgt"); // prefer unzipped if exists
        if (!hgtFile.exists()){
            hgtFile = new File(hgtDir, hgtName+".SRTMGL1.hgt.zip");
        }
        return hgtFile;
    }

    public ArrayList<String> getExistingHgtNames(){
        ArrayList<String> res = new ArrayList<>();
        String[] hgtFileNames = hgtDir.list();
        if (hgtFileNames != null){
            for (String hgtFileName : hgtFileNames){
                res.add( hgtFileName.replaceFirst( "\\..*",""));
            }
        }
        return res;
    }

    public void dropHgt(String hgtName){
        deleteFile(getHgtFile(hgtName));
    }

    public FileOutputStream openHgtOutput(String hgtName){
        try {
            return new FileOutputStream(getHgtFile(hgtName));
        } catch (FileNotFoundException e) {
            mgLog.e(e);
        }
        return null;
    }

    public byte[] getHgtBuf(String hgtName){
        byte[] buf = new byte[0];
        try {
            File hgtFile = getHgtFile(hgtName);
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
            mgLog.w("Failed to rename gpx: "+oldFilename+" to "+newFilename);
        if (!getAbsoluteFile(trackMetaDir, oldFilename, ".meta").renameTo( getAbsoluteFile(trackMetaDir, newFilename, ".meta") ))
            mgLog.w("Failed to rename meta: "+oldFilename+" to "+newFilename);
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
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteRecursivly(@NonNull File file) {
        if (file.exists()){
            if (file.isDirectory()){
                File[] subFiles = file.listFiles();
                if (subFiles != null){
                    for (File f : subFiles){
                        deleteRecursivly(f);
                    }
                }
            }
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
                    mgLog.i("configDir not found: "+configDir.getAbsolutePath());
                }
            }
            File[] configFiles = configDir.listFiles((dir, filename) -> filename.matches(name));
            if (configFiles != null){
                for (File configFile : configFiles){
                    try (FileInputStream fis = new FileInputStream(configFile)){
                        props.load( fis );
                    }
                }
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
        return props;

    }

    @SuppressWarnings("SameParameterValue")
    void createGraphhopperCfgIfNotExists(String ghCfg){
        try {
            File graphhopperSearchConfig = new File(searchConfigDir, ghCfg);
            if (!graphhopperSearchConfig.exists()){
                IOUtil.copyStreams( application.getAssets().open("graphhopper.log"), Files.newOutputStream(new File(searchConfigDir, ghCfg).toPath()));
            }
        } catch (IOException e) {
            mgLog.e(e);
        }
    }
}