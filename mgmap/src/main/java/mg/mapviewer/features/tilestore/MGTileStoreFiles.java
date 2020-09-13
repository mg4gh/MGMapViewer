package mg.mapviewer.features.tilestore;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.queue.Job;

import java.io.File;

import mg.mapviewer.util.BgJob;

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
}
