package mg.mgmap.generic.graph.impl5;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.MapViewerBase;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.activity.mgmap.features.routing.profile.ShortestDistance;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper;
import mg.mgmap.application.util.WayProviderHelper4DGraph;
import mg.mgmap.generic.graph.GraphAlgorithm;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.MGLog;

public class DGraphTest {

    @Test
    public void testDGraphSetup1() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        WayProvider wayProvider = new WayProviderHelper4DGraph();

        Pref<Boolean> prefWayDetails = new Pref<>(false);
        Pref<String> prefRoutingAlgorithm = new Pref<>("BidirectionalAStar");
        Pref<Boolean> prefSmooth4Routing = new Pref<>(false);
        Pref<String> prefSmoothDistance = new Pref<>("16");
        DGraphTileFactory dGraphTileFactory = new DGraphTileFactory().onCreate(wayProvider,elevationProvider,prefWayDetails,
                prefRoutingAlgorithm, prefSmooth4Routing, prefSmoothDistance);

        DGraphTile dGraphTile = dGraphTileFactory.getDGraphTile(8586,5598);

        assert (dGraphTile.initialNodeCnt == 10);
        assert (dGraphTile.borderNodeCnt == 1);
        assert (dGraphTile.initialNeighbourCnt == 28);

        dumpDGraphTile(dGraphTile);

        PointModel pm1 = new PointModelImpl(49.400010,8.663);
        DApproach dpm1 = dGraphTileFactory.calcApproach(pm1, PointModelUtil.getCloseThreshold());
        assert (dpm1 != null);
        assert( dpm1.tileIdx == DGraphTileFactory.getTileIdx(8586,5598));

        PointModel pm2 = new PointModelImpl(49.400010,8.667);
        DApproach dpm2 = dGraphTileFactory.calcApproach(pm2, PointModelUtil.getCloseThreshold());
        assert (dpm2 != null);
        assert( dpm2.tileIdx == DGraphTileFactory.getTileIdx(8586,5598));

        RoutingProfile rp = new ShortestDistance();
        ArrayList<DGraphTile> tiles = new ArrayList<>();
        tiles.add(dGraphTile);
        DGraphMulti dGraphMulti = new DGraphMulti(dGraphTileFactory, tiles);

        GraphAlgorithm algorithm = dGraphTileFactory.getAlgorithmForGraph(dGraphMulti,rp);
        MultiPointModel mpm = algorithm.perform(dpm1, dpm2, 100000, new AtomicInteger(), new ArrayList<>());
        System.out.println(algorithm.getResult());
        for (PointModel pm : mpm) {
            System.out.println(pm.toString());
        }
        double distance = PointModelUtil.distance(mpm);
        System.out.printf(Locale.ENGLISH, "distance: %.1f%n",distance);
        Assert.assertEquals(289.8,distance, 0.1);
    }

    @Test
    public void testDGraphSetup2() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        WayProvider wayProvider = new WayProviderHelper4DGraph();

        Pref<Boolean> prefWayDetails = new Pref<>(false);
        Pref<String> prefRoutingAlgorithm = new Pref<>("BidirectionalAStar");
        Pref<Boolean> prefSmooth4Routing = new Pref<>(false);
        Pref<String> prefSmoothDistance = new Pref<>("16");
        DGraphTileFactory dGraphTileFactory = new DGraphTileFactory().onCreate(wayProvider,elevationProvider,prefWayDetails,
                prefRoutingAlgorithm, prefSmooth4Routing, prefSmoothDistance);

        DGraphTile dGraphTile = dGraphTileFactory.getDGraphTile(8586,5598);

        assert (dGraphTile.initialNodeCnt == 10);
        assert (dGraphTile.borderNodeCnt == 1);
        assert (dGraphTile.initialNeighbourCnt == 28);

        dumpDGraphTile(dGraphTile);

        PointModel pm1 = new PointModelImpl(49.400010,8.663);
        DApproach dpm1 = dGraphTileFactory.calcApproach(pm1, PointModelUtil.getCloseThreshold());
        assert (dpm1 != null);
        assert( dpm1.tileIdx == DGraphTileFactory.getTileIdx(8586,5598));

        PointModel pm2 = new PointModelImpl(49.410010,8.6655);
        DApproach dpm2 = dGraphTileFactory.calcApproach(pm2, PointModelUtil.getCloseThreshold());
        assert (dpm2 != null);
        assert( dpm2.tileIdx == DGraphTileFactory.getTileIdx(8586,5598));

        RoutingProfile rp = new ShortestDistance();
        ArrayList<DGraphTile> tiles = new ArrayList<>();
        tiles.add(dGraphTile);
        DGraphMulti dGraphMulti = new DGraphMulti(dGraphTileFactory, tiles);

        GraphAlgorithm algorithm = dGraphTileFactory.getAlgorithmForGraph(dGraphMulti,rp);
        MultiPointModel mpm = algorithm.perform(dpm1, dpm2, 100000, new AtomicInteger(), new ArrayList<>());
        System.out.println(algorithm.getResult());
        for (PointModel pm : mpm) {
            System.out.println(pm.toString());
        }
        double distance = PointModelUtil.distance(mpm);
        System.out.printf(Locale.ENGLISH, "distance: %.1f%n",distance);
        Assert.assertEquals(1728.8,distance, 0.1);
    }



    @Test
    public void testDGraphSetup3() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        WayProvider wayProvider = new WayProviderHelper4DGraph();

        Pref<Boolean> prefWayDetails = new Pref<>(false);
        Pref<String> prefRoutingAlgorithm = new Pref<>("BidirectionalAStar");
        Pref<Boolean> prefSmooth4Routing = new Pref<>(false);
        Pref<String> prefSmoothDistance = new Pref<>("16");
        DGraphTileFactory dGraphTileFactory = new DGraphTileFactory().onCreate(wayProvider,elevationProvider,prefWayDetails,
                prefRoutingAlgorithm, prefSmooth4Routing, prefSmoothDistance);

        DGraphTile dGraphTile1 = dGraphTileFactory.getDGraphTile(8586,5598);
        assert (dGraphTile1.initialNodeCnt == 10);
        assert (dGraphTile1.borderNodeCnt == 1);
        assert (dGraphTile1.initialNeighbourCnt == 28);

        DGraphTile dGraphTile2 = dGraphTileFactory.getDGraphTile(8586,5597);
        assert (dGraphTile2.initialNodeCnt == 4);
        assert (dGraphTile2.borderNodeCnt == 1);
        assert (dGraphTile2.initialNeighbourCnt == 6);

        PointModel pm1 = new PointModelImpl(49.400010,8.663);
        DApproach dpm1 = dGraphTileFactory.calcApproach(pm1, PointModelUtil.getCloseThreshold());
        assert (dpm1 != null);
        assert( dpm1.tileIdx == DGraphTileFactory.getTileIdx(8586,5598));

        PointModel pm2 = new PointModelImpl(49.420010,8.667);
        DApproach dpm2 = dGraphTileFactory.calcApproach(pm2, PointModelUtil.getCloseThreshold());
        assert (dpm2 != null);
        assert( dpm2.tileIdx == DGraphTileFactory.getTileIdx(8586,5597));

        RoutingProfile rp = new ShortestDistance();
        ArrayList<DGraphTile> tiles = new ArrayList<>();
        tiles.add(dGraphTile1);
        tiles.add(dGraphTile2);
        DGraphMulti dGraphMulti = new DGraphMulti(dGraphTileFactory, tiles);

        dumpDGraphTile(dGraphTile1);
        dumpDGraphTile(dGraphTile2);


        GraphAlgorithm algorithm = dGraphTileFactory.getAlgorithmForGraph(dGraphMulti,rp);
        MultiPointModel mpm = algorithm.perform(dpm1, dpm2, 100000, new AtomicInteger(), new ArrayList<>());
        System.out.println(algorithm.getResult());
        for (PointModel pm : mpm) {
            System.out.println(pm.toString());
        }
        double distance = PointModelUtil.distance(mpm);
        System.out.printf(Locale.ENGLISH, "distance: %.1f%n",distance);
        Assert.assertEquals(2858.2,distance, 0.1);
    }

    @Test
    public void testDGraphSetup4() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        WayProvider wayProvider = new WayProviderHelper4DGraph();

        Pref<Boolean> prefWayDetails = new Pref<>(false);
        Pref<String> prefRoutingAlgorithm = new Pref<>("BidirectionalAStar");
        Pref<Boolean> prefSmooth4Routing = new Pref<>(false);
        Pref<String> prefSmoothDistance = new Pref<>("16");
        DGraphTileFactory dGraphTileFactory = new DGraphTileFactory().onCreate(wayProvider,elevationProvider,prefWayDetails,
                prefRoutingAlgorithm, prefSmooth4Routing, prefSmoothDistance);

        DGraphTile dGraphTile1 = dGraphTileFactory.getDGraphTile(8586,5598);
        assert (dGraphTile1.initialNodeCnt == 10);
        assert (dGraphTile1.borderNodeCnt == 1);
        assert (dGraphTile1.initialNeighbourCnt == 28);

        DGraphTile dGraphTile2 = dGraphTileFactory.getDGraphTile(8586,5597);
        assert (dGraphTile2.initialNodeCnt == 4);
        assert (dGraphTile2.borderNodeCnt == 1);
        assert (dGraphTile2.initialNeighbourCnt == 6);

        PointModel pm1 = new PointModelImpl(49.400010,8.663);
        DApproach dpm1 = dGraphTileFactory.calcApproach(pm1, PointModelUtil.getCloseThreshold());
        assert (dpm1 != null);
        assert( dpm1.tileIdx == DGraphTileFactory.getTileIdx(8586,5598));

        PointModel pm2 = new PointModelImpl(49.420010,8.667);
        DApproach dpm2 = dGraphTileFactory.calcApproach(pm2, PointModelUtil.getCloseThreshold());
        assert (dpm2 != null);
        assert( dpm2.tileIdx == DGraphTileFactory.getTileIdx(8586,5597));

        RoutingProfile rp = new ShortestDistance();
        ArrayList<DGraphTile> tiles = new ArrayList<>();
        tiles.add(dGraphTile1);
        tiles.add(dGraphTile2);
        DGraphMulti dGraphMulti = new DGraphMulti(dGraphTileFactory, tiles);

        dumpDGraphTile(dGraphTile1);
        dumpDGraphTile(dGraphTile2);


        GraphAlgorithm algorithm = dGraphTileFactory.getAlgorithmForGraph(dGraphMulti,rp);
        MultiPointModel mpm = algorithm.perform(dpm2, dpm1, 100000, new AtomicInteger(), new ArrayList<>());
        System.out.println(algorithm.getResult());
        for (PointModel pm : mpm) {
            System.out.println(pm.toString());
        }
        double distance = PointModelUtil.distance(mpm);
        System.out.printf(Locale.ENGLISH, "distance: %.1f%n",distance);
        Assert.assertEquals(2858.2,distance, 0.1);
    }

    @Test
    public void testDGraphSetup10() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        WayProvider wayProvider = new WayProviderHelper4DGraph();

        Pref<Boolean> prefWayDetails = new Pref<>(false);
        Pref<String> prefRoutingAlgorithm = new Pref<>("BidirectionalAStar");
        Pref<Boolean> prefSmooth4Routing = new Pref<>(false);
        Pref<String> prefSmoothDistance = new Pref<>("16");
        DGraphTileFactory dGraphTileFactory = new DGraphTileFactory().onCreate(wayProvider,elevationProvider,prefWayDetails,
                prefRoutingAlgorithm, prefSmooth4Routing, prefSmoothDistance, Config.ZOOM_LEVEL, 2, MapViewerBase.TILE_SIZE);

        DGraphTile dGraphTile1 = dGraphTileFactory.getDGraphTile(8586,5598);
        assert (dGraphTile1.initialNodeCnt == 10);
        assert (dGraphTile1.borderNodeCnt == 1);
        assert (dGraphTile1.initialNeighbourCnt == 28);

        DGraphTile dGraphTile2 = dGraphTileFactory.getDGraphTile(8586,5597);
        assert (dGraphTile2.initialNodeCnt == 4);
        assert (dGraphTile2.borderNodeCnt == 1);
        assert (dGraphTile2.initialNeighbourCnt == 6);

        PointModel pm1 = new PointModelImpl(49.400010,8.663);
        DApproach dpm1 = dGraphTileFactory.calcApproach(pm1, PointModelUtil.getCloseThreshold());
        assert (dpm1 != null);
        assert( dpm1.tileIdx == DGraphTileFactory.getTileIdx(8586,5598));

        PointModel pm2 = new PointModelImpl(49.420010,8.667);
        DApproach dpm2 = dGraphTileFactory.calcApproach(pm2, PointModelUtil.getCloseThreshold());
        assert (dpm2 != null);
        assert( dpm2.tileIdx == DGraphTileFactory.getTileIdx(8586,5597));

        dGraphTileFactory.getDGraphTile(8586,5596); // this should kick DGraphTile-(8586,5598) out of the chache
        System.out.println( dGraphTileFactory.getCached() );

        RoutingProfile rp = new ShortestDistance();
        ArrayList<DGraphTile> tiles = new ArrayList<>();
        tiles.add(dGraphTile1);
        tiles.add(dGraphTile2);
        DGraphMulti dGraphMulti = new DGraphMulti(dGraphTileFactory, tiles);

        dumpDGraphTile(dGraphTile1);
        dumpDGraphTile(dGraphTile2);


        GraphAlgorithm algorithm = dGraphTileFactory.getAlgorithmForGraph(dGraphMulti,rp);
        MultiPointModel mpm = algorithm.perform(dpm2, dpm1, 100000, new AtomicInteger(), new ArrayList<>());
        System.out.println(algorithm.getResult());
        for (PointModel pm : mpm) {
            System.out.println(pm.toString());
        }
        double distance = PointModelUtil.distance(mpm);
        System.out.printf(Locale.ENGLISH, "distance: %.1f%n",distance);
        Assert.assertEquals(2858.2,distance, 0.1);
    }



    public void dumpDGraphTile(DGraphTile dGraphTile){
        System.out.println("************************* Nodes "+dGraphTile+"*************************");
        for (short nodeIdx=0; nodeIdx<dGraphTile.maxNodeCnt; nodeIdx++){
            System.out.println(dGraphTile.nodeToString(nodeIdx));
        }
        System.out.println("************************* End Nodes "+dGraphTile+"*************************");
        System.out.println("************************* Neighbours "+dGraphTile+"*************************");
        for (short neighbourIdx=0; neighbourIdx<dGraphTile.maxNeighbourCnt; neighbourIdx++){
            System.out.println(dGraphTile.neighbourToString(neighbourIdx));
        }
        System.out.println("************************* End Neighbours "+dGraphTile+"*************************");
    }

}
