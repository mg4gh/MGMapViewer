package mg.mapviewer.features.tilestore;

import android.util.Log;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.queue.Job;

import java.io.File;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.BBox;
import mg.mapviewer.util.BgJob;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PersistenceManager;

public class MGTileStoreFiles extends MGTileStore {

    private static final String SUFFIX = ".png";
    public MGTileStoreFiles(File storeDir, GraphicFactory graphicFactory){
        super(storeDir, SUFFIX, graphicFactory);
    }

    @Override
    public float getDefaultAlpha() {
        return 0.2f;
    }

    @Override
    public synchronized boolean containsKey(Job key) {
        File zoomDir = new File(storeDir, Byte.toString(key.tile.zoomLevel));
        if (!zoomDir.exists()) return false;

        File xDir = new File(zoomDir, Integer.toString(key.tile.tileX));
        if (!zoomDir.exists()) return false;

        File yFile = new File(xDir, Integer.toString(key.tile.tileY) + SUFFIX);
        return yFile.exists();
    }

    @Override
    public BgJob getLoaderJob(TileStoreLoader tileStoreLoader, Tile tile) {
        return new MGTileStoreLoaderJobFile(tileStoreLoader, tile);
    }

    public BgJob getDropJob(TileStoreLoader tileStoreLoader, int tileXMin, int tileXMax, int tileYMin, int tileYMax, byte zoomLevel){
        return new BgJob(){
            @Override
            protected void doJob() throws Exception {
                PersistenceManager pm = PersistenceManager.getInstance();
                File zoomDir = new File(tileStoreLoader.storeDir,Byte.toString(zoomLevel));
                for (int tileX = tileXMin+1; tileX< tileXMax; tileX++){
                    File xDir = new File(zoomDir,Integer.toString(tileX));
                    for (int tileY = tileYMin+1; tileY< tileYMax; tileY++) {
                        File yFile = new File(xDir,Integer.toString(tileY)+".png");
                        if (yFile.exists()){
                            yFile.delete();
                        }
                    }
                }
            }
        };
    }

}
