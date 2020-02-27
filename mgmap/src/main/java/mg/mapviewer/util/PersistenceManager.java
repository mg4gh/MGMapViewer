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
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

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
import java.util.Locale;
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

    private File themesDir;
    private File hgtDir;
    private File logDir;

    private File fRaw;
    private FileOutputStream fosRaw = null;

    private String hgtName = "";
    private byte[] hgtBuf = null;

    synchronized private static void init(Context context){
        if (baseDir == null){
            boolean bPrefStorage = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getResources().getString(R.string.preferences_storage_key), false);
            baseDir = Environment.getExternalStorageDirectory();
            if (baseDir.canWrite() && bPrefStorage){
            } else {
                baseDir = context.getExternalFilesDir(null);

            }
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " Storage: "+baseDir.getAbsolutePath());
        }
    }

    private PersistenceManager() {
        File appDir = createIfNotExists(baseDir, "MGMapViewer");
        File trackDir = createIfNotExists(appDir, "track");
        trackMetaDir = createIfNotExists(trackDir, "meta");
        trackGpxDir = createIfNotExists(trackDir, "gpx");
        File trackRecDir = createIfNotExists(trackDir, "recording");

        mapsDir = createIfNotExists(appDir, "maps");
        createIfNotExists(mapsDir, "mapsforge");
        createIfNotExists(mapsDir, "mapstores");
        createIfNotExists(mapsDir, "maponline");
        createIfNotExists(mapsDir, "mapgrid");
        themesDir = createIfNotExists(appDir, "themes");
        hgtDir = createIfNotExists(appDir, "hgt");
        logDir = createIfNotExists(appDir, "log");
        fRaw = new File(trackRecDir, "raw.dat");
    }

    public File getLogDir(){
        return logDir;
    }
    public File getMapsDir(){
        return mapsDir;
    }

    private File createIfNotExists(File parent, String subDir) {
        File f = new File(parent, subDir);
        if (!f.exists()) {
            if (!f.mkdir()) {
                Log.e(MGMapApplication.LABEL, NameUtil.context()+" Failed to create directory: " + f.getAbsolutePath());
                throw new RuntimeException("Failed to create directory: " + f.getAbsolutePath());
            }
        }
        return f;
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

    public String getThemesDir(){
        return themesDir.getAbsolutePath();
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

    public byte[] getHgtBuf(int iLat, int iLon){
        try {
            String file = String.format(Locale.GERMANY, "N%02dE%03d",iLat,iLon);
            if (! file.equals(hgtName)) {
                hgtName = file;
                ZipFile zipFile = new ZipFile(new File(hgtDir, file+".SRTMGL1.hgt.zip"));
                ZipEntry zipEntry = zipFile.getEntry(file+".hgt");
                InputStream zis = zipFile.getInputStream(zipEntry);
                int todo = zis.available();
                hgtBuf = new byte[todo];
                int done = 0;
                while (todo > 0) {
                    int step = zis.read(hgtBuf, done, todo);
                    todo -= step;
                    done += step;
                }
                zipFile.close();
            }
        } catch (IOException e){
            // if no file for this region is available, this exception can occur
            hgtBuf = null;
        }
        return hgtBuf;
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

}