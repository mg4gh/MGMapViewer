package mg.mgmap.features.tilestore;

import org.mapsforge.core.model.Tile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;

import mg.mgmap.util.PersistenceManager;

public class MGTileStoreLoaderJobFile extends MGTileStoreLoaderJob{

    public MGTileStoreLoaderJobFile(TileStoreLoader tileStoreLoader, Tile tile){
        super(tileStoreLoader,tile);
    }

    @Override
    protected void doJob() throws Exception {
        PersistenceManager pm = PersistenceManager.getInstance();
        File zoomDir = pm.createIfNotExists(tileStoreLoader.storeDir,Byte.toString(tile.zoomLevel));
        File xDir = pm.createIfNotExists(zoomDir,Integer.toString(tile.tileX));
        File yFile = new File(xDir,Integer.toString(tile.tileY)+".png");

        URLConnection conn = tileStoreLoader.xmlTileSource.getURLConnection(tile.zoomLevel, tile.tileX, tile.tileY);
        InputStream is = conn.getInputStream();
        OutputStream os = new FileOutputStream(yFile);

        byte[] b = new byte[2048];
        int length;

        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }

        is.close();
        os.close();


    }


}
