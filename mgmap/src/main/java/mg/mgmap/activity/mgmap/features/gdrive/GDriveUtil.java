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
package mg.mgmap.activity.mgmap.features.gdrive;

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
import java.util.TreeMap;
import java.util.TreeSet;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;


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


    public static String createFile(Drive driveService, String parentId, java.io.File iFile, String id){
        try {

            if (id != null){ // first delete old file
                driveService.files().delete(id).execute();
            }
            File fileMetadata = new File();
            fileMetadata.setName(iFile.getName());
            fileMetadata.setParents(Collections.singletonList(parentId));
            Map<String, String> props = new HashMap<>();
            props.put("app","MGMapViewer");
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
                        .setFields("nextPageToken, files(id, name, size, modifiedTime)")
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

    /** Returns a map FileNames->File in the directory with idParent, which match the nameRegex for the filename. */
    public static TreeMap<String, File> getFiles(Drive driveService, String idParent, String nameRegex, String namePart2skip){
        TreeMap<String, File> treeMap = new TreeMap<>();

        try {
            String pageToken = null;
            do {
                FileList result = driveService.files().list()
                        .setQ("mimeType != 'application/vnd.google-apps.folder' and trashed=false and '"+idParent+"' in parents")
                        .setSpaces("drive")
                        .setFields("nextPageToken, files(id, name, size, modifiedTime)")
                        .setPageToken(pageToken)
                        .execute();
                for (File file : result.getFiles()) {
                    if (file.getName().matches(nameRegex)){
                        treeMap.put(file.getName().replaceFirst(namePart2skip, ""),file);
                    }
                }
                pageToken = result.getNextPageToken();
            } while (pageToken != null);
        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        return treeMap;
    }

}
