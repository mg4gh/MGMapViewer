package mg.mgmap.activity.mgmap.features.tilestore;

public class MGTileStoreDropJobDB extends MGTileStoreDropJob{

    public MGTileStoreDropJobDB(TileStoreLoader tileStoreLoader, int tileXMin, int tileXMax, int tileYMin, int tileYMax, byte zoomLevel) {
        super(tileStoreLoader, tileXMin, tileXMax, tileYMin, tileYMax, zoomLevel);
    }

    @Override
    protected void doJob() throws Exception {
        MGTileStoreDB mgTileStoreDB = (MGTileStoreDB)tileStoreLoader.mgTileStore;
        mgTileStoreDB.dropTiles(tileXMin,tileXMax, tileYMin, tileYMax, zoomLevel);
    }
}
