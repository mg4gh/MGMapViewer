package mg.mgmap.util;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.Way;

import java.util.List;

public interface WayProvider {

    List<Way> getWays(Tile tile);

    boolean isHighway(Way way);
}
