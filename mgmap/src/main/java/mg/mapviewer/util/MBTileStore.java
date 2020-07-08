package mg.mapviewer.util;

import android.database.Cursor;
import android.util.Log;

import org.mapsforge.core.graphics.CorruptedInputStreamException;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.cache.TileStore;
import org.mapsforge.map.layer.queue.Job;
import org.sqlite.database.sqlite.SQLiteDatabase;
import org.sqlite.database.sqlite.SQLiteException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.BBox;

public class MBTileStore extends TileStore {

    static {
        try {
            System.loadLibrary("sqliteX");
        } catch (Throwable t) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), t);
        }
    }

    public static TileStore getTileStore(File storeDir) throws Exception{
        TileStore tileStore = null;
        String[] files = storeDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mbtiles");
            }
        });
        if (files.length == 1){ // ok, this tile store is based on a .mbtiles file
            tileStore = new MBTileStore(storeDir,files[0], AndroidGraphicFactory.INSTANCE);
        }
        if (tileStore == null){ // didn't get an mbtiles store - take normal TileStore
            tileStore = new TileStore(storeDir, ".png", AndroidGraphicFactory.INSTANCE);
        }
        return tileStore;
    }

    private SQLiteDatabase db;
    GraphicFactory graphicFactory;
    BBox bBox;
    byte maxZoom, minZoom;

    public MBTileStore(File rootDirectory, String mbtiles, GraphicFactory graphicFactory) {
        super(rootDirectory, null, graphicFactory);

        this.graphicFactory = graphicFactory;
        db = SQLiteDatabase.openDatabase(rootDirectory.getAbsolutePath()+File.separator+mbtiles, null, SQLiteDatabase.OPEN_READONLY);
        bBox = getBoundingBox();
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+bBox);
        maxZoom = getMaxZoom();
        minZoom = getMinZoom();
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" maxZoom="+maxZoom+" minZoom="+minZoom);
    }

    @Override
    public synchronized boolean containsKey(Job key) {
        if ((minZoom <= key.tile.zoomLevel) && (key.tile.zoomLevel <= maxZoom)){
            return (bBox.intersects(key.tile.getBoundingBox()));
        }
        return false;
    }

    @Override
    public synchronized TileBitmap get(Job key) {
        Tile tile = key.tile;
        long localTileX = tile.tileX;
        long localTileY = tile.tileY;

        // conversion needed to fit the MbTiles coordinate system
        final int[] tmsTileXY = googleTile2TmsTile(localTileX, localTileY, tile.zoomLevel);
        Log.d(MGMapApplication.LABEL,NameUtil.context()+String.format(" Tile requested %d %d is now %d %d", tile.tileX, tile.tileY, tmsTileXY[0], tmsTileXY[1]));
        byte[] bytes = getTileAsBytes(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]), Byte.toString(tile.zoomLevel));

        InputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(bytes);
            TileBitmap bitmap = this.graphicFactory.createTileBitmap(inputStream, tile.tileSize, key.hasAlpha);
            if (bitmap.getWidth() != tile.tileSize || bitmap.getHeight() != tile.tileSize) {
                bitmap.scaleTo(tile.tileSize, tile.tileSize);
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }

    }

    @Override
    public synchronized void destroy() {
        try {
            db.close();
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
    }

    /**
     * queries the database for the data of an raster image
     *
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param z
     *            the z coordinate
     * @return the data, if available for these coordinates
     */
    public byte[] getTileAsBytes(String x, String y, String z) {
        try {
            final Cursor c = this.db.rawQuery(
                    "select tile_data from tiles where tile_column=? and tile_row=? and zoom_level=?", new String[] {
                            x, y, z });
            if (!c.moveToFirst()) {
                c.close();
                return null;
            }
            byte[] bb = c.getBlob(c.getColumnIndex("tile_data"));
            c.close();
            return bb;
        } catch (NullPointerException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
            return null;
        } catch (SQLiteException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
            return null;
        }
    }


    /**
     * Converts Google tile coordinates to TMS Tile coordinates.
     * <p>
     * Code copied from: http://code.google.com/p/gmap-tile-generator/
     * </p>
     *
     * @param tx
     *            the x tile number.
     * @param ty
     *            the y tile number.
     * @param zoom
     *            the current zoom level.
     * @return the converted values.
     */

    public static int[] googleTile2TmsTile(long tx, long ty, byte zoom) {
        return new int[] { (int) tx, (int) ((Math.pow(2, zoom) - 1) - ty) };
    }



    public BBox getBoundingBox() {

        try {
            final Cursor c = this.db.rawQuery("select value from metadata where name=?", new String[] { "bounds" });
            if (!c.moveToFirst()) {
                c.close();
                return null;
            }
            final String box = c.getString(c.getColumnIndex("value"));

            String[] split = box.split(",");
            if (split.length != 4) {
                return null;
            }
            double minlon = Double.parseDouble(split[0]);
            double minlat = Double.parseDouble(split[1]);
            double maxlon = Double.parseDouble(split[2]);
            double maxlat = Double.parseDouble(split[3]);

            return new BBox().extend(minlat, minlon).extend(maxlat, maxlon);

        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
        return null;
    }

    private byte getMaxZoom(){
        byte res = Byte.MAX_VALUE;
        try {
            final Cursor c = this.db.rawQuery("select max(zoom_level) from tiles;", null);
            if (c.moveToFirst()) {
                res = (byte)(c.getInt(0));
            } else {
                c.close();
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
        return res;
    }
    private byte getMinZoom(){
        byte res = Byte.MIN_VALUE;
        try {
            final Cursor c = this.db.rawQuery("select min(zoom_level) from tiles;", null);
            if (c.moveToFirst()) {
                res = (byte)(c.getInt(0));
            } else {
                c.close();
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
        return res;
    }

}
