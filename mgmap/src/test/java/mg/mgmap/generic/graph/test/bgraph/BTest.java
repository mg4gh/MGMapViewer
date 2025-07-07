package mg.mgmap.generic.graph.test.bgraph;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.Way;

import java.util.List;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.test.Test;

public class BTest extends Test {


    BTile bTile = null;

    @Override
    protected void loadWays(ElevationProvider elevationProvider, Tile tile, List<Way> ways) {
        bTile = new BTile(elevationProvider, tile);
        bTile.init(ways);
    }

    @Override
    protected void summary(long start, long end) {
        System.out.println("BTest: numObjects="+bTile.nodes.nodesUsed+" elapsed="+(end - start)/1000 );

    }
}
