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
package mg.mapviewer.features.gdrive;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PersistenceManager;
import mg.mapviewer.util.Zipper;

public class MSGDrive extends MGMicroService {


    public MSGDrive(MGMapActivity mmActivity) {
        super(mmActivity);
    }

    @Override
    protected void start() {

    }



    @Override
    @SuppressWarnings("EmptyCatchBlock")
    protected void stop() {
    }

    @Override
    protected void onUpdate(Observable o, Object arg) {

    }

    @Override
    protected void doRefresh() {
    }




    /** Directory to store authorization tokens for this application. */
    private static final String DATA_STORE_SUB_DIR = "tokens";
    /** Directory to store authorization tokens for this application. */
    private static final String APP_NAME = "MGMapViewer";
    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;
    /**
     * <p>If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList( DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    public void trySynchronisation() {
        new Thread(){
            @Override
            public void run() {
                trySynchronisationAsync();
            }
        }.start();
    }

    public void trySynchronisationAsync(){

        try {
            HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(new File(PersistenceManager.getInstance().getConfigDir(),DATA_STORE_SUB_DIR));

            Credential credential = authorize();

            Drive dservice = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(APP_NAME)
                    .build();

            //TODO: make Top Folder configurable to separate test and real live data
            String idMgmFolder = GDriveUtil.getOrCreateTopFolder(dservice, APP_NAME);
            File gpxFolder = PersistenceManager.getInstance().getTrackGpxDir();

            //TODO: make zip password configurable
            Zipper zip = new Zipper("geheimXgeheim");

            Set<String> localSet = new TreeSet<String>();
            for (String filename : gpxFolder.list()){
                if (filename.endsWith(".gpx")){
                    localSet.add(filename);
                }
            }

            TreeMap<String, String> remoteMap = new TreeMap();
            Set<String> rSet = GDriveUtil.listFiles(dservice, idMgmFolder, ".*\\.gpx\\.zip");
            for (String entry : rSet){
                String[] parts = entry.split(".zip:");
                if (parts.length == 2){
                    remoteMap.put(parts[0], parts[1]);
                }
            }
            Set<String> commonSet = new TreeSet<>(localSet);
            commonSet.retainAll(remoteMap.keySet());

            localSet.removeAll(commonSet);

            Log.i(MGMapApplication.LABEL, NameUtil.context()+" rSet: "+rSet);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" commonSet: "+commonSet);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" localSet: "+localSet);

            for (String name : localSet){
                File gpxFile = new File(gpxFolder, name);
                File zipFile = zip.pack(gpxFile.getAbsolutePath());
                if (zipFile.exists()){
                    System.out.println("Upload: "+zipFile.getAbsolutePath());
                    GDriveUtil.createFile(dservice,idMgmFolder, zipFile);
                    zipFile.delete();
                }
            }

            for (String name : remoteMap.keySet()){
                if (!commonSet.contains(name)){
                    System.out.println("Download: "+name);
                    File zipFile = new File(gpxFolder, name+".zip");
                    GDriveUtil.downloadFile(dservice, remoteMap.get(name), zipFile);
                    zip.unpack(zipFile.getAbsolutePath(), gpxFolder.getAbsolutePath());
                    zipFile.delete();
                }
            }

        } catch (Throwable t) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), t);
            return;
        }


    }

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    public Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = MSGDrive.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();

        AuthorizationCodeInstalledApp.Browser browser = new AuthorizationCodeInstalledApp.Browser() {
            @Override
            public void browse(String url) throws IOException {
                try {
                    Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse( url ));
                    getActivity().startActivity(myIntent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        AuthorizationCodeInstalledApp aa =    new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver(), browser);

        Credential credential = aa.authorize("user");
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" got credentials.");
        return credential;
    }



}
