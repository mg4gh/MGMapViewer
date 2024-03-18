package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.profile.ShortestDistance;
import mg.mgmap.activity.mgmap.features.routing.profile.TrekkingBike;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.AStar;
import mg.mgmap.generic.graph.BidirectionalAStar;
import mg.mgmap.generic.graph.GGraphMulti;
import mg.mgmap.generic.graph.GGraphSearch;
import mg.mgmap.generic.graph.GGraphTile;
import mg.mgmap.generic.graph.GGraphTileFactory;
import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.MGLog;


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
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false);

        RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext);
        routingEngine.setRoutingProfile(new ShortestDistance());

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

    @Test
    public void _02_routing() {
        PointModelUtil.init(32);
//        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false);

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy HH:mm:ss.SSS");
        for (int x=0; x<100; x++){
            System.out.printf(Locale.ENGLISH, "%s x=%03d started%n",sdf.format(new Date()),x );
            GNode gStart = gGraphTileFactory.getGGraphTile(17184+x/2, 11160+x).getNodes().get(5);
            GNode gEnd = gGraphTileFactory.getGGraphTile(17202+x/2, 11179+x).getNodes().get(5);

            ArrayList<GGraphTile> gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(gStart).extend(PointModelUtil.getCloseThreshold()));
            gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(gEnd).extend(PointModelUtil.getCloseThreshold())));
            GGraphMulti multi = new GGraphMulti(gGraphTileFactory, gGraphTiles);


            RoutingProfile rp = new TrekkingBike();

            MultiPointModel mpm1 = performSearch(new BidirectionalAStar(multi, rp), gStart, gEnd);
            MultiPointModel mpm2 = performSearch(new AStar(multi, rp), gStart, gEnd);

            Assert.assertEquals(mpm1.size(), mpm2.size());
            for (int i=0; i<mpm1.size(); i++){
                Assert.assertEquals(mpm1.get(i),mpm2.get(i));
            }
        }

    }



    private MultiPointModel performSearch(GGraphSearch routingAlg, GNode gStart, GNode gEnd){
        ArrayList<PointModel> relaxed = new ArrayList<>();
        MultiPointModel mpm = routingAlg.perform(gStart, gEnd, 100000, new AtomicInteger(),relaxed);
        System.out.println(routingAlg.getResult().replaceAll("\n","    "));
        return mpm;
    }
}
