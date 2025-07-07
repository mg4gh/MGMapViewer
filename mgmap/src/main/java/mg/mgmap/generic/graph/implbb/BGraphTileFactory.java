package mg.mgmap.generic.graph.implbb;

import static mg.mgmap.generic.graph.implbb.BNodes.FLAG_INVALID;

import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.activity.mgmap.MapViewerBase;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.graph.GraphAlgorithm;
import mg.mgmap.generic.graph.GraphFactory;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

public class BGraphTileFactory implements GraphFactory {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());


    private final byte ZOOM_LEVEL = 15;
    final static int CACHE_LIMIT = 2000;
    private final int TILE_SIZE = MapViewerBase.TILE_SIZE;

    static int getKey(int tileX,int tileY){
        return ( tileX <<16) + tileY;
    }

    private WayProvider wayProvider = null;
    private ElevationProvider elevationProvider = null;
    Pref<String> prefRoutingAlgorithm;
    Pref<Boolean> prefSmooth4Routing;
    BTileCache bTileCache;

    public BGraphTileFactory(){}

    public BGraphTileFactory onCreate(WayProvider wayProvider, ElevationProvider elevationProvider, Pref<String> prefRoutingAlgorithm, Pref<Boolean> prefSmooth4Routing){
        this.wayProvider = wayProvider;
        this.elevationProvider = elevationProvider;
        this.prefRoutingAlgorithm = prefRoutingAlgorithm;
        this.prefSmooth4Routing = prefSmooth4Routing;

        bTileCache = new BTileCache(CACHE_LIMIT);
        return this;
    }


    public ArrayList<? extends Graph> getGraphList(BBox bBox){
        return getBGraphTileList(bBox);
    }

    @Override
    public Graph getGraph(PointModel pm1, PointModel pm2) {
        return null;
    }

    @Override
    public ApproachModel calcApproach(PointModel pointModel, int closeThreshold) {
        BBox mtlpBBox = new BBox()
                .extend(pointModel)
                .extend(closeThreshold);

        ApproachModel bestApproach = null;
        WriteablePointModel pmApproach = new TrackLogPoint();

        ArrayList<BGraphTile> tiles = getBGraphTileList(mtlpBBox);
        for (BGraphTile bGraphTile : tiles){
            for (short node=0; node < bGraphTile.nodes.nodesUsed; node++) {
                if (!bGraphTile.nodes.isFlag(node, FLAG_INVALID)) { // valid point
                    double lat = LaLo.md2d(bGraphTile.nodes.getLatitude(node));
                    double lon = LaLo.md2d(bGraphTile.nodes.getLongitude(node));
                    float ele = bGraphTile.nodes.getEle(node);
                    short neighbour = bGraphTile.nodes.getNeighbour(node);
                    while ((neighbour = bGraphTile.neighbours.getNextNeighbour(neighbour)) != 0) {
                        short neighbourNode = bGraphTile.neighbours.getNeighbourPoint(neighbour);
                        if (bGraphTile.neighbours.getTileSelector(neighbour) == 0){ // neighbour node is in same tile
                            double neighbourLat = LaLo.md2d(bGraphTile.nodes.getLatitude(neighbourNode));
                            double neighbourLon = LaLo.md2d(bGraphTile.nodes.getLongitude(neighbourNode));
                            if (PointModelUtil.compareTo(lat, lon, neighbourLat, neighbourLon) < 0){ // neighbour relations exist in both direction - here we can reduce to one
                                BBox bBoxPart = new BBox().extend(lat, lon).extend(neighbourLat, neighbourLon);
                                boolean bIntersects = mtlpBBox.intersects(bBoxPart);
                                if (bIntersects) { // ok, is candidate for close
                                    float neighbourEle = bGraphTile.nodes.getEle(neighbourNode);
                                    if (PointModelUtil.findApproach(pointModel, lat, lon, ele, 0, neighbourLat, neighbourLon, neighbourEle, 0, pmApproach , closeThreshold)) {
                                        double distance = PointModelUtil.distance(pointModel, pmApproach)+0.0001;
                                        if (distance < closeThreshold){ // ok, is close ==> new Approach found
                                            if ((bestApproach == null) || (distance < bestApproach.getApproachDistance())){ // ... and it is better than current best approach - so keep it as the best
                                                BNode bNode = new BNode(bGraphTile, node);
                                                BNode bNeighbourNode = new BNode(bGraphTile, neighbourNode);
                                                PointModelImpl approachNode = new PointModelImpl(pmApproach.getLat(), pmApproach.getLon(), pmApproach.getEle(), 0); // so we get a new node for the approach, since pmApproach will be overwritten in next cycle
                                                bestApproach = new ApproachModelImpl(pointModel, approachNode, bNode, bNeighbourNode, (float)distance);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
        return bestApproach;

    }

    @Override
    public ApproachModel validateApproachModel(ApproachModel approachModel) {
        return approachModel;
    }

    @Override
    public GraphAlgorithm getAlgorithmForGraph(Graph graph, RoutingProfile routingProfile) {
        return null;
    }

    @Override
    public void connectApproach2Graph(Graph graph, ApproachModel approachModel) {

    }

    @Override
    public void disconnectApproach2Graph(Graph graph, ApproachModel approachModel) {

    }

    @Override
    public void clearCache() {

    }

    @Override
    public void resetCosts() {

    }

    @Override
    public void serviceCache() {

    }

    @Override
    public void onDestroy() {

    }

    ArrayList<BGraphTile> getCached(){
        return bTileCache.getAll();
    }


    public ArrayList<BGraphTile> getBGraphTileList(BBox bBox){
        ArrayList<BGraphTile> tileList = new ArrayList<>();
        try {
            long mapSize = MercatorProjection.getMapSize(ZOOM_LEVEL, TILE_SIZE);
            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.minLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.maxLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.maxLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.minLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);

            int totalTiles = (tileXMax-tileXMin+1) * (tileYMax-tileYMin+1);
            mgLog.d(()->"create BGraphTileList with tileXMin=" + tileXMin + " tileYMin=" + tileYMin + " and tileXMax=" + tileXMax + " tileYMax=" + tileYMax +
                    " - total tiles=" + totalTiles);
            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    BGraphTile graph = getBGraphTile(tileX,tileY);
                    tileList.add(graph);
                }
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
        return tileList;
    }


    public BGraphTile getBGraphTile(int tileX, int tileY){
        return getBGraphTile(tileX, tileY, true);
    }
    public BGraphTile getBGraphTile(int tileX, int tileY, boolean load){
        BGraphTile bGraphTile = bTileCache.get(tileX, tileY);
        if (load && (bGraphTile == null)){
            synchronized (this){  // prevent parallel access from routing thread and FSGraphDetails
                bGraphTile = loadBGraphTile(tileX, tileY);
                if (prefSmooth4Routing.getValue()){
                    bGraphTile.smoothGraph();
                }
                bTileCache.put(tileX, tileY, bGraphTile);
            }
        }
        return bGraphTile;
    }


    private BGraphTile loadBGraphTile(int tileX, int tileY){
        Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
        mgLog.d(()->"Load "+(bTileCache.size())+" tileX=" + tileX + " tileY=" + tileY + " "+tile.getBoundingBox().getCenterPoint());
        BGraphTile bGraphTile = new BGraphTile(wayProvider, elevationProvider, tile);
        bGraphTile.loadGGraphTile();
        return bGraphTile;
    }



}
