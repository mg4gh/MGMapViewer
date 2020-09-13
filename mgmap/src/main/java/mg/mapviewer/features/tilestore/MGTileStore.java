package mg.mapviewer.features.tilestore;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.cache.TileStore;
import org.mapsforge.map.layer.queue.Job;

import java.io.File;
import java.io.FilenameFilter;

import mg.mapviewer.util.BgJob;

public abstract class MGTileStore extends TileStore {

    protected int tileSize = 256;
    File storeDir;

    public MGTileStore(File storeDir, String suffix, GraphicFactory graphicFactory){
        super(storeDir, suffix, graphicFactory);
        this.storeDir = storeDir;
    }

    public static MGTileStore createTileStore(File storeDir){
        String[] files = storeDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mbtiles");
            }
        });
        if (files.length == 1){ // ok, this tile store is based on a .mbtiles file
            return new MGTileStoreDB(storeDir, files[0], AndroidGraphicFactory.INSTANCE);
        } else {
            return new MGTileStoreFiles(storeDir, AndroidGraphicFactory.INSTANCE);
        }
    }

    @Override
    public synchronized boolean containsKey(Job key) {
        return true;
    }

    public abstract float getDefaultAlpha();

    public boolean hasConfig(){
        return ( new File(storeDir,"config.xml").exists() );
    }

    public File getStoreDir() {
        return storeDir;
    }

    public int getTileSize(){
        return tileSize;
    }

    public abstract BgJob getLoaderJob(TileStoreLoader tileStoreLoader, Tile tile);
}
