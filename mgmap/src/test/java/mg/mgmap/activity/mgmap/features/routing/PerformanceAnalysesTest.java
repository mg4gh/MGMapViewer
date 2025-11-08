package mg.mgmap.activity.mgmap.features.routing;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper;
import mg.mgmap.application.util.ElevationProviderImplHelper2;
import mg.mgmap.application.util.HgtProvider2;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.impl2.GGraphTile;
import mg.mgmap.generic.graph.impl2.GGraphTileFactory;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.MGLog;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PerformanceAnalysesTest {


    @Test
    public void _00_load_justData() {

        MGLog.logConfig.put("mg.mgmap", MGLog.Level.WARN);
        MGLog.setUnittest(true);

        PointModelUtil.init(32);

        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        int baseX = 17173;
        int baseY = 11196;

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);

        long start = System.nanoTime();
        for (int x = baseX; x < baseX + 100; x++){
            for (int y = baseY; y<baseY + 100; y++){
                Tile tile = new Tile(x, y, (byte)15, 256);
                wayProvider.getWays(tile);
            }
        }
        System.out.println("R: dt="+((System.nanoTime() - start)/(1000*1000)));

    }

    @Test
    public void _01_load_wo_height() {

        MGLog.logConfig.put("mg.mgmap", MGLog.Level.WARN);
        MGLog.setUnittest(true);

        PointModelUtil.init(32);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        int baseX = 17173;
        int baseY = 11196;


        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(false), new Pref<>("16"), new Pref<>(false));

        BBox bBox = new BBox();
        long start = System.nanoTime();
        int nodes = 0;
        for (int x = baseX; x < baseX + 100; x++){
            for (int y = baseY; y<baseY + 100; y++){
                GGraphTile tile = gGraphTileFactory.loadGGraphTile(x, y);
                bBox.extend(tile.getBBox());
                nodes += tile.getGNodes().size();
            }
        }
        System.out.println("R: dt="+((System.nanoTime() - start)/(1000*1000)));
        System.out.println("BBox: "+bBox);
        System.out.println("nodes: "+nodes);

    }

    @Test
    public void _02_load_with_height() {

        MGLog.logConfig.put("mg.mgmap", MGLog.Level.WARN);
        MGLog.setUnittest(true);

        PointModelUtil.init(32);

        HgtProvider2 hgtProvider = new HgtProvider2();
        ElevationProvider elevationProvider = new ElevationProviderImplHelper2( hgtProvider );
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        int baseX = 17173;
        int baseY = 11196;


        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>("BidirectionalAStar"), new Pref<>(false), new Pref<>("16"), new Pref<>(false));

        long start = System.nanoTime();
        for (int x = baseX; x < baseX + 100; x++){
            for (int y = baseY; y<baseY + 100; y++){
                gGraphTileFactory.loadGGraphTile(x, y);
            }
        }
        System.out.println("R: dt="+((System.nanoTime() - start)/(1000*1000)));

    }

    @Test
    public void _03_load_only_height() {

        MGLog.logConfig.put("mg.mgmap", MGLog.Level.WARN);
        MGLog.setUnittest(true);

        PointModelUtil.init(32);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper2( new HgtProvider2() );
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        int baseX = 17173;
        int baseY = 11196;

        long start = System.nanoTime();
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl();
        for (int x = baseX; x < baseX + 100; x++){
            for (int y = baseY; y<baseY + 100; y++){
                int pointsPerTile = 285;
                Tile tile = new Tile(x,y,(byte)15,256);
                double minLat = tile.getBoundingBox().minLatitude;
                double minLon = tile.getBoundingBox().minLongitude;
                double stepLat = (tile.getBoundingBox().maxLatitude - minLat) / pointsPerTile;
                double stepLon = (tile.getBoundingBox().maxLongitude - minLon) / pointsPerTile;
                for (int k=0; k<pointsPerTile; k++){
                    wpmi.setLat(minLat + k*stepLat );
                    wpmi.setLon(minLon + k*stepLon );
                    elevationProvider.setElevation(wpmi);
                }
            }
        }
        System.out.println("R: dt="+((System.nanoTime() - start)/(1000*1000)));

    }

}
