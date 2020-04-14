package mg.mapviewer.util;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.util.Log;

import org.mapsforge.core.util.MercatorProjection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Locale;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.BBox;

public class TileStoreLoader {





    public File storeDir;
    MGMapApplication application;
    MGMapActivity activity;

    public XmlTileSource xmlTileSource;

    public TileStoreLoader(MGMapActivity activity, MGMapApplication application, File storeDir) throws Exception {
        this.activity = activity;
        this.application = application;
        this.storeDir = storeDir;

        XmlTileSourceConfig config = new XmlTileSourceConfigReader().parseXmlTileSourceConfig(storeDir.getName(), new FileInputStream(new File(storeDir, "config.xml")));
        xmlTileSource = new XmlTileSource(config);
        File sample = new File(storeDir, "sample.curl");
        if (sample.exists()){
            BufferedReader in = new BufferedReader(new FileReader(sample));
            String line = in.readLine();
            in.close();


            String[] parts = line.split(" -H ");
            for (int i=1; i<parts.length;i++){
                String[] subparts = parts[i].replaceAll("'$","").replaceAll("^'","").split(": ");
                if (subparts.length == 2){
                    config.setConnRequestProperty(subparts[0], subparts[1]);
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" \""+subparts[0]+"\"=\""+subparts[1]+"\"");
                }
            }
        }
    }


    ArrayList<TileStoreLoadJob> jobs = new ArrayList<>();

    public void loadFromBB(BBox bBox){
        long now = System.currentTimeMillis();
        int tileSize = 512;
        for (byte zoomLevel=xmlTileSource.getZoomLevelMin(); zoomLevel<= xmlTileSource.getZoomLevelMax(); zoomLevel++) {
            long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
            int tileXMin = MercatorProjection.pixelXToTileX(MercatorProjection.longitudeToPixelX(bBox.minLongitude, mapSize), zoomLevel, tileSize);
            int tileXMax = MercatorProjection.pixelXToTileX(MercatorProjection.longitudeToPixelX(bBox.maxLongitude, mapSize), zoomLevel, tileSize);
            int tileYMin = MercatorProjection.pixelYToTileY(MercatorProjection.latitudeToPixelY(bBox.maxLatitude, mapSize), zoomLevel, tileSize); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY(MercatorProjection.latitudeToPixelY(bBox.minLatitude, mapSize), zoomLevel, tileSize);
            Log.i(MGMapApplication.LABEL, NameUtil.context() + " " + String.format(Locale.ENGLISH, "dls %d %d %d %d %d", zoomLevel, tileXMin, tileXMax, tileYMin, tileYMax));

            File zoomDir = new File(storeDir, Byte.toString(zoomLevel));
            for (int tileX = tileXMin; tileX<= tileXMax; tileX++){
                File xDir = new File(zoomDir, Integer.toString(tileX));
                for (int tileY = tileYMin; tileY<= tileYMax; tileY++) {
                    File yFile = new File(xDir, Integer.toString(tileY)+".png");
                    if (yFile.exists() && (now - yFile.lastModified() < 3 * 30 * 24 * 60 * 60 * 1000L)) {
                        continue;
                    }
                    jobs.add( new TileStoreLoadJob(this, zoomLevel, tileX, tileY));
                }
            }
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        builder.setTitle("Load Tiles for \""+storeDir.getName()+"\"");
        builder.setMessage("Load "+jobs.size()+" tiles?");

        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                // Do nothing, but close the dialog
                dialog.dismiss();
                Log.i(MGMapApplication.LABEL, NameUtil.context() + " do it." );
                application.addBgJobs(jobs);
            }
        });

        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {

                // Do nothing
                dialog.dismiss();
                Log.i(MGMapApplication.LABEL, NameUtil.context() + " don't do it." );
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }
}
