package mg.mgmap.generic.graph.test.agraph;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.Way;

import java.util.List;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.test.Test;
import mg.mgmap.generic.graph.test.bgraph.BTile;

public class ATest extends Test {


    ATile aTile = null;

    @Override
    protected void loadWays(ElevationProvider elevationProvider, Tile tile, List<Way> ways) {
        aTile = new ATile(elevationProvider, tile);
        aTile.init(ways);
    }

    @Override
    protected void summary(long start, long end) {
        aTile.timestamps.add(0,start);
        aTile.timestamps.add(end);
        String elapsed = "elapsed="+(end-start)/1000+" details: ";
        for (int i=1; i<aTile.timestamps.size(); i++){
            elapsed += (aTile.timestamps.get(i) - aTile.timestamps.get(i-1))/1000 +" ";
        }
        System.out.println("ATest: numObjects="+aTile.nodes.nodesUsed+" "+elapsed );

    }
}
