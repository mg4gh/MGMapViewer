package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S2;
import mg.mgmap.activity.mgmap.features.routing.profile.ShortestDistance;
import mg.mgmap.activity.mgmap.features.routing.profile.TrekkingBike;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImpl;
import mg.mgmap.application.util.ElevationProviderImplHelper;
import mg.mgmap.application.util.ElevationProviderImplHelper2;
import mg.mgmap.application.util.HgtProvider2;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.impl2.AStar;
import mg.mgmap.generic.graph.impl2.ApproachModelImpl;
import mg.mgmap.generic.graph.impl2.BidirectionalAStar;
import mg.mgmap.generic.graph.impl2.GGraphMulti;
import mg.mgmap.generic.graph.impl2.GGraphAlgorithm;
import mg.mgmap.generic.graph.impl2.GGraphTile;
import mg.mgmap.generic.graph.impl2.GGraphTileFactory;
import mg.mgmap.generic.graph.impl2.GNode;
import mg.mgmap.generic.graph.RoutingSummary;
import mg.mgmap.generic.graph.impl4.DGraphTileFactory;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.util.ObservableImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.gpx.GpxExporter;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RoutingTest {

    @Test
    public void _00_routing() {

        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        PointModelUtil.init(32);
        RoutingContext interactiveRoutingContext = new RoutingContext(
                10000,
                false, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                1); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        ElevationProvider elevationProvider = new ElevationProviderImplHelper2( new HgtProvider2() );
        File mapFile = new File("src/test/assets/map_local/Ruegen_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);

        PointModel pm1 = new PointModelImpl(54.420327,13.437579);
        PointModel pm2 = new PointModelImpl(54.415861,13.447284);
        PointModel pm3 = new PointModelImpl(54.418657,13.456617);
        PointModel pm4 = new PointModelImpl(54.424050,13.454930);
        PointModel pm5 = new PointModelImpl(54.421417,13.444976);
        PointModel pm6 = new PointModelImpl(54.423379,13.433771);

        WriteableTrackLog rotlG;
        {
            GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(false), new Pref<>(""), new Pref<>(true), new Pref<>("16"));
            PointModel opm1 = gGraphTileFactory.calcApproach(pm1, 32).getApproachNode();
            PointModel opm2 = gGraphTileFactory.calcApproach(pm2, 32).getApproachNode();
            PointModel opm3 = gGraphTileFactory.calcApproach(pm3, 32).getApproachNode();
            PointModel opm4 = gGraphTileFactory.calcApproach(pm4, 32).getApproachNode();
            PointModel opm5 = gGraphTileFactory.calcApproach(pm5, 32).getApproachNode();
            PointModel opm6 = gGraphTileFactory.calcApproach(pm6, 32).getApproachNode();
            RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
            routingEngine.setRoutingProfile(new ShortestDistance());
//            routingEngine.setRoutingProfile(new MTB_K2S2());
            routingEngine.refreshRequired.set(0);
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(opm1);
            mtl.addPoint(opm2);
            mtl.addPoint(opm3);
            mtl.addPoint(opm4);
            mtl.addPoint(opm5);
            mtl.addPoint(opm6);
            rotlG = routingEngine.updateRouting2(mtl, null);
            System.out.println( rotlG.getTrackStatistic().toString() );
        }
        WriteableTrackLog rotlD;
        {
            DGraphTileFactory graphTileFactory = new DGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(false), new Pref<>(""), new Pref<>(true), new Pref<>("16"));
            PointModel opm1 = graphTileFactory.calcApproach(pm1, 32).getApproachNode();
            PointModel opm2 = graphTileFactory.calcApproach(pm2, 32).getApproachNode();
            PointModel opm3 = graphTileFactory.calcApproach(pm3, 32).getApproachNode();
            PointModel opm4 = graphTileFactory.calcApproach(pm4, 32).getApproachNode();
            PointModel opm5 = graphTileFactory.calcApproach(pm5, 32).getApproachNode();
            PointModel opm6 = graphTileFactory.calcApproach(pm6, 32).getApproachNode();
            RoutingEngine routingEngine = new RoutingEngine(graphTileFactory, interactiveRoutingContext, new ObservableImpl());
            routingEngine.setRoutingProfile(new ShortestDistance());
//            routingEngine.setRoutingProfile(new MTB_K2S2());
            routingEngine.refreshRequired.set(0);
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(opm1);
            mtl.addPoint(opm2);
            mtl.addPoint(opm3);
            mtl.addPoint(opm4);
            mtl.addPoint(opm5);
            mtl.addPoint(opm6);
            rotlD = routingEngine.updateRouting2(mtl, null);
            System.out.println( rotlD.getTrackStatistic().toString() );
        }

        System.out.println( rotlG.getTrackStatistic().getDuration() );
        System.out.println( rotlD.getTrackStatistic().getDuration() );

        TrackLogSegment tlsG = rotlG.getTrackLogSegment(0);
        TrackLogSegment tlsD = rotlD.getTrackLogSegment(0);
        assert tlsD.size() == tlsG.size();
        System.out.println( tlsD.size() );
        for (int i=0; i< tlsG.size(); i++){
            System.out.println(tlsG.get(i)+" "+tlsG.get(i).getTimestamp()+" "+tlsD.get(i)+" "+tlsD.get(i).getTimestamp()+" ");
            assert tlsD.get(i).equals(tlsG.get(i));
        }
        assert rotlG.getTrackStatistic().getDuration() == rotlD.getTrackStatistic().getDuration();
    }

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
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(false), new Pref<>(""), new Pref<>(false), new Pref<>("16"));

        RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
        routingEngine.setRoutingProfile(new ShortestDistance());
        routingEngine.refreshRequired.set(0);

        {   // einfache Strecke Nahe Spyrer Hof
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);

            mtl.addPoint(gGraphTileFactory.calcApproach(new PointModelImpl(49.396434,8.708706), PointModelUtil.getCloseThreshold()).getApproachNode());
            mtl.addPoint(gGraphTileFactory.calcApproach(new PointModelImpl(49.397147,8.705246), PointModelUtil.getCloseThreshold()).getApproachNode());

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
            mtl.addPoint(gGraphTileFactory.calcApproach(new PointModelImpl(49.365779,8.794608), PointModelUtil.getCloseThreshold()).getApproachNode());
            mtl.addPoint(gGraphTileFactory.calcApproach(new PointModelImpl(49.371844,8.795191), PointModelUtil.getCloseThreshold()).getApproachNode());

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            Assert.assertEquals(680.29, rotl.getTrackStatistic().getTotalLength(), 0.01);
            Assert.assertEquals(7, rotl.getTrackStatistic().getNumPoints());
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
            Assert.assertEquals(73, rotl.getTrackStatistic().getNumPoints());
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
            Assert.assertEquals(12, rotl.getTrackStatistic().getNumPoints());
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
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(false), new Pref<>(""), new Pref<>(false), new Pref<>("16"));

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy HH:mm:ss.SSS");
        for (int x=0; x<100; x++){
            System.out.printf(Locale.ENGLISH, "%s x=%03d started%n",sdf.format(new Date()),x );

            RoutingProfile rp = new TrekkingBike();
            ArrayList<GGraphTile> gGraphTiles;
            GGraphMulti multi;
            GNode gStart;
            GNode gEnd;

            int tileXStart = 8541;
            int tileYStart = 5580;
            int tileXEnd = 8551;
            int tileYEnd = 5599;

            gStart = gGraphTileFactory.getGGraphTile(tileXStart+x/2, tileYStart+x).getGNodes().get(5);
            gEnd = gGraphTileFactory.getGGraphTile(tileXEnd+x/2, tileYEnd+x).getGNodes().get(5);
            gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(gStart).extend(PointModelUtil.getCloseThreshold()));
            gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(gEnd).extend(PointModelUtil.getCloseThreshold())));
            multi = new GGraphMulti(gGraphTileFactory, gGraphTiles);
            MultiPointModel mpm1 = performSearch(new BidirectionalAStar(multi, rp), gStart, gEnd);
            multi.finalizeUsage();
            gGraphTileFactory.serviceCache();

            gStart = gGraphTileFactory.getGGraphTile(tileXStart+x/2, tileYStart+x).getGNodes().get(5);
            gEnd = gGraphTileFactory.getGGraphTile(tileXEnd+x/2, tileYEnd+x).getGNodes().get(5);
            gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(gStart).extend(PointModelUtil.getCloseThreshold()));
            gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(gEnd).extend(PointModelUtil.getCloseThreshold())));
            multi = new GGraphMulti(gGraphTileFactory, gGraphTiles);
            MultiPointModel mpm2 = performSearch(new AStar(multi, rp), gStart, gEnd);
            multi.finalizeUsage();
            gGraphTileFactory.serviceCache();

            Assert.assertEquals(mpm1.size(), mpm2.size());
            for (int i=0; i<mpm1.size(); i++){
                Assert.assertEquals(mpm1.get(i),mpm2.get(i));
            }
        }

    }

    @Test
    public void _03_routing() {
        PointModelUtil.init(32);
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(false), new Pref<>(""), new Pref<>(false), new Pref<>("16"));

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy HH:mm:ss.SSS");
        for (int x=0; x<100; x++){
            System.out.printf(Locale.ENGLISH, "%s x=%03d started%n",sdf.format(new Date()),x );

            RoutingProfile rp = new TrekkingBike();
            ArrayList<GGraphTile> gGraphTiles;
            GGraphMulti multi;
            GNode gStart;
            GNode gEnd;
            int tileXStart = 8541;
            int tileYStart = 5580;
            int tileXEnd = 8551;
            int tileYEnd = 5599;

            gStart = gGraphTileFactory.getGGraphTile(tileXStart+x/2, tileYStart+x).getGNodes().get(5);
            gEnd = gGraphTileFactory.getGGraphTile(tileXEnd+x/2, tileYEnd+x).getGNodes().get(5);
            gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(gStart).extend(PointModelUtil.getCloseThreshold()));
            gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(gEnd).extend(PointModelUtil.getCloseThreshold())));
            multi = new GGraphMulti(gGraphTileFactory, gGraphTiles);
            MultiPointModel mpm1 = performSearch(new BidirectionalAStar(multi, rp), gStart, gEnd);
            multi.finalizeUsage();
            gGraphTileFactory.serviceCache();

            gStart = gGraphTileFactory.getGGraphTile(tileXStart+x/2, tileYStart+x).getGNodes().get(5);
            gEnd = gGraphTileFactory.getGGraphTile(tileXEnd+x/2, tileYEnd+x).getGNodes().get(5);
            gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(gStart).extend(PointModelUtil.getCloseThreshold()));
            gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(gEnd).extend(PointModelUtil.getCloseThreshold())));
            multi = new GGraphMulti(gGraphTileFactory, gGraphTiles);
            MultiPointModel mpm2 = performSearch(new AStar(multi, rp), gStart, gEnd);
            multi.finalizeUsage();
            gGraphTileFactory.serviceCache();

            Assert.assertEquals(mpm1.size(), mpm2.size());
            for (int i=0; i<mpm1.size(); i++){
                Assert.assertEquals(mpm1.get(i),mpm2.get(i));
            }
        }
    }



    private MultiPointModel performSearch(GGraphAlgorithm routingAlg, GNode gStart, GNode gEnd){
        ArrayList<PointModel> relaxed = new ArrayList<>();
        ApproachModelImpl amStart = new ApproachModelImpl(0,0,gStart,null,null, null, gStart, 0);
        ApproachModelImpl amEnd = new ApproachModelImpl(0,0,gEnd,null,null,null, gEnd, 0);
        MultiPointModel mpm = routingAlg.perform(amStart, amEnd, 100000, new AtomicInteger(),relaxed);
        System.out.println(routingAlg.getResult().replaceAll("\n","    "));
        return mpm;
    }


    @Test
    public void _04_routing() throws Exception{
        PointModelUtil.init(32);
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        RoutingContext interactiveRoutingContext = new RoutingContext(
                1000000,
                false, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                1); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(false), new Pref<>(""), new Pref<>(false), new Pref<>("16"));

        RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
        routingEngine.setRoutingProfile(new TrekkingBike());
        routingEngine.refreshRequired.set(0);

        {
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.405697,8.679110));
            mtl.addPoint(new PointModelImpl(48.817059,9.060983));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            File gpxFile = new File("src/test/assets/temp_local/test.gpx"); // !!! gpx is not uploaded to git (test result)

            GpxExporter.export(new PrintWriter(gpxFile), rotl);
            try (PrintWriter pw = new PrintWriter(gpxFile)){
                GpxExporter.export(pw, rotl);
            } catch (Exception e) {
                MGLog.se(e);
            }

        }

        routingEngine.setRoutingProfile(new MTB_K2S2());
        routingEngine.refreshRequired.set(0);

        {
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.405697,8.679110));
            mtl.addPoint(new PointModelImpl(48.817059,9.060983));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            File gpxFile = new File("src/test/assets/temp_local/test2.gpx"); // !!! gpx is not uploaded to git (test result)

            GpxExporter.export(new PrintWriter(gpxFile), rotl);
            try (PrintWriter pw = new PrintWriter(gpxFile)){
                GpxExporter.export(pw, rotl);
            } catch (Exception e) {
                MGLog.se(e);
            }

        }

    }
    @Test
    public void _05_routing() throws Exception{
        PointModelUtil.init(32);
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        RoutingContext interactiveRoutingContext = new RoutingContext(
                1000000,
                true, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                32); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(false), new Pref<>("BidirectionalAstarExt"), new Pref<>(false), new Pref<>("16"));

        RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
        routingEngine.setRoutingProfile(new TrekkingBike());
        routingEngine.refreshRequired.set(0);

        {
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.405490,8.679168));
            mtl.addPoint(new PointModelImpl(49.398707,8.694131));
            mtl.stopSegment(10L);
            mtl.startSegment(20L);
            mtl.addPoint(new PointModelImpl(49.404678,8.698063));
            mtl.addPoint(new PointModelImpl(49.398282,8.704087));
            mtl.stopSegment(30L);
            mtl.stopTrack(41L);

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            File gpxFile = new File("src/test/assets/temp_local/test.gpx"); // !!! gpx is not uploaded to git (test result)

            GpxExporter.export(new PrintWriter(gpxFile), rotl);
            try (PrintWriter pw = new PrintWriter(gpxFile)){
                GpxExporter.export(pw, rotl);
            } catch (Exception e) {
                MGLog.se(e);
            }

            for (RoutingSummary routingSummary : RoutingSummary.routingSummaries){
                System.out.println( routingSummary);
            }
        }

    }

    /** compare bilinear interpolation with bicubic and with bicubic spline interpolation */
    @Test
    public void _10_routing(){
        PointModelUtil.init(32);
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.WARN);
        MGLog.setUnittest(true);

        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());
        assert(mapFile.exists());

        RoutingContext interactiveRoutingContext = new RoutingContext(
                1000000,
                false, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                1); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        Pref<Boolean> useQubicInterpolation = new Pref<>(true);
        Pref<Boolean> useQubicSplineInterpolation = new Pref<>(true);
        ElevationProvider elevationProvider = new ElevationProviderImpl( new HgtProvider2(), useQubicInterpolation, useQubicSplineInterpolation );
        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);

        WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
        mtl.startTrack(1L);
        mtl.startSegment(2L);
        mtl.addPoint(new PointModelImpl(49.407757, 8.679018));
//        mtl.addPoint(new PointModelImpl(49.136299, 8.914161));
        mtl.addPoint(new PointModelImpl(48.948888, 8.960108));

        Pref<String> prefSmoothingDistance = new Pref<>("32");

        // need multiple rounds, maybe related to caching effects (at the beginning each round is faster then the previous)
        for (int i=0; i<10; i++)
        {
            useQubicInterpolation.setValue(false);
            useQubicSplineInterpolation.setValue(false);
            GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(false), new Pref<>(""), new Pref<>(false), prefSmoothingDistance);
            RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
            routingEngine.setRoutingProfile(new TrekkingBike());
            routingEngine.refreshRequired.set(0);

            long now1 = System.currentTimeMillis();
            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            long now2 = System.currentTimeMillis();
            String statistic = rotl.getTrackStatistic().toString();
//            System.out.println("******************************************************** "+useQubicInterpolation.getValue()+","+useQubicSplineInterpolation.getValue()+" *********************************************************");
            System.out.println("round"+ i +" duration="+(now2-now1)+"ms  "+statistic);
//            System.out.println("********************************************************************************************************************************");
        }
        {
            useQubicInterpolation.setValue(false);
            useQubicSplineInterpolation.setValue(false);
            GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(false), new Pref<>(""), new Pref<>(false), prefSmoothingDistance);
            RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
            routingEngine.setRoutingProfile(new TrekkingBike());
            routingEngine.refreshRequired.set(0);

            long now1 = System.currentTimeMillis();
            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            long now2 = System.currentTimeMillis();
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println("******************************************************** "+useQubicInterpolation.getValue()+","+useQubicSplineInterpolation.getValue()+" *********************************************************");
            System.out.println("duration="+(now2-now1)+"ms  "+statistic);
            System.out.println("********************************************************************************************************************************");
            System.out.println();
        }
        {
            useQubicInterpolation.setValue(true);
            useQubicSplineInterpolation.setValue(false);
            GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(false), new Pref<>(""), new Pref<>(false), prefSmoothingDistance);
            RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
            routingEngine.setRoutingProfile(new TrekkingBike());
            routingEngine.refreshRequired.set(0);

            long now1 = System.currentTimeMillis();
            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            long now2 = System.currentTimeMillis();
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println("******************************************************** "+useQubicInterpolation.getValue()+","+useQubicSplineInterpolation.getValue()+" *********************************************************");
            System.out.println("duration="+(now2-now1)+"ms  "+statistic);
            System.out.println("********************************************************************************************************************************");
            System.out.println();
        }
        for (int i=0; i<10; i++)
        {
            useQubicInterpolation.setValue(true);
            useQubicSplineInterpolation.setValue(true);
            GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, new Pref<>(false), new Pref<>(""), new Pref<>(false), prefSmoothingDistance);
            RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
            routingEngine.setRoutingProfile(new TrekkingBike());
            routingEngine.refreshRequired.set(0);

            long now1 = System.currentTimeMillis();
            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            long now2 = System.currentTimeMillis();
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println("******************************************************** "+useQubicInterpolation.getValue()+","+useQubicSplineInterpolation.getValue()+" *********************************************************");
            System.out.println("duration="+(now2-now1)+"ms  "+statistic);
            System.out.println("********************************************************************************************************************************");
            System.out.println();
        }
    }




}
