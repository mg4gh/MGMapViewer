package mg.mgmap.features.gdrive;

import android.util.Log;

import com.google.api.services.drive.Drive;

import java.io.File;

import mg.mgmap.MGMapApplication;
import mg.mgmap.util.BgJob;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.Zipper;

public class DownloadJob extends BgJob {

    private Drive dservice; // drive service
    private Zipper zip; // zip file creator to use
    private String idMgmFile; // id of file to download on gdrive
    private File gpxFolder; // local folder for gpx files
    private String name; // filename to download (without ".zip" extension)

    DownloadJob(Drive dservice, Zipper zip, String idMgmFile, File gpxFolder, String name) {
        this.dservice = dservice;
        this.zip = zip;
        this.idMgmFile = idMgmFile;
        this.gpxFolder = gpxFolder;
        this.name = name;
    }

    @Override
    protected void doJob() throws Exception {
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" Download: "+name);

        File zipFile = new File(gpxFolder, name+".zip");
        GDriveUtil.downloadFile(dservice, idMgmFile, zipFile);
        zip.unpack(zipFile.getAbsolutePath(), gpxFolder.getAbsolutePath());
        zipFile.delete();
    }
}
