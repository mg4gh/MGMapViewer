package mg.mgmap.features.tilestore;

import org.mapsforge.core.model.Tile;

import mg.mgmap.util.BgJob;

public abstract class MGTileStoreLoaderJob extends BgJob {

    TileStoreLoader tileStoreLoader;
    Tile tile;

    public MGTileStoreLoaderJob(TileStoreLoader tileStoreLoader, Tile tile){
        this.tileStoreLoader = tileStoreLoader;
        this.tile = tile;
    }

 }
