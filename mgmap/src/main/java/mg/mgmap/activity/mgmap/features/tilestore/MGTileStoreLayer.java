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
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.tilestore.TileStoreLayer;
import org.mapsforge.map.model.MapViewPosition;

public class MGTileStoreLayer extends TileStoreLayer {

    final MGTileStore mgTileStore;

    public MGTileStoreLayer(MGTileStore mgTileStore, TileCache tileCache, MapViewPosition mapViewPosition, GraphicFactory graphicFactory, boolean isTransparent) {
        super(tileCache, mapViewPosition, graphicFactory, isTransparent);
        this.mgTileStore = mgTileStore;
    }

    public MGTileStore getMGTileStore(){
        return mgTileStore;
    }

    @Override
    protected void onRemove() {
        super.onRemove();
        mgTileStore.destroy();
    }
}
