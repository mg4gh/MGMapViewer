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
package mg.mgmap.features.gdrive;

import android.util.Log;

import com.google.api.services.drive.Drive;

import java.io.File;

import mg.mgmap.MGMapApplication;
import mg.mgmap.util.BgJob;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.Zipper;

public class UploadJob extends BgJob {

    private Drive dservice; // drive service
    private Zipper zip; // zip file creator to use
    private String idMgmFolder; // id of target folder on gdrive
    private File gpxFolder; // local folder for gpx files
    private String name; // filename to upload

    UploadJob(Drive dservice, Zipper zip, String idMgmFolder, File gpxFolder, String name) {
        this.dservice = dservice;
        this.zip = zip;
        this.idMgmFolder = idMgmFolder;
        this.gpxFolder = gpxFolder;
        this.name = name;
    }

    @Override
    protected void doJob() throws Exception {
        File gpxFile = new File(gpxFolder, name);
        File zipFile = zip.pack(gpxFile.getAbsolutePath());
        if (zipFile.exists()){
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" Upload: "+zipFile.getAbsolutePath());
            GDriveUtil.createFile(dservice,idMgmFolder, zipFile);
            zipFile.delete();
        }
    }
}
