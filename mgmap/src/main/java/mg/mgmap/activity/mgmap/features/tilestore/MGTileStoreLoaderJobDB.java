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

import org.mapsforge.core.model.Tile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;

import mg.mgmap.generic.util.basic.IOUtil;
import mg.mgmap.generic.util.basic.MGLog;

public class MGTileStoreLoaderJobDB extends MGTileStoreLoaderJob{

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final boolean bOld;

    public MGTileStoreLoaderJobDB(TileStoreLoader tileStoreLoader, Tile tile, boolean bOld){
        super(tileStoreLoader,tile);
        this.bOld = bOld;
    }

    @Override
    protected void doJob() throws Exception {
        MGTileStoreDB mgTileStoreDB = (MGTileStoreDB)tileStoreLoader.mgTileStore;

        super.doJob();

        InputStream is = null;
        try {
            is = conn.getInputStream();
        } catch (IOException e) {
            if (conn instanceof HttpURLConnection httpURLConnection) {
                mgLog.d(httpURLConnection.getResponseCode()+" "+httpURLConnection.getResponseMessage()+" "+conn.getURL());

                if (httpURLConnection.getResponseCode() == 404){
                    is = tileStoreLoader.application.getAssets().open("empty.png");
                }
            }
            if (is == null){
                throw e;
            }
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        IOUtil.copyStreams(is , os);
        byte[] tileData = os.toByteArray();

        mgTileStoreDB.saveTileBytes(tile, tileData, bOld);
    }
}
