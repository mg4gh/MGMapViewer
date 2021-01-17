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
package mg.mgmap.features.gdrive;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import mg.mgmap.MGMapApplication;
import mg.mgmap.util.BgJob;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.PersistenceManager;
import mg.mgmap.util.Zipper;

public class FGDrive {

    /** Directory to store authorization tokens for this application. */
    private static final String GDRIVE_CONFIG = "gdrive.cfg";
    private static final String GDRIVE_CONFIG_TOP_DIR_KEY = "GDRIVE_TOP_DIR";
    private static final String GDRIVE_CONFIG_ZIP_PW_KEY = "ZIP_PW";
    /** Directory to store authorization tokens for this application. */
    private static final String DATA_STORE_SUB_DIR = "tokens";
    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;
    /**
     * <p>If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Collections.singletonList(DriveScopes.DRIVE);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";


    Activity activity;
    MGMapApplication application;
    boolean abortSync = false;

    public FGDrive(Activity activity) {
        this.activity = activity;
        application = (MGMapApplication)activity.getApplication();
    }

    public void trySynchronisation() {
        abortSync = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle("GDrive synchronisation service");
        builder.setMessage("Trying to identify necessary upload and download jobs.");

        builder.setNegativeButton("Abort", (dialog, which) -> {
            abortSync = true;
            dialog.dismiss();
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " don't do it - abort." );
        });
        AlertDialog alert = builder.create();
        alert.show();

        new Thread(){
            @Override
            public void run() {
                trySynchronisationAsync(alert);
            }
        }.start();
    }

    private void trySynchronisationAsync(AlertDialog alert){

        try {
            Properties props = PersistenceManager.getInstance().getConfigProperties(null,GDRIVE_CONFIG);

            HTTP_TRANSPORT = new com.google.api.client.http.javanet.NetHttpTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(new File(PersistenceManager.getInstance().getConfigDir(),DATA_STORE_SUB_DIR));

            Credential credential = authorize();

            Drive dservice = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                    .setApplicationName(MGMapApplication.LABEL)
                    .build();

            String idMgmFolder = GDriveUtil.getOrCreateTopFolder(dservice, props.getProperty(GDRIVE_CONFIG_TOP_DIR_KEY,MGMapApplication.LABEL) );
            File gpxFolder = PersistenceManager.getInstance().getTrackGpxDir();

            Zipper zip = new Zipper(props.getProperty(GDRIVE_CONFIG_ZIP_PW_KEY,"geheimXgeheim!"));

            Set<String> localSet = new TreeSet<>();
            //noinspection ConstantConditions
            for (String filename : gpxFolder.list()){
                if (filename.endsWith(".gpx")){
                    localSet.add(filename);
                }
            }

            TreeMap<String, String> remoteMap = new TreeMap<>();
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

            ArrayList<BgJob> jobs = new ArrayList<>();

            for (String name : localSet){
                jobs.add( new UploadJob(dservice,zip,idMgmFolder,gpxFolder,name) );
            }
            int numUploadJobs = jobs.size();

            for (String name : remoteMap.keySet()){
                if (!commonSet.contains(name)){
                    jobs.add( new DownloadJob(dservice,zip,remoteMap.get(name),gpxFolder,name) );
                }
            }
            int numDownloadJobs = jobs.size()-numUploadJobs;
            String message = "GDrive Sync Overview: \ntracks in sync: "+commonSet.size()+" \ntracks to upload: "+numUploadJobs+" \ntracks to download: "+numDownloadJobs;
            Log.i(MGMapApplication.LABEL, NameUtil.context()+message);
            // ok, now do the real work!!!
            activity.runOnUiThread(() -> {
                alert.hide();
                if (abortSync) return;

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);

                builder.setTitle("GDrive synchronisation service");
                builder.setMessage(message);


                builder.setPositiveButton("Sync now", (dialog, which) -> {
                    dialog.dismiss();
                    Log.i(MGMapApplication.LABEL, NameUtil.context() + " do it." );
                    application.addBgJobs( jobs );
                });

                builder.setNegativeButton("Abort", (dialog, which) -> {
                    // Do nothing
                    dialog.dismiss();
                    Log.i(MGMapApplication.LABEL, NameUtil.context() + " don't do it." );
                });
                AlertDialog alert2 = builder.create();
                alert2.show();
                alert2.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(numDownloadJobs+numUploadJobs>0);
            });

        } catch (Throwable t) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), t);
        }
    }


    /**
     * Creates an authorized Credential object.
     */
    private Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = FGDrive.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
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
        AuthorizationCodeInstalledApp.Browser browser = url -> {
            try {
                Intent myIntent = new Intent(Intent.ACTION_VIEW, Uri.parse( url ));
                activity.startActivity(myIntent);
            } catch (Exception e) {
                Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
            }
        };
        AuthorizationCodeInstalledApp aa =    new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver(), browser);
        Credential credential = aa.authorize("user");
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" got credentials.");
        return credential;
    }

}
