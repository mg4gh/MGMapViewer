package mg.mgmap.features.gdrive;

import android.util.Log;

import com.google.api.client.http.FileContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import mg.mgmap.MGMapApplication;
import mg.mgmap.util.NameUtil;


public class GDriveUtil {

    public static String getOrCreateTopFolder (Drive drive, String name){
        String rootId = getRoot(drive);
        if (rootId != null){
            String fId = listFolder(drive,rootId,name);
            if (fId  == null){
                fId = createFolder(drive, rootId, name);
            }
            return fId;
        }
        return null;
    }

    public static String createFolder(Drive driveService, String parentId, String name) {
        try {
            File fileMetadata = new File();
            fileMetadata.setParents(Collections.singletonList(parentId));
            fileMetadata.setName(name);
            fileMetadata.setMimeType("application/vnd.google-apps.folder");

            File file = driveService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            return file.getId();
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }

    public static String getRoot(Drive driveService){
        try {
            return driveService.files().get("root").setFields("id").execute().getId();
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }


    public static String createFile(Drive driveService, String parentId, java.io.File iFile){
        try {

            File fileMetadata = new File();
            fileMetadata.setName(iFile.getName());
            fileMetadata.setParents(Collections.singletonList(parentId));
            Map<String, String> props = new HashMap<>();
            props.put("app","MGMapViewer");
//            props.put("appVersion","0.1");
//            props.put("date","16.06.2020");
            fileMetadata.setProperties(props);
            fileMetadata.setModifiedTime(new DateTime(iFile.lastModified()));
            FileContent content = new FileContent("application/zip", iFile);
            File file = driveService.files().create(fileMetadata, content)
                    .setFields("id, parents")
                    .execute();
            return file.getId();
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }


    public static void downloadFile(Drive driveService, String fileId, java.io.File storeToFile){
        try {
            OutputStream outputStream = new FileOutputStream(storeToFile);
            driveService.files().get(fileId)
                    .executeMediaAndDownloadTo(outputStream);
            outputStream.close();
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }

    }

    public static String listFolder(Drive driveService, String idParent, String name){
        try {
            String pageToken = null;
            do {
                FileList result = driveService.files().list()
                        .setQ("mimeType = 'application/vnd.google-apps.folder' and trashed=false and '"+idParent+"' in parents and name = '"+name+"'")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name, parents)")
                        .setPageToken(pageToken)
                        .execute();
                if (result.getFiles().size() > 1){
                    Log.e(MGMapApplication.LABEL, NameUtil.context() + "Multiple folders found: "+name);
                }
                if (result.getFiles().size() > 0){
                    return result.getFiles().get(0).getId();
                }
                pageToken = result.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return null;
    }



    /** Returns a list of FileNames in the directory with idParent, which match the "nameRegex for the filename. */
    public static Set<String> listFiles(Drive driveService, String idParent, String nameRegex){
        Set<String> nameIdSet = new TreeSet<>();

        try {
            String pageToken = null;
            do {
                FileList result = driveService.files().list()
                        .setQ("mimeType != 'application/vnd.google-apps.folder' and trashed=false and '"+idParent+"' in parents")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name)")
                        .setPageToken(pageToken)
                        .execute();
                for (File file : result.getFiles()) {
                    String idName = String.format("%s:%s", file.getName(),file.getId());
                    if (file.getName().matches(nameRegex)){
                        nameIdSet.add(idName);
                    }
                }
                pageToken = result.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return nameIdSet;
    }

}
