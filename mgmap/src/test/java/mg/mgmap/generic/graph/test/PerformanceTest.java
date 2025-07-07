package mg.mgmap.generic.graph.test;

import org.junit.Test;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper;
import mg.mgmap.application.util.WayProviderHelper;

import mg.mgmap.generic.graph.test.agraph.ATest;
import mg.mgmap.generic.graph.test.bgraph.BTest;
import mg.mgmap.generic.graph.test.ggraph.GTest;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.MGLog;

public class PerformanceTest {

    private MGLog mgLog;

    @Test
    public void testBObjects(){

        PointModelUtil.init(32);
//        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);
        mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Germany-South_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath() + " " + mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);

        int tileX = 17174;
        int tileY = 11197;
        byte ZOOM_LEVEL = 15;
        int TILE_SIZE = 256;

        for (int x=17174; x<17174+15; x++){
            for (int y=11197; y<11197+15; y++){
                Tile tile = new Tile(x, y, ZOOM_LEVEL, TILE_SIZE);

                List<Way> ways = wayProvider.getWays(tile);
                ways.removeIf(way->!wayProvider.isWayForRouting(way));

                System.out.println("x="+x+" y="+y);
                new ATest().test(elevationProvider, tile, ways);
//                new BTest().test(elevationProvider, tile, ways);
                new GTest(elevationProvider).test(elevationProvider, tile, ways);

            }
        }


    }
}
