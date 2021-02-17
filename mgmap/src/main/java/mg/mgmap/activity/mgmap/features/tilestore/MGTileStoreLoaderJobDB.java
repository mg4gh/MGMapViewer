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

import android.util.Log;

import org.mapsforge.core.model.Tile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;

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
