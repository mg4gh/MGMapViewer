package mg.mgmap.features.tilestore;

import android.util.Log;

import org.mapsforge.core.model.Tile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import mg.mgmap.MGMapApplication;
import mg.mgmap.util.NameUtil;

public class MGTileStoreLoaderJobDB extends MGTileStoreLoaderJob{

    public MGTileStoreLoaderJobDB(TileStoreLoader tileStoreLoader, Tile tile){
        super(tileStoreLoader,tile);
    }

    @Override
    protected void doJobNow() throws Exception {
        MGTileStoreDB mgTileStoreDB = (MGTileStoreDB)tileStoreLoader.mgTileStore;

        conn = tileStoreLoader.xmlTileSource.getURLConnection(tile.zoomLevel, tile.tileX, tile.tileY);
        debug = conn.getURL() + " "+conn.getRequestProperties();

        InputStream is = null;
        try {
            is = conn.getInputStream();
        } catch (IOException e) {
            if (conn instanceof HttpURLConnection) {
                HttpURLConnection httpURLConnection = (HttpURLConnection) conn;
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+tileStoreLoader.successCounter+"/"+tileStoreLoader.errorCounter+"/"+tileStoreLoader.jobCounter+" "
                        +httpURLConnection.getResponseCode()+" "+httpURLConnection.getResponseMessage()+" "+conn.getURL());

                if (httpURLConnection.getResponseCode() == 404){
                    is = tileStoreLoader.application.getAssets().open("empty.png");
                }
            }
            if (is == null){
                throw e;
            }
        }
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
