package mg.mgmap.activity.mgmap.features.tilestore;

import mg.mgmap.generic.util.BgJob;

public abstract class MGTileStoreDropJob extends BgJob {

    TileStoreLoader tileStoreLoader;
    int tileXMin;
    int tileXMax;
    int tileYMin;
    int tileYMax;
    byte zoomLevel;

    public MGTileStoreDropJob(TileStoreLoader tileStoreLoader, int tileXMin, int tileXMax, int tileYMin, int tileYMax, byte zoomLevel) {
        this.tileStoreLoader = tileStoreLoader;
        this.tileXMin = tileXMin;
        this.tileXMax = tileXMax;
        this.tileYMin = tileYMin;
        this.tileYMax = tileYMax;
        this.zoomLevel = zoomLevel;
    }


}
