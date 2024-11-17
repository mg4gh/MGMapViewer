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

import java.net.URLConnection;

import mg.mgmap.generic.util.BgJob;

public abstract class MGTileStoreLoaderJob extends BgJob {

    final TileStoreLoader tileStoreLoader;
    final Tile tile;
    URLConnection conn = null;
    String debug = null;

    public MGTileStoreLoaderJob(TileStoreLoader tileStoreLoader, Tile tile){
        this.tileStoreLoader = tileStoreLoader;
        this.tile = tile;
    }

    @Override
    protected void doJob() throws Exception {
        conn = tileStoreLoader.xmlTileSource.getURLConnection(tile.zoomLevel, tile.tileX, tile.tileY);
        debug = conn.getURL() + " "+conn.getRequestProperties();
    }
}
