package mg.mapviewer.features.tilestore;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.tilestore.TileStoreLayer;
import org.mapsforge.map.model.IMapViewPosition;

public class MGTileStoreLayer extends TileStoreLayer {

    MGTileStore mgTileStore;

    public MGTileStoreLayer(MGTileStore mgTileStore, TileCache tileCache, IMapViewPosition mapViewPosition, GraphicFactory graphicFactory, boolean isTransparent) {
        super(tileCache, mapViewPosition, graphicFactory, isTransparent);
        this.mgTileStore = mgTileStore;
    }

    public MGTileStore getMGTileStore(){
        return mgTileStore;
    }
}
