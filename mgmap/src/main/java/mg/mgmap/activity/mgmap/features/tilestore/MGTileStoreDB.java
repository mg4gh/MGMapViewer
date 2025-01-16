/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.mgmap.features.tilestore;

import android.content.ContentValues;
import android.content.res.AssetManager;
import android.database.Cursor;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.layer.queue.Job;
import org.sqlite.database.sqlite.SQLiteDatabase;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.Locale;

import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.MGLog;

@SuppressWarnings("IOStreamConstructor")
public class MGTileStoreDB extends MGTileStore {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    static {
        try {
            System.loadLibrary("sqliteX");
        } catch (Throwable t) {
            mgLog.e(t);
        }
    }

    private final SQLiteDatabase db;
    private final GraphicFactory graphicFactory;

    public MGTileStoreDB(File storeDir, AssetManager am, GraphicFactory graphicFactory) throws Exception{
        super(storeDir, null, graphicFactory);
        this.graphicFactory = graphicFactory;

        File store = null;
        File storeRW = null;
        File[] files = storeDir.listFiles();
        if (files != null){
            for (File file : files){
                if (file.getName().endsWith(".mbtiles")){
                    if (Files.isReadable(file.toPath())){
                        store = file;
                        if (Files.isWritable(file.toPath())){
                            storeRW = file;
                            break;
                        }
                    }
                }
            }
        }
        String prefix = "["+storeDir.getName()+"] ";
        if (store == null){
            mgLog.i(prefix+"nothing found - copy empty store form assets.");
            storeRW = new File(storeDir, "store.mbtiles");
            IOUtil.copyStreams(am.open("store.mbtiles"), new FileOutputStream(storeRW));
        } else if (storeRW == null){ // store found, but is not writeable - create a clone that is writeable
            mgLog.i(prefix+"store found ("+store.getName()+"), but is not writeable - create a clone that is writeable");
            storeRW = new File(store.getAbsolutePath().replaceFirst("\\.mbtiles", "_rw.mbtiles"));
            IOUtil.copyFile(store, storeRW);
        } else {
            mgLog.i(prefix+"store found with read/write access found: "+storeRW.getName());
        }
        db = SQLiteDatabase.openDatabase(storeRW.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
    }


    @Override
    public synchronized TileBitmap get(Job key) {
        Tile tile = key.tile;
        byte[] bytes = getTileAsBytes(tile);

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
            mgLog.i("Try to close database.");
            db.close();
        } catch (Exception e) {
            mgLog.e(e);
        }
    }

    @Override
    public synchronized boolean containsKey(Job key) {
        Tile tile = key.tile;
        final int[] tmsTileXY = googleTile2TmsTile(tile.tileX, tile.tileY, tile.zoomLevel);
        return containsKey(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]), Byte.toString(tile.zoomLevel));
    }

    private synchronized boolean containsKey(String x, String y, String z) {
        boolean bRes = false;
        try {
            final Cursor c = this.db.rawQuery(
                    "select tile_row from tiles where tile_column=? and tile_row=? and zoom_level=?", new String[] {
                            x, y, z });
            if (c.moveToFirst()) {
                bRes = true;
            }
            c.close();
        } catch (Exception e) {
            mgLog.e(e);
        }
        return bRes;

    }

    @Override
    public BgJob getLoaderJob(TileStoreLoader tileStoreLoader, Tile tile, boolean bOld) {
        return new MGTileStoreLoaderJobDB(tileStoreLoader, tile, bOld);
    }

    @Override
    public BgJob getDropJob(TileStoreLoader tileStoreLoader, int tileXMin, int tileXMax, int tileYMin, int tileYMax, byte zoomLevel) {
        return new MGTileStoreDropJobDB(tileStoreLoader,tileXMin,tileXMax,tileYMin,tileYMax,zoomLevel);
    }


    byte[] getTileAsBytes(Tile tile){
        final int[] tmsTileXY = googleTile2TmsTile(tile.tileX, tile.tileY, tile.zoomLevel);
        return getTileAsBytes(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]), Byte.toString(tile.zoomLevel));
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
    private synchronized byte[] getTileAsBytes(String x, String y, String z) {
        try {
            final Cursor c = this.db.rawQuery(
                    "select tile_data from tiles where tile_column=? and tile_row=? and zoom_level=?", new String[] {
                            x, y, z });
            if (!c.moveToFirst()) {
                c.close();
                return null;
            }
            int colIdx = c.getColumnIndex("tile_data");
            assert (colIdx >= 0);
            byte[] bb = c.getBlob(colIdx);
            c.close();
            return bb;
        } catch (Exception e) {
            mgLog.e(e);
            return null;
        }
    }

    void saveTileBytes(Tile tile, byte[] tileData, boolean bOld){
        final int[] tmsTileXY = googleTile2TmsTile(tile.tileX, tile.tileY, tile.zoomLevel);
        saveTileBytes(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]), Byte.toString(tile.zoomLevel), tileData ,bOld);
    }


    private synchronized void saveTileBytes(String x, String y, String z, byte[] bb, boolean bOld){
        try {
            if (bOld){
                dropTile(x,y);
            }
            ContentValues cv = new ContentValues();
            cv.put("zoom_level", z);
            cv.put("tile_column", x);
            cv.put("tile_row", y);
            cv.put("tile_data",bb);

            db.insert("tiles", null, cv);

        } catch (Exception e) {
            mgLog.e(e);
        }
    }

    void dropTiles(int tileXMin, int tileXMax, int tileYMin, int tileYMax, byte zoomLevel){
        final int[] tmsTileXYMin = googleTile2TmsTile(tileXMin, tileYMin, zoomLevel);
        final int[] tmsTileXYMax = googleTile2TmsTile(tileXMax, tileYMax, zoomLevel);
        dropTiles(tmsTileXYMin[0],tmsTileXYMax[0],tmsTileXYMax[1],tmsTileXYMin[1]);
    }


    private synchronized void dropTile(String tileX, String tileY){
        try {
            String sql = String.format(Locale.ENGLISH," ((%s == tile_column) AND (%s == tile_row));",tileX,tileY);
            mgLog.i("SQL: "+sql);
            db.delete("tiles", sql, null);
        } catch (Exception e) {
            mgLog.e(e);
        }
    }

    private synchronized void dropTiles(int tileXMin, int tileXMax, int tileYMin, int tileYMax){
        try {
            String sql = String.format(Locale.ENGLISH," ((%d < tile_column) AND (tile_column<%d) AND (%d<tile_row) AND (tile_row<%d));",tileXMin,tileXMax,tileYMin,tileYMax);
            mgLog.i("SQL: "+sql);
            db.delete("tiles", sql, null);
        } catch (Exception e) {
            mgLog.e(e);
        }
    }


    /**
     * Converts Google tile coordinates to TMS Tile coordinates.
     * <p>
     * Code copied from: h t t p://code.google.com/p/gmap-tile-generator/
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

    @Override
    public float getDefaultAlpha() {
        return 1.0f;
    }
}
