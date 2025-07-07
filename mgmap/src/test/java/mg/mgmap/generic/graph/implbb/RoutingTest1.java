package mg.mgmap.generic.graph.implbb;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingContext;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S2;
import mg.mgmap.activity.mgmap.features.routing.profile.ShortestDistance;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.impl.BidirectionalAStar;
import mg.mgmap.generic.graph.impl.GGraphMulti;
import mg.mgmap.generic.graph.impl.GGraphTile;
import mg.mgmap.generic.graph.impl.GGraphTileFactory;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.MGLog;

public class RoutingTest1 {

    private MGLog mgLog;

    @Test
    public void _01_test1() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);
        mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

        PointModelUtil.init(32);

        RoutingContext interactiveRoutingContext = new RoutingContext(
                10000,
                false, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                10); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath() + " " + mapFile.exists());

        PointModelImpl pm1 = new PointModelImpl(49.402665, 8.686337);
//        PointModelImpl pm2 = new PointModelImpl(49.401867, 8.687130);
        PointModelImpl pm2 = new PointModelImpl(49.401967, 8.687125);
        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(false));

        ArrayList<GGraphTile> tiles = gGraphTileFactory.getGGraphTileList(new BBox().extend(pm1).extend(pm2));
        GGraphMulti graphMulti = new GGraphMulti(gGraphTileFactory, tiles);
        BidirectionalAStar algorithm = new BidirectionalAStar(graphMulti, new ShortestDistance());

        ApproachModel sam = gGraphTileFactory.calcApproach(pm1, 10);
        ApproachModel tam = gGraphTileFactory.calcApproach(pm2, 10);
        gGraphTileFactory.connectApproach2Graph(graphMulti, sam);
        gGraphTileFactory.connectApproach2Graph(graphMulti, tam);

        System.out.println("source node=" + pm1);
        System.out.println("source approach: " + sam);
        System.out.println("target node=" + pm2);
        System.out.println("target approach: " + tam);

        MultiPointModel mpm = algorithm.performAlgo(sam, tam, Double.MAX_VALUE, new AtomicInteger(), new ArrayList<>());
        System.out.println("mpm.size=" + mpm.size());
        for (int i = 0; i < mpm.size(); i++) {
            System.out.println("  (" + i + ")=" + mpm.get(i));
        }


    }


    @Test
    public void _01_test2() {
        PointModelUtil.init(32);
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);
        mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());


        RoutingContext interactiveRoutingContext = new RoutingContext(
                10000,
                false, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                10); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath() + " " + mapFile.exists());

        PointModelImpl pm1 = new PointModelImpl(49.402665, 8.686337);
//        PointModelImpl pm2 = new PointModelImpl(49.401867, 8.687130);
        PointModelImpl pm2 = new PointModelImpl(49.401967, 8.687125);
        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);

        BGraphTileFactory bGraphTileFactory = new BGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(""), new Pref<>(false));

        ArrayList<BGraphTile> tiles = bGraphTileFactory.getBGraphTileList(new BBox().extend(pm1).extend(pm2));
        BGraphMulti graphMulti = new BGraphMulti(bGraphTileFactory, tiles);
        mg.mgmap.generic.graph.implbb.BidirectionalAStar algorithm = new mg.mgmap.generic.graph.implbb.BidirectionalAStar(graphMulti, new ShortestDistance());

        ApproachModel sam = bGraphTileFactory.calcApproach(pm1, 10);
        ApproachModel tam = bGraphTileFactory.calcApproach(pm2, 10);


        System.out.println("source node=" + pm1);
        System.out.println("source approach node=" + sam.getApproachNode());
        System.out.println("target node=" + pm2);
        System.out.println("target approach node=" + tam.getApproachNode());

        MultiPointModel mpm = algorithm.perform(sam, tam, Double.MAX_VALUE, new AtomicInteger(), new ArrayList<>());
        System.out.println("mpm.size=" + mpm.size());
        for (int i = 0; i < mpm.size(); i++) {
            System.out.println("  (" + i + ")=" + mpm.get(i));
        }
    }


    @Test
    public void _01_test3() {
        PointModelUtil.init(32);
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);
        mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());


        RoutingContext interactiveRoutingContext = new RoutingContext(
                10000,
                false, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                10); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath() + " " + mapFile.exists());

        PointModelImpl pm1 = new PointModelImpl(49.402665, 8.686337);
//        PointModelImpl pm2 = new PointModelImpl(49.401867, 8.687130);
//        PointModelImpl pm2 = new PointModelImpl(49.403416, 8.685677);
//        PointModelImpl pm2 = new PointModelImpl(49.401967, 8.687125);
//        PointModelImpl pm2 = new PointModelImpl(49.404388, 8.685524);
//        PointModelImpl pm2 = new PointModelImpl(49.392902, 8.707811);
        PointModelImpl pm2 = new PointModelImpl(49.319473, 8.755065);

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);

        MultiPointModel mpmG;
        MultiPointModel mpmB;
        {
            GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(false));
            ArrayList<GGraphTile> tiles = gGraphTileFactory.getGGraphTileList(new BBox().extend(pm1).extend(pm2));
            GGraphMulti graphMulti = new GGraphMulti(gGraphTileFactory, tiles);
            BidirectionalAStar algorithm = new BidirectionalAStar(graphMulti, new MTB_K2S2());

            ApproachModel sam = gGraphTileFactory.calcApproach(pm1, 10);
            ApproachModel tam = gGraphTileFactory.calcApproach(pm2, 10);
            gGraphTileFactory.connectApproach2Graph(graphMulti, sam);
            gGraphTileFactory.connectApproach2Graph(graphMulti, tam);

            System.out.println("source node=" + pm1);
            System.out.println("source approach: " + sam);
            System.out.println("target node=" + pm2);
            System.out.println("target approach: " + tam);

            mpmG = algorithm.performAlgo(sam, tam, Double.MAX_VALUE, new AtomicInteger(), new ArrayList<>());
        }
        {
            BGraphTileFactory bGraphTileFactory = new BGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(""), new Pref<>(false));

            ArrayList<BGraphTile> tiles = bGraphTileFactory.getBGraphTileList(new BBox().extend(pm1).extend(pm2));
            BGraphMulti graphMulti = new BGraphMulti(bGraphTileFactory, tiles);
            mg.mgmap.generic.graph.implbb.BidirectionalAStar algorithm = new mg.mgmap.generic.graph.implbb.BidirectionalAStar(graphMulti, new MTB_K2S2());

            ApproachModel sam = bGraphTileFactory.calcApproach(pm1, 10);
            ApproachModel tam = bGraphTileFactory.calcApproach(pm2, 10);


            System.out.println("source node=" + pm1);
            System.out.println("source approach node=" + sam.getApproachNode());
            System.out.println("target node=" + pm2);
            System.out.println("target approach node=" + tam.getApproachNode());

            mpmB = algorithm.perform(sam, tam, Double.MAX_VALUE, new AtomicInteger(), new ArrayList<>());

        }
        System.out.println("mpmGsize=" + mpmG.size()+" mpmBsize=" + mpmB.size());
        Assert.assertEquals (mpmG.size(), mpmB.size());
        for (int i = 0; i < mpmG.size(); i++) {
            System.out.println("  (" + i + ")=" + mpmG.get(i)+"  -  "+mpmB.get(i));
            Assert.assertEquals (mpmG.get(i).toString(), mpmB.get(i).toString());
        }

    }



}