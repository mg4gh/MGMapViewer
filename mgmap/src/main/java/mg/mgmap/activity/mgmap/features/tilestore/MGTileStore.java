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
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.cache.TileStore;
import org.mapsforge.map.layer.queue.Job;

import java.io.File;
import java.io.FilenameFilter;

import mg.mgmap.generic.util.BgJob;

public abstract class MGTileStore extends TileStore {

    protected int tileSize = 256;
    File storeDir;

    public MGTileStore(File storeDir, String suffix, GraphicFactory graphicFactory){
        super(storeDir, suffix, graphicFactory);
        this.storeDir = storeDir;
    }

    public static MGTileStore createTileStore(File storeDir){
        String[] files = storeDir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".mbtiles");
            }
        });
        if (files.length == 1){ // ok, this tile store is based on a .mbtiles file
            return new MGTileStoreDB(storeDir, files[0], AndroidGraphicFactory.INSTANCE);
        } else {
            return new MGTileStoreFiles(storeDir, AndroidGraphicFactory.INSTANCE);
        }
    }

    @Override
    public synchronized boolean containsKey(Job key) {
        return true;
    }

    public abstract float getDefaultAlpha();

    public boolean hasConfig(){
        return ( new File(storeDir,"config.xml").exists() );
    }

    public File getStoreDir() {
        return storeDir;
    }

    public int getTileSize(){
        return tileSize;
    }

    public abstract BgJob getLoaderJob(TileStoreLoader tileStoreLoader, Tile tile);

    public abstract BgJob getDropJob(TileStoreLoader tileStoreLoader, int tileXMin, int tileXMax, int tileYMin, int tileYMax, byte zoomLevel);
}
