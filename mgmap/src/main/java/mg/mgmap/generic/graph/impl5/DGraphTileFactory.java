/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.generic.graph.impl5;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.Way;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mg.mgmap.activity.mgmap.MapViewerBase;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.WayAttributesManager;
import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.graph.GraphAlgorithm;
import mg.mgmap.generic.graph.GraphFactory;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.MGLog;

public class DGraphTileFactory implements GraphFactory {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());


    static int getTileIdx(int tileX, int tileY){
        return ( tileX << 16) + tileY;
    }
    static int getTileX(int tileIdx){
        return tileIdx >> 16;
    }
    static int getTileY(int tileIdx){
        return tileIdx & 0xFFFF;
    }

    private byte zoomLevel;
    private int tileSize = MapViewerBase.TILE_SIZE;
    private WayProvider wayProvider = null;
    private ElevationProvider elevationProvider = null;
    private Pref<Boolean> prefWayDetails;
    Pref<String> prefRoutingAlgorithm;
    DTileCache tileCache;
    double smoothingDistance;
    Set<DGraphTile> persistencyCandidates = null;
    DTilePersistentCache persistentCache = null;

    AlgorithmStatistics statistics = new AlgorithmStatistics();



    public DGraphTileFactory(){}

    public DGraphTileFactory onCreate(File cacheDir, WayProvider wayProvider, ElevationProvider elevationProvider, Pref<Boolean> prefWayDetails,
                                      Pref<String> prefRoutingAlgorithm, Pref<Boolean> prefSmooth4Routing, Pref<String> prefSmoothDistance) {
        return onCreate(cacheDir, wayProvider, elevationProvider, prefWayDetails, prefRoutingAlgorithm, prefSmooth4Routing, prefSmoothDistance, Config.ZOOM_LEVEL, Config.CACHE_LIMIT, tileSize);
    }

    public DGraphTileFactory onCreate(WayProvider wayProvider, ElevationProvider elevationProvider, Pref<Boolean> prefWayDetails,
                                      Pref<String> prefRoutingAlgorithm, Pref<Boolean> prefSmooth4Routing, Pref<String> prefSmoothDistance) {
        return onCreate(null, wayProvider, elevationProvider, prefWayDetails, prefRoutingAlgorithm, prefSmooth4Routing, prefSmoothDistance, Config.ZOOM_LEVEL, Config.CACHE_LIMIT, tileSize);
    }

    public DGraphTileFactory onCreate(File cacheDir, WayProvider wayProvider, ElevationProvider elevationProvider, Pref<Boolean> prefWayDetails,
                                      Pref<String> prefRoutingAlgorithm, Pref<Boolean> prefSmooth4Routing, Pref<String> prefSmoothDistance, byte zoomLevel, int cacheLimit, int tileSize){
        this.wayProvider = wayProvider;
        this.elevationProvider = elevationProvider;
        this.prefWayDetails = prefWayDetails;
        this.prefRoutingAlgorithm = prefRoutingAlgorithm;
        this.zoomLevel = zoomLevel;
        this.tileSize = tileSize;

        if (prefSmooth4Routing.getValue()){ // smoothing is switched on
            smoothingDistance = PointModelUtil.getCloseThreshold()/2d;
            try {
                smoothingDistance = Double.parseDouble(prefSmoothDistance.getValue());
            } catch (NumberFormatException e) {
                mgLog.w(e.getMessage());
            }
        } else {
            smoothingDistance = -1d;
        }
// TODO better solution for this
        DGraphTile.initStatics();

        persistentCache = new DTilePersistentCacheFS();
        persistencyCandidates = Collections.synchronizedSet(new HashSet<>());
        persistentCache.init(cacheDir, persistencyCandidates);

        tileCache = new DTileCache(persistencyCandidates, cacheLimit);
        return this;
    }

    public int getTileSize(){
        return tileSize;
    }

    public byte getZoomLevel() {
        return zoomLevel;
    }

    public void onDestroy(){
        wayProvider = null;
        tileCache = null;
    }

    public void resetCosts(){
        if (tileCache != null) {
            for (DGraphTile graph : tileCache.getAll()) {
                graph.resetCosts();
            }
        }
    }

    public void clearCache(){
        tileCache.clear();
        persistentCache.clear();
    }

    public void serviceCache(){
        try {
            tileCache.service();
            persistentCache.service();
        } catch (Exception e) {
            mgLog.e(e);
        }
    }

    ArrayList<DGraphTile> getCached(){
        return tileCache.getAll();
    }

    ArrayList<DGraphTile> getUsed(){
        ArrayList<DGraphTile> used = new ArrayList<>();
        for (DGraphTile tile : tileCache.getAll()){
            if (tile.used){
                used.add(tile);
            }
        }
        return used;
    }

    public ArrayList<? extends Graph> getGraphList(BBox bBox){
        return getDGraphTileList(bBox);
    }

    public ArrayList<DGraphTile> getDGraphTileList(BBox bBox){
        ArrayList<DGraphTile> tileList = new ArrayList<>();
        try {
            long mapSize = MercatorProjection.getMapSize(zoomLevel, tileSize);
            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.minLongitude , mapSize) , zoomLevel, tileSize);
            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.maxLongitude , mapSize) , zoomLevel, tileSize);
            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.maxLatitude , mapSize) , zoomLevel, tileSize); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.minLatitude , mapSize) , zoomLevel, tileSize);

            int totalTiles = (tileXMax-tileXMin+1) * (tileYMax-tileYMin+1);
            mgLog.d(()->"create DGraphTileList with tileXMin=" + tileXMin + " tileYMin=" + tileYMin + " and tileXMax=" + tileXMax + " tileYMax=" + tileYMax +
                        " - total tiles=" + totalTiles);
            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    DGraphTile graph = getDGraphTile(tileX,tileY);
                    tileList.add(graph);
                }
            }
            mgLog.d(()->"create DGraphTileList done - total tiles=" + totalTiles);
        } catch (Exception e) {
            mgLog.e(e);
        }
        return tileList;
    }

    @SuppressWarnings("unused")
    public AlgorithmStatistics getStatistics(){
        return statistics;
    }

    public AlgorithmStatistics renewStatistics(){
        AlgorithmStatistics oldStatistics = statistics;
        statistics = new AlgorithmStatistics();
        return oldStatistics;
    }

    public Graph getGraph(PointModel pm1, PointModel pm2){
        ArrayList<DGraphTile> dGraphTiles = getDGraphTileList (new BBox().extend(pm1).extend(PointModelUtil.getCloseThreshold()));
        dGraphTiles.addAll(getDGraphTileList(new BBox().extend(pm2).extend(PointModelUtil.getCloseThreshold())));
        return new DGraphMulti(this, dGraphTiles);
    }

    public GraphAlgorithm getAlgorithmForGraph(Graph graph, RoutingProfile routingProfile){
        BidirectionalAStar algorithm = new BidirectionalAStar(graph, routingProfile);
        algorithm.statistics = statistics;
        return algorithm;
    }

    public ApproachModel validateApproachModel(ApproachModel approachModel){
        return approachModel; // is always valid
    }

    public DApproach calcApproach(PointModel pos, int closeThreshold){
        pos = new PointModelImpl(pos);
        BBox posBBox = new BBox()
                .extend(pos)
                .extend(closeThreshold);

        DApproach bestApproach = new DApproach();
        DNode pmApproach = new DNode();

        ArrayList<DGraphTile> tiles = getDGraphTileList(posBBox);
        for (DGraphTile dGraphTile : tiles) {
            dGraphTile.bestApproach(pos, bestApproach, pmApproach);
        }
        return (bestApproach.distance > closeThreshold)?null:bestApproach;
    }



    public void connectApproach2Graph(Graph graph, ApproachModel approachModel){}

    public void checkDirectConnectApproaches(Graph graph, ApproachModel sourceApproachModel, ApproachModel targetApproachModel) {}

    @SuppressWarnings("unused")
    public void disconnectApproach2Graph(Graph graph, ApproachModel approachModel){}

    public DGraphTile getDGraphTile(int tileIdx){
        return getDGraphTile(getTileX(tileIdx), getTileY(tileIdx), true);
    }

    @Override
    public DGraphTile getGraphTile(ApproachModel approachModel) {
        if (approachModel instanceof DApproach dam){
            return getDGraphTile(dam.tileIdx>>16, dam.tileIdx &0xFFFF);
        }
        return null;
    }
    public DGraphTile getDGraphTile(int tileX, int tileY){
        return getDGraphTile(tileX, tileY, true);
    }
    public DGraphTile getDGraphTile(int tileX, int tileY, boolean load){
        DGraphTile dGraphTile = tileCache.get(tileX, tileY);
        if (load && (dGraphTile == null)){
            synchronized (this){  // prevent parallel access from routing thread and FSGraphDetails
                dGraphTile = tileCache.get(tileX, tileY); // check, if tile is meanwhile in cache
                if (dGraphTile == null) {
                    Tile tile = new Tile(tileX, tileY, zoomLevel, tileSize);
                    dGraphTile = new DGraphTile(tile, elevationProvider, statistics, smoothingDistance);
                    if (!persistentCache.get(dGraphTile)){
                        loadGGraphTile(dGraphTile);
//                        persistentCache.put(dGraphTile);
                    }
                    tileCache.put(tileX, tileY, dGraphTile);
                }
            }
        }
        return dGraphTile;
    }

    public void loadGGraphTile(DGraphTile dGraphTile){
        mgLog.d(()->"Load tileX=" + dGraphTile.tile.tileX + " tileY=" + dGraphTile.tile.tileY + " (" + tileCache.size() + ")");
        List<Way> ways = wayProvider.getWays(dGraphTile.tile);
        for (Way way : ways) {
            if (wayProvider.isWayForRouting(way)){

                WayAttributs wayAttributs = WayAttributesManager.getWayAttributs(way);
//                WayAttributs wayAttributs = new WayAttributs(way);
                dGraphTile.addLatLongs( wayAttributs, way.latLongs[0]);

                // now setup rawWays
                if (prefWayDetails.getValue()){
                    MultiPointModelImpl mpm = new MultiPointModelImpl();
                    for (LatLong latLong : way.latLongs[0] ){
                        mpm.addPoint(new PointModelImpl(latLong));
                    }
                    dGraphTile.getRawWays().add(mpm);
                }
            }
        }
        dGraphTile.setupFinished();
    }

}
