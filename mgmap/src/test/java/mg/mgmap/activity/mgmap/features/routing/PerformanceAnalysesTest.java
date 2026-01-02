package mg.mgmap.activity.mgmap.features.routing;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.util.regex.Pattern;

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
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(false), new Pref<>("16"));

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
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>("BidirectionalAStar"), new Pref<>(false), new Pref<>("16"));

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

    @Test
    public void _04_regex() {
        Pattern pb = Pattern.compile("^[a-zA-Z0-9]+([.\\-][a-zA-Z0-9]+)*@[a-zA-Z0-9]+([.\\-][a-zA-Z0-9]+)*[.][a-zA-Z0-9]+");
        boolean b1 = pb.matcher("abc.def@ab-de.de").matches();
        boolean b2 = pb.matcher("abc.def@ab-de.de-ef").matches();
        boolean b3 = pb.matcher("abc.def@ab-de.de-ef.gh").matches();
        boolean b4 = pb.matcher("abc..def@ab-de.de").matches();
        boolean b5 = pb.matcher("a-bc.def@ab-de.de").matches();
        boolean b6 = pb.matcher("a-bc.de_f@ab-de.de").matches();
        boolean b7 = pb.matcher(".def@ab-de.de").matches();

        Pattern pc = Pattern.compile("^[a-zA-Z0-9.\\-@]*");
        boolean c1 = pc.matcher("a.def@ab-de.de").matches();
        boolean c2 = pc.matcher("a.def@ab-de.de ").matches();
        boolean c3 = pc.matcher("\na.def@ab-de.de").matches();
        boolean c4 = pc.matcher("a.d@ef@ab-de.de").matches();
        boolean c5 = pc.matcher("a.de%f@ab-de.de").matches();
        boolean c6 = pc.matcher("a.def@ab+de.de").matches();

        Pattern pd = Pattern.compile("[a-f0-9]{8}");
        boolean d1 = pd.matcher("12345678").matches();
        boolean d2 = pd.matcher("123456aa").matches();
        boolean d3 = pd.matcher("12345678a").matches();
        boolean d4 = pd.matcher("1234567").matches();
        boolean d5 = pd.matcher("123456-8").matches();
        boolean d6 = pd.matcher("1fg456d8").matches();
        boolean d7 = pd.matcher("ff3456d8").matches();

        System.out.println(b1);
    }


}
