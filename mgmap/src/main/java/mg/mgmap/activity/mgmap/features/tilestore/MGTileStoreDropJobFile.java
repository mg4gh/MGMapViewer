package mg.mgmap.activity.mgmap.features.tilestore;

import java.io.File;

public class MGTileStoreDropJobFile extends MGTileStoreDropJob{

    public MGTileStoreDropJobFile(TileStoreLoader tileStoreLoader, int tileXMin, int tileXMax, int tileYMin, int tileYMax, byte zoomLevel) {
        super(tileStoreLoader, tileXMin, tileXMax, tileYMin, tileYMax, zoomLevel);
    }

    @Override
    protected void doJob() throws Exception {
        File zoomDir = new File(tileStoreLoader.storeDir,Byte.toString(zoomLevel));
        for (int tileX = tileXMin+1; tileX< tileXMax; tileX++){
            File xDir = new File(zoomDir,Integer.toString(tileX));
            for (int tileY = tileYMin+1; tileY< tileYMax; tileY++) {
                File yFile = new File(xDir, tileY +".png");
                if (yFile.exists()){
                    if (!yFile.delete()){
                        throw new Exception("failed to delete File "+yFile.getAbsolutePath());
                    }
                }
            }
        }
    }
}
