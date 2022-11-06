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

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.layer.queue.Job;

import java.io.File;

import mg.mgmap.generic.util.BgJob;

public class MGTileStoreFiles extends MGTileStore {

    private static final String SUFFIX = ".png";
    public MGTileStoreFiles(File storeDir, GraphicFactory graphicFactory){
        super(storeDir, SUFFIX, graphicFactory);
    }

    @Override
    public float getDefaultAlpha() {
        return 0.2f;
    }

    @Override
    public synchronized boolean containsKey(Job key) {
        File zoomDir = new File(storeDir, Byte.toString(key.tile.zoomLevel));
        if (!zoomDir.exists()) return false;

        File xDir = new File(zoomDir, Integer.toString(key.tile.tileX));
        if (!zoomDir.exists()) return false;

        File yFile = new File(xDir, key.tile.tileY + SUFFIX);
        return yFile.exists();
    }

    @Override
    public BgJob getLoaderJob(TileStoreLoader tileStoreLoader, Tile tile, boolean bOld) {
        return new MGTileStoreLoaderJobFile(tileStoreLoader, tile);
    }

    public BgJob getDropJob(TileStoreLoader tileStoreLoader, int tileXMin, int tileXMax, int tileYMin, int tileYMax, byte zoomLevel){
        return new MGTileStoreDropJobFile(tileStoreLoader,tileXMin,tileXMax,tileYMin,tileYMax,zoomLevel);
    }

}
