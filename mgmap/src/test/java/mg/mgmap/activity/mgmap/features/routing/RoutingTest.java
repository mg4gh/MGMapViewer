package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.GGraphTileFactory;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.util.WayProvider;


public class RoutingTest {

    @Test
    public void _01_routing() {

        RoutingContext interactiveRoutingContext = new RoutingContext(
                10000,
                false, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                1); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider);

        RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext);

        {   // einfache Strecke Nahe Spyrer Hof
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.396434,8.708706));
            mtl.addPoint(new PointModelImpl(49.397147,8.705246));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            Assert.assertEquals(476.68, rotl.getTrackStatistic().getTotalLength(), 0.01);
            Assert.assertEquals(52, rotl.getTrackStatistic().getNumPoints());
        }
        {   // Problemfall Hollmuth
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.365779,8.794608));
            mtl.addPoint(new PointModelImpl(49.371844,8.795191));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            Assert.assertEquals(680.29, rotl.getTrackStatistic().getTotalLength(), 0.01);
            Assert.assertEquals(8, rotl.getTrackStatistic().getNumPoints());
        }
        {   // Problemfall B45 Bammentaler Strasse
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.382485,8.788726));
            mtl.addPoint(new PointModelImpl(49.361620,8.789067));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            Assert.assertEquals(2412.19, rotl.getTrackStatistic().getTotalLength(), 0.01);
            Assert.assertEquals(77, rotl.getTrackStatistic().getNumPoints());
        }

        {   // Problemfall Heiligkreuzsteinach
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.485806,8.791403));
            mtl.addPoint(new PointModelImpl(49.485658,8.789728));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            Assert.assertEquals(211.55, rotl.getTrackStatistic().getTotalLength(), 0.01);
            Assert.assertEquals(13, rotl.getTrackStatistic().getNumPoints());
        }



    }
}
