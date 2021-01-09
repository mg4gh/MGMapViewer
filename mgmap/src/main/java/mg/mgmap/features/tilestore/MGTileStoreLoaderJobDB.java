package mg.mgmap.features.tilestore;

import org.mapsforge.core.model.Tile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URLConnection;

public class MGTileStoreLoaderJobDB extends MGTileStoreLoaderJob{

    public MGTileStoreLoaderJobDB(TileStoreLoader tileStoreLoader, Tile tile){
        super(tileStoreLoader,tile);
    }

    @Override
    protected void doJob() throws Exception {
        MGTileStoreDB mgTileStoreDB = (MGTileStoreDB)tileStoreLoader.mgTileStore;



        URLConnection conn = tileStoreLoader.xmlTileSource.getURLConnection(tile.zoomLevel, tile.tileX, tile.tileY);
        InputStream is = conn.getInputStream();
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        byte[] b = new byte[2048];
        int length;

        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }
        byte[] tileData = os.toByteArray();
        is.close();
        os.close();

        mgTileStoreDB.saveTileBytes(tile, tileData);
    }
}
