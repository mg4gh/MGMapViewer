package mg.mgmap.generic.graph.test;

import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.Way;

import java.util.List;

import mg.mgmap.application.util.ElevationProvider;

public abstract class Test {

    public void test(ElevationProvider elevationProvider, Tile tile, List<Way> ways){
        long start = System.nanoTime();
        loadWays(elevationProvider, tile, ways);
        long end = System.nanoTime();
        summary(start, end);
    }

    protected abstract void loadWays(ElevationProvider elevationProvider, Tile tile, List<Way> ways);

    protected abstract void summary(long start, long end);
}
