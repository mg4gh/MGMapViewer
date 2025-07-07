package mg.mgmap.generic.graph.test.ggraph;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.Way;

import java.util.Collections;
import java.util.List;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.impl.GGraphTile;
import mg.mgmap.generic.graph.impl.GGraphTileFactory;
import mg.mgmap.generic.graph.test.Test;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;

public class GTest extends Test {


    private class GWayProvider implements WayProvider{

        @Override
        public List<Way> getWays(Tile tile) {
            return GTest.this.ways;
        }

        @Override
        public boolean isWayForRouting(Way way) {
            return true;
        }
    }

    GGraphTile gTile = null;
    GGraphTileFactory factory;
    List<Way> ways = null;

    public GTest(ElevationProvider elevationProvider){
        factory = new GGraphTileFactory().onCreate(new GWayProvider(), elevationProvider, false, new Pref<>(""), new Pref<>(true));
    }

    @Override
    protected void loadWays(ElevationProvider elevationProvider, Tile tile, List<Way> ways) {
        this.ways = ways;
        gTile = factory.getGGraphTile(tile.tileX, tile.tileY);

    }

    @Override
    protected void summary(long start, long end) {
        gTile.timestamps.add(0,start);
        gTile.timestamps.add(end);
        String elapsed = "elapsed="+(end-start)/1000+" details: ";
        for (int i=1; i<gTile.timestamps.size(); i++){
            elapsed += (gTile.timestamps.get(i) - gTile.timestamps.get(i-1))/1000 +" ";
        }
        System.out.println("GTest: numObjects="+gTile.getNodes().size()+" "+elapsed );

    }
}
