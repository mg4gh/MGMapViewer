package mg.mgmap.application.util;

import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.Way;

import java.util.List;

import mg.mgmap.generic.util.WayProvider;

public class WayProviderHelper implements WayProvider {

    MapDataStore mds;

    public WayProviderHelper(MapDataStore mds){
        this.mds = mds;
    }
    public List<Way> getWays(Tile tile){
        return mds.readMapData(tile).ways;
    }


    public boolean isWayForRouting(Way way){
        for (Tag tag : way.tags){
            if (tag.key.equals("highway")){
                return true;
            }
            if (tag.key.equals("route") && tag.value.equals("ferry")) {
                return true;
            }
        }
        return false;
    }

}
