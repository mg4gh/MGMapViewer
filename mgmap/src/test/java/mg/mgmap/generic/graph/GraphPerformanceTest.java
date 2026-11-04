package mg.mgmap.generic.graph;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.activity.mgmap.features.routing.profile.TrekkingBike;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper2;
import mg.mgmap.application.util.HgtProvider2;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.impl2.GGraphMulti;
import mg.mgmap.generic.graph.impl2.GGraphTile;
import mg.mgmap.generic.graph.impl2.GGraphTileFactory;
import mg.mgmap.generic.graph.impl4.DApproach;
import mg.mgmap.generic.graph.impl4.DGraphMulti;
import mg.mgmap.generic.graph.impl4.DGraphTile;
import mg.mgmap.generic.graph.impl4.DGraphTileFactory;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.MGLog;

@FixMethodOrder(MethodSorters.DEFAULT)
public class GraphPerformanceTest {
    RoutingProfile rp = new TrekkingBike();


    PointModel pm1 = new PointModelImpl(49.405673,8.679079);
//    PointModel pm2 = new PointModelImpl(49.406390, 8.679864);
//    PointModel pm2 = new PointModelImpl(49.406624, 8.683024);
//    PointModel pm2 = new PointModelImpl(49.386410,8.703825);
    PointModel pm2 = new PointModelImpl(49.136262, 8.914056);
//    PointModel pm2 = new PointModelImpl(49.792982, 9.925589);

    @Test
    public void test01__DGraphRoute1() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.WARN);
        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper2( new HgtProvider2() );
        File mapFile = new File("src/test/assets/map_local/Germany-South_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);

        Pref<Boolean> prefWayDetails = new Pref<>(false);
        Pref<String> prefRoutingAlgorithm = new Pref<>("BidirectionalAstarExt");
        Pref<Boolean> prefSmooth4Routing = new Pref<>(false);
        Pref<String> prefSmoothDistance = new Pref<>("16");
        DGraphTileFactory graphTileFactory = new DGraphTileFactory().onCreate(wayProvider,elevationProvider,prefWayDetails,
                prefRoutingAlgorithm, prefSmooth4Routing, prefSmoothDistance);

        ArrayList<DGraphTile> tiles = new ArrayList<>();

        {
            DApproach dpm1 = graphTileFactory.calcApproach(pm1, PointModelUtil.getCloseThreshold());
            assert (dpm1 != null);
            assert (dpm1.getApproachNode() != null);
            tiles.add(graphTileFactory.getGraphTile(dpm1));


            DApproach dpm2 = graphTileFactory.calcApproach(pm2, PointModelUtil.getCloseThreshold());
            assert (dpm2 != null);
            assert (dpm2.getApproachNode() != null);
            tiles.add(graphTileFactory.getGraphTile(dpm2));

            DGraphMulti graphMulti = new DGraphMulti(graphTileFactory, tiles);

            graphTileFactory.connectApproach2Graph(graphMulti, dpm1);
            graphTileFactory.connectApproach2Graph(graphMulti, dpm2);
            graphTileFactory.checkDirectConnectApproaches(graphMulti, dpm1, dpm2);

            GraphAlgorithm algorithm = graphTileFactory.getAlgorithmForGraph(graphMulti, rp);
            MultiPointModel mpm1 = algorithm.perform(dpm1, dpm2, Double.MAX_VALUE, new AtomicInteger(), new ArrayList<>());
            System.out.println(algorithm.getResult());
            graphMulti.finalizeUsage();
            graphTileFactory.serviceCache();
            graphTileFactory.disconnectApproach2Graph(graphMulti, dpm1);
            graphTileFactory.disconnectApproach2Graph(graphMulti, dpm2);


            System.out.printf(Locale.ENGLISH, "distance: %.1f%n", PointModelUtil.distance(mpm1));
            System.out.printf(Locale.ENGLISH, "%s\n", graphTileFactory.renewStatistics());

        }
        System.out.printf(Locale.ENGLISH, "************************************* end 1  *****************************************************\n");
        System.gc();
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.printf(Locale.ENGLISH, "************************************* start 2*****************************************************\n");

        {
            DApproach dpm1 = graphTileFactory.calcApproach(pm1, PointModelUtil.getCloseThreshold());
            assert (dpm1 != null);
            assert (dpm1.getApproachNode() != null);
            tiles.add(graphTileFactory.getGraphTile(dpm1));

            DApproach dpm2 = graphTileFactory.calcApproach(pm2, PointModelUtil.getCloseThreshold());
            assert (dpm2 != null);
            assert (dpm2.getApproachNode() != null);
            tiles.add(graphTileFactory.getGraphTile(dpm2));

            DGraphMulti graphMulti = new DGraphMulti(graphTileFactory, tiles);

            graphTileFactory.connectApproach2Graph(graphMulti, dpm1);
            graphTileFactory.connectApproach2Graph(graphMulti, dpm2);
            graphTileFactory.checkDirectConnectApproaches(graphMulti, dpm1, dpm2);

            GraphAlgorithm algorithm = graphTileFactory.getAlgorithmForGraph(graphMulti, rp);
            MultiPointModel mpm1 = algorithm.perform(dpm1, dpm2, Double.MAX_VALUE, new AtomicInteger(), new ArrayList<>());
            System.out.println(algorithm.getResult());
            graphMulti.finalizeUsage();
            graphTileFactory.serviceCache();
            graphTileFactory.disconnectApproach2Graph(graphMulti, dpm1);
            graphTileFactory.disconnectApproach2Graph(graphMulti, dpm2);

            System.out.printf(Locale.ENGLISH, "distance: %.1f%n", PointModelUtil.distance(mpm1));
            System.out.printf(Locale.ENGLISH, "%s\n", graphTileFactory.renewStatistics());

        }

        for (RoutingSummary summary : RoutingSummary.routingSummaries) {
            System.out.printf(Locale.ENGLISH, "%s\n",summary);
        }
    }

    @Test
    public void test02__DGraphRoute2() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.WARN);
        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper2( new HgtProvider2() );
        File mapFile = new File("src/test/assets/map_local/Germany-South_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);

        Pref<Boolean> prefWayDetails = new Pref<>(false);
        Pref<String> prefRoutingAlgorithm = new Pref<>("BidirectionalAstarExt");
        Pref<Boolean> prefSmooth4Routing = new Pref<>(false);
        Pref<String> prefSmoothDistance = new Pref<>("16");
        GGraphTileFactory graphTileFactory = new GGraphTileFactory().onCreate(wayProvider,elevationProvider,prefWayDetails,
                prefRoutingAlgorithm, prefSmooth4Routing, prefSmoothDistance);

        ArrayList<GGraphTile> tiles = new ArrayList<>();


        {
            ApproachModel dpm1 = graphTileFactory.calcApproach(pm1, PointModelUtil.getCloseThreshold());
            assert (dpm1 != null);
            assert (dpm1.getApproachNode() != null);
            tiles.add(graphTileFactory.getGraphTile(dpm1));

            ApproachModel dpm2 = graphTileFactory.calcApproach(pm2, PointModelUtil.getCloseThreshold());
            assert (dpm2 != null);
            assert (dpm2.getApproachNode() != null);
            tiles.add(graphTileFactory.getGraphTile(dpm2));

            GGraphMulti graphMulti = new GGraphMulti(graphTileFactory, tiles);

            graphTileFactory.connectApproach2Graph(graphMulti, dpm1);
            graphTileFactory.connectApproach2Graph(graphMulti, dpm2);
            graphTileFactory.checkDirectConnectApproaches(graphMulti, dpm1, dpm2);

            GraphAlgorithm algorithm = graphTileFactory.getAlgorithmForGraph(graphMulti, rp);
            MultiPointModel mpm1 = algorithm.perform(dpm1, dpm2, Double.MAX_VALUE, new AtomicInteger(), new ArrayList<>());
            System.out.printf(Locale.ENGLISH, "distance: %.1f%n", PointModelUtil.distance(mpm1));

            graphMulti.finalizeUsage();
            graphTileFactory.serviceCache();
            graphTileFactory.disconnectApproach2Graph(graphMulti, dpm1);
            graphTileFactory.disconnectApproach2Graph(graphMulti, dpm2);
        }

        {
            ApproachModel dpm1 = graphTileFactory.calcApproach(pm1, PointModelUtil.getCloseThreshold());
            assert (dpm1 != null);
            assert (dpm1.getApproachNode() != null);
            tiles.add(graphTileFactory.getGraphTile(dpm1));

            ApproachModel dpm2 = graphTileFactory.calcApproach(pm2, PointModelUtil.getCloseThreshold());
            assert (dpm2 != null);
            assert (dpm2.getApproachNode() != null);
            tiles.add(graphTileFactory.getGraphTile(dpm2));

            GGraphMulti graphMulti = new GGraphMulti(graphTileFactory, tiles);

            graphTileFactory.connectApproach2Graph(graphMulti, dpm1);
            graphTileFactory.connectApproach2Graph(graphMulti, dpm2);
            graphTileFactory.checkDirectConnectApproaches(graphMulti, dpm1, dpm2);

            GraphAlgorithm algorithm = graphTileFactory.getAlgorithmForGraph(graphMulti, rp);
            MultiPointModel mpm1 = algorithm.perform(dpm1, dpm2, Double.MAX_VALUE, new AtomicInteger(), new ArrayList<>());
            System.out.printf(Locale.ENGLISH, "distance: %.1f%n", PointModelUtil.distance(mpm1));

            graphMulti.finalizeUsage();
            graphTileFactory.serviceCache();
            graphTileFactory.disconnectApproach2Graph(graphMulti, dpm1);
            graphTileFactory.disconnectApproach2Graph(graphMulti, dpm2);
        }

        for (RoutingSummary summary : RoutingSummary.routingSummaries) {
            System.out.printf(Locale.ENGLISH, "%s\n",summary);
        }
    }


}
