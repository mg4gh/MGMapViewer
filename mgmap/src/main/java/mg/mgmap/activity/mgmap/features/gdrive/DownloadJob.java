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

import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;

import java.io.File;
import java.io.FileInputStream;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.generic.util.Zipper;
import mg.mgmap.generic.util.gpx.GpxImporter;

public class DownloadJob extends BgJob {

    private MGMapApplication application;
    private Drive dservice; // drive service
    private Zipper zip; // zip file creator to use
    private String idMgmFile; // id of file to download on gdrive
    private File gpxFolder; // local folder for gpx files
    private String name; // filename to download (without ".zip" extension)
    private DateTime dateTime; // timestamp of file to download

    DownloadJob(MGMapApplication application, Drive dservice, Zipper zip, String idMgmFile, File gpxFolder, String name, DateTime dateTime) {
        this.application = application;
        this.dservice = dservice;
        this.zip = zip;
        this.idMgmFile = idMgmFile;
        this.gpxFolder = gpxFolder;
        this.name = name;
        this.dateTime = dateTime;
    }

    @Override
    protected void doJob() throws Exception {
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" Download: "+name);

        File zipFile = new File(gpxFolder, name+".zip");
        GDriveUtil.downloadFile(dservice, idMgmFile, zipFile);
        zip.unpack(zipFile.getAbsolutePath(), gpxFolder.getAbsolutePath());
        zipFile.delete();

        name = name.replaceFirst(".gpx$", "");
        TrackLog trackLog = new GpxImporter(application.getAltitudeProvider()).parseTrackLog(name, application.getPersistenceManager().openGpxInput(name));
        trackLog.setModified(false);
        application.metaTrackLogs.put(trackLog.getNameKey(), trackLog);
        application.getMetaDataUtil().createMetaData(trackLog);
        application.getMetaDataUtil().writeMetaData(application.getPersistenceManager().openMetaOutput(name), trackLog);
        if (dateTime != null){
            application.getPersistenceManager().getGpx(name).setLastModified(dateTime.getValue());
        }
    }
}
