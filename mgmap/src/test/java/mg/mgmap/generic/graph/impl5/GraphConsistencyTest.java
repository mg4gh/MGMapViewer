package mg.mgmap.generic.graph.impl5;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.MapViewerBase;
import mg.mgmap.activity.mgmap.MultiMapDataStore;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.activity.mgmap.features.routing.profile.TrekkingBike;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper2;
import mg.mgmap.application.util.HgtProvider2;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.GraphAlgorithm;
import mg.mgmap.generic.graph.WayAttributesManager;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.ObservableImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.MGLog;

@FixMethodOrder(MethodSorters.DEFAULT)
public class GraphConsistencyTest {
    RoutingProfile rp = new TrekkingBike();


    @Test
    public void test01_1() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.WARN);
        MGLog.setUnittest(true);

        ElevationProviderImplHelper2 elevationProvider = new ElevationProviderImplHelper2( new HgtProvider2() );
        elevationProvider.getPrefBicubicInterpolation().setValue(true);
        elevationProvider.getPrefBicubicSplineInterpolation().setValue(true);
        File mapFile = new File("src/test/assets/map_local/Germany-South_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MultiMapDataStore mmds = new MultiMapDataStore(MultiMapDataStore.DataPolicy.FAST_DETAILS);
//        MultiMapDataStore mmds = new MultiMapDataStore(MultiMapDataStore.DataPolicy.RETURN_ALL);
        MapDataStore mds = new MapFile(mapFile, "de");
        mmds.addMapDataStore(mds, false, false);
        WayProvider wayProvider = new WayProviderHelper(mmds);

        Pref<Boolean> prefWayDetails = new Pref<>(false);
        Pref<String> prefRoutingAlgorithm = new Pref<>("BidirectionalAstarExt");
        Pref<Boolean> prefSmooth4Routing = new Pref<>(true);
        Pref<String> prefSmoothDistance = new Pref<>("32");
        DGraphTileFactory graphTileFactory = new DGraphTileFactory().onCreate(wayProvider,elevationProvider,prefWayDetails,
                prefRoutingAlgorithm, prefSmooth4Routing, prefSmoothDistance, Config.ZOOM_LEVEL, 1000, MapViewerBase.TILE_SIZE);


        ArrayList<DGraphTile> tiles = new ArrayList<>();
        PointModel pm1 = new PointModelImpl(49.405761,8.679108); // Heidelberg
        PointModel pm2 = new PointModelImpl(49.792993,9.925837); // Würzburg


//        PointModel pm1 = new PointModelImpl(49.405618,8.679108); // Heidelberg
//        PointModel pm1 = new PointModelImpl(49.405673,8.679079); // Heidelberg
//        PointModel pm1 = new PointModelImpl(49.406391, 8.679864); // Heidelberg
//        PointModel pm2 = new PointModelImpl(49.792982, 9.925589); // Würzburg
//        PointModel pm2 = new PointModelImpl(49.792999,9.925915); // Würzburg
//        PointModel pm2 = new PointModelImpl(49.921241, 10.178596); // Wipfeld (Main)
//        PointModel pm2 = new PointModelImpl(50.043004, 10.237682); // Schweifurt
//        PointModel pm2 = new PointModelImpl(50.355218, 10.687741); // Streufdorf

        double distance;
        {
            DApproach dpm1 = graphTileFactory.calcApproach(pm1, PointModelUtil.getCloseThreshold());
            assert (dpm1 != null);
            assert (dpm1.getApproachNode() != null);
            tiles.add(graphTileFactory.getGraphTile(dpm1));

            DApproach dpm2 = graphTileFactory.calcApproach(pm2, PointModelUtil.getCloseThreshold());
            assert (dpm2 != null);
            assert (dpm2.getApproachNode() != null);
            tiles.add(graphTileFactory.getGraphTile(dpm2));

            System.out.println("tiles="+tiles);
            System.out.println("Cache="+graphTileFactory.getCached());
            DGraphMulti graphMulti = new DGraphMulti(graphTileFactory, tiles);

            GraphAlgorithm algorithm = graphTileFactory.getAlgorithmForGraph(graphMulti, rp);
            algorithm.setRouteIntermediatesObservable(new ObservableImpl());
            MultiPointModel mpm1 = algorithm.perform(dpm1, dpm2, Double.MAX_VALUE, new AtomicInteger(), new ArrayList<>());
            System.out.println(algorithm.getResult());
            graphMulti.finalizeUsage();
            graphTileFactory.serviceCache();

            distance = PointModelUtil.distance(mpm1);
            System.out.printf(Locale.ENGLISH, "distance: %.1f%n", distance);
            AlgorithmStatistics statistics = graphTileFactory.renewStatistics();
            System.out.printf(Locale.ENGLISH, "%s\n", statistics);
            System.out.printf(Locale.ENGLISH, "waMap.size=%d\n", WayAttributesManager.waMap.size());

            if (algorithm instanceof BidirectionalAStar bas){
                assert (bas.rc == 0);
            }
        }
    }


    // repeat test with small cache size
    @Test
    public void test03_3() {
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
                prefRoutingAlgorithm, prefSmooth4Routing, prefSmoothDistance, Config.ZOOM_LEVEL, 2, MapViewerBase.TILE_SIZE);

        ArrayList<DGraphTile> tiles = new ArrayList<>();
        PointModel pm1 = new PointModelImpl(49.428772, 8.782577);
        PointModel pm2 = new PointModelImpl(49.428450, 8.782398);

        double distance;
        {
            DApproach dpm1 = graphTileFactory.calcApproach(pm1, PointModelUtil.getCloseThreshold());
            assert (dpm1 != null);
            assert (dpm1.getApproachNode() != null);
            tiles.add(graphTileFactory.getGraphTile(dpm1));

            DApproach dpm2 = graphTileFactory.calcApproach(pm2, PointModelUtil.getCloseThreshold());
            assert (dpm2 != null);
            assert (dpm2.getApproachNode() != null);
            tiles.add(graphTileFactory.getGraphTile(dpm2));

            System.out.println("tiles="+tiles);
            System.out.println("Cache="+graphTileFactory.getCached());
            DGraphMulti graphMulti = new DGraphMulti(graphTileFactory, tiles);

            GraphAlgorithm algorithm = graphTileFactory.getAlgorithmForGraph(graphMulti, rp);
            MultiPointModel mpm1 = algorithm.perform(dpm1, dpm2, Double.MAX_VALUE, new AtomicInteger(), new ArrayList<>());
            System.out.println(algorithm.getResult());
            graphMulti.finalizeUsage();
            graphTileFactory.serviceCache();

            System.out.println("ResultPath:");
            for (int i=0; i<mpm1.size(); i++){
                System.out.println(" i="+i+"  "+mpm1.get(i));
            }
            distance = PointModelUtil.distance(mpm1);
            System.out.printf(Locale.ENGLISH, "distance: %.1f%n", distance);
            AlgorithmStatistics statistics = graphTileFactory.renewStatistics();
            System.out.printf(Locale.ENGLISH, "%s\n", statistics);
        }
        System.out.printf(Locale.ENGLISH, "************************************* end 1  *****************************************************\n");
        tiles.clear();
        System.gc();

    }




}
