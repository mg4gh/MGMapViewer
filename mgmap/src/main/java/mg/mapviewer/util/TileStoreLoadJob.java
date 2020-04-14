package mg.mapviewer.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;

public class TileStoreLoadJob extends BgJob{

    TileStoreLoader tileStoreLoader;
    byte zoom;
    int tileX;
    int tileY;

    public TileStoreLoadJob(TileStoreLoader tileStoreLoader, byte zoom, int tileX, int tileY){
        this.tileStoreLoader = tileStoreLoader;
        this.zoom = zoom;
        this.tileX = tileX;
        this.tileY = tileY;
    }

    @Override
    protected void doJob() throws Exception {
        PersistenceManager pm = PersistenceManager.getInstance();
        File zoomDir = pm.createIfNotExists(tileStoreLoader.storeDir,Byte.toString(zoom));
        File xDir = pm.createIfNotExists(zoomDir,Integer.toString(tileX));
        File yFile = new File(xDir,Integer.toString(tileY)+".png");

        URLConnection conn = tileStoreLoader.xmlTileSource.getURLConnection(zoom, tileX, tileY);
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
