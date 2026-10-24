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
package mg.mgmap.generic.graph.impl3;

import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.Way;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.activity.mgmap.MapViewerBase;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.graph.GraphAlgorithm;
import mg.mgmap.generic.graph.GraphFactory;
import mg.mgmap.generic.graph.WayAttributs;

import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.LaLo;
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

    private int cacheLimit;
    private byte zoomLevel;
    private int tileSize = MapViewerBase.TILE_SIZE;
    private WayProvider wayProvider = null;
    private ElevationProvider elevationProvider = null;
    private Pref<Boolean> prefWayDetails;
    Pref<String> prefRoutingAlgorithm;
    Pref<Boolean> prefSmooth4Routing;
    DTileCache tileCache;
    Pref<String> prefSmoothDistance;

    public DGraphTileFactory(){}

    public DGraphTileFactory onCreate(WayProvider wayProvider, ElevationProvider elevationProvider, Pref<Boolean> prefWayDetails,
                                      Pref<String> prefRoutingAlgorithm, Pref<Boolean> prefSmooth4Routing, Pref<String> prefSmoothDistance) {
        return onCreate(wayProvider, elevationProvider, prefWayDetails, prefRoutingAlgorithm, prefSmooth4Routing, prefSmoothDistance, Config.ZOOM_LEVEL, Config.CACHE_LIMIT, tileSize);
    }

    public DGraphTileFactory onCreate(WayProvider wayProvider, ElevationProvider elevationProvider, Pref<Boolean> prefWayDetails,
                                      Pref<String> prefRoutingAlgorithm, Pref<Boolean> prefSmooth4Routing, Pref<String> prefSmoothDistance, byte zoomLevel, int cacheLimit, int tileSize){
        this.wayProvider = wayProvider;
        this.elevationProvider = elevationProvider;
        this.prefWayDetails = prefWayDetails;
        this.prefRoutingAlgorithm = prefRoutingAlgorithm;
        this.prefSmooth4Routing = prefSmooth4Routing;
        this.prefSmoothDistance = prefSmoothDistance;
        this.zoomLevel = zoomLevel;
        this.cacheLimit = cacheLimit;
        this.tileSize = tileSize;

        tileCache = new DTileCache(cacheLimit);
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
    }

    public void serviceCache(){
        try {
            tileCache.service();
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
            if (totalTiles < cacheLimit){
                for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                    for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                        DGraphTile graph = getDGraphTile(tileX,tileY);
                        tileList.add(graph);
                    }
                }
            } else {
                mgLog.e("totalTiles exceeds CACHE_LIMIT: totalTiles="+ totalTiles+" CACHE_LIMIT="+ cacheLimit);
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
        return tileList;
    }

    public Graph getGraph(PointModel pm1, PointModel pm2){
        ArrayList<DGraphTile> dGraphTiles = getDGraphTileList (new BBox().extend(pm1).extend(PointModelUtil.getCloseThreshold()));
        dGraphTiles.addAll(getDGraphTileList(new BBox().extend(pm2).extend(PointModelUtil.getCloseThreshold())));
        return new DGraphMulti(this, dGraphTiles);
    }

    public GraphAlgorithm getAlgorithmForGraph(Graph graph, RoutingProfile routingProfile){
        return new BidirectionalAStar(graph, routingProfile);
    }

    public ApproachModel validateApproachModel(ApproachModel approachModel){
        return approachModel; // is always valid
    }

    public ApproachModel calcApproach(PointModel pos, int closeThreshold){
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



    public void connectApproach2Graph(Graph graph, ApproachModel approachModel){
        if (approachModel instanceof DApproach dam){
            DGraphTile dGraphTile = getDGraphTile(dam.tileIdx>>16, dam.tileIdx &0xFFFF);
            assert (dam.tileIdx == dGraphTile.tileIdx);
            dGraphTile.connectApproach2Graph(dam);
        }
    }

    public void checkDirectConnectApproaches(Graph graph, ApproachModel sourceApproachModel, ApproachModel targetApproachModel) {
        if ((sourceApproachModel instanceof DApproach sdam) && (targetApproachModel instanceof DApproach tdam)){
            if (sdam.tileIdx == tdam.tileIdx){
                DGraphTile dGraphTile = getDGraphTile(sdam.tileIdx>>16, sdam.tileIdx &0xFFFF);
                dGraphTile.checkDirectConnectApproaches(sdam, tdam);
            }
        }
    }

    @SuppressWarnings("unused")
    public void disconnectApproach2Graph(Graph graph, ApproachModel approachModel){
        if ((graph instanceof DGraphMulti dGraphMulti) && (approachModel instanceof DApproach dam)){
            DGraphTile dGraphTile = getDGraphTile(dam.tileIdx>>16, dam.tileIdx &0xFFFF);
            assert (dam.tileIdx == dGraphTile.tileIdx);
            dGraphTile.disconnectApproach2Graph(dam);
        }
    }

    public DGraphTile getDGraphTile(int tileIdx){
        return getDGraphTile(getTileX(tileIdx), getTileY(tileIdx), true);
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
                    dGraphTile = loadGGraphTile(tileX, tileY);
                    if (prefSmooth4Routing.getValue()){
                        smoothGGraphTile(dGraphTile);
                        reduceIntermediates(dGraphTile);
                    }
                    tileCache.put(tileX, tileY, dGraphTile);
                }
            }
        }
        return dGraphTile;
    }

    @SuppressWarnings("CommentedOutCode")
    public DGraphTile loadGGraphTile(int tileX, int tileY){
        mgLog.d(()->"Load tileX=" + tileX + " tileY=" + tileY + " (" + tileCache.size() + ")");
        Tile tile = new Tile(tileX, tileY, zoomLevel, tileSize);
        DGraphTile dGraphTile = new DGraphTile(elevationProvider, tile);
        for (Way way : wayProvider.getWays(tile)) {
            if (wayProvider.isWayForRouting(way)){

                WayAttributs wayAttributs = new WayAttributs(way);
                dGraphTile.addLatLongs( wayAttributs, way.latLongs[0]);

                // now setup rawWays
                if (prefWayDetails.getValue()){
                    // TODO add rawWays somehow
                }
            }
        }
        dGraphTile.setupFinished();

        int latThreshold = LaLo.d2md( PointModelUtil.latitudeDistance(Config.CONNECT_THRESHOLD_METER) );
        int lonThreshold = LaLo.d2md( PointModelUtil.longitudeDistance(Config.CONNECT_THRESHOLD_METER, tile.getBoundingBox().getCenterPoint().getLatitude()) );
//            Log.v(MGMapApplication.LABEL, NameUtil.context()+" latThreshold="+latThreshold+" lonThreshold="+lonThreshold);

        //all highways are in the map ... try to correct data ..
// TODO implement data correction
/*
        ArrayList<GNode> nodes = dGraphTile.getGNodes();
        for (int iIdx=0; iIdx<nodes.size(); iIdx++){
            GNode iNode = nodes.get(iIdx);
            if (iNode.isFlag(GNode.FLAG_INVALID)) continue; // invalid node
            int iNeighbours = dGraphTile.countNeighbours(iNode);
            for (int nIdx=iIdx+1; nIdx<nodes.size(); nIdx++ ) {
                GNode nNode = nodes.get(nIdx);
                if (nNode.isFlag(GNode.FLAG_INVALID)) continue; // invalid node
                if (iNode.laMdDiff(nNode) >= latThreshold) break; // go to next iIdx
                if (iNode.loMdDiff(nNode) >= lonThreshold)
                    continue; // goto next mIdx
                if (PointModelUtil.distance(iNode, nNode) > GGraph.CONNECT_THRESHOLD_METER)
                    continue;
                if (!neighbourPoints.isEmpty()) neighbourPoints.clear(); // reuse list object, but clear if necessary
                if (dGraphTile.getNeighbours(iNode, neighbourPoints).contains(nNode)){
                    continue;
                }

//This doesn't work well for routing hints
//                    graph.addSegment(iNode, nNode);

//And this didn't work too - removes the resulting point from tile clip process
//                  // Try to simplify the graph by removing node nNode
                // iterate over al neighbours from nNode
//                    GNeighbour nextNeighbour = nNode.getNeighbour();
//                    while (nextNeighbour.getNextNeighbour() != null) {
//                        nextNeighbour = nextNeighbour.getNextNeighbour();
//                        // remove nNode as a Neighbour
//                        nextNeighbour.getNeighbourNode().removeNeighbourNode(nNode);
//                        graph.addSegment(iNode, nextNeighbour.getNeighbourNode());
//                    }

//And this is still not good: (Hollmuth,Heiligkreuzsteinach) there are 2 neighbours at one end ... and one on the other ... and this doesn't work
//                    if (nNode.countNeighbours() != 1) continue; // connect only end points
//                    // Third solution approach: connect only point with exactly 1 neighbour
//                    // Therefore this shouldn't be a Problem for routing hints, since both connected points have now 2 neighbours - so they are no routing points
//                    graph.addSegment(iNode, nNode);

                int nNeighbours = dGraphTile.countNeighbours(nNode);
                if ((iNeighbours == 0) || (nNeighbours == 0)) { // don't connect, if a node has no neighbours (might occur due to former reduceGraph action)
                    continue;
                }
                if ((iNeighbours == 1) && (nNeighbours == 1)) { // 1:1 connect -> no routing hint problem
                    dGraphTile.addSegment(null,iNode, nNode);
                    continue;
                }
                if (isBorderPoint(dGraphTile.bBox, nNode) || isBorderPoint(dGraphTile.bBox, iNode)) { // border points must be kept for MultiTiles; accept potential routing hint problem
                    dGraphTile.addSegment(null,iNode, nNode);
                    continue;
                }
                if ((iNeighbours == 2) && (nNeighbours == 1)) { // 2:1 connect -> might give routing hint problem
                    reduceGraph(dGraphTile, iNode, nNode);  // drop nNode; move neighbour form nNode to iNode
                    continue;
                }
                if ((iNeighbours == 1) && (nNeighbours == 2)) { // 1:2 connect -> might give routing hint problem
                    reduceGraph(dGraphTile, nNode, iNode); // drop iNode; move neighbour form iNode to nNode
                    iNeighbours = 0; // just in case there is a second close nNode
                    continue;
                }
                // else (n:m) accept routing hint issue
                dGraphTile.addSegment(null,iNode, nNode);

            }
        }
        dGraphTile.getGNodes().removeIf(node -> node.isFlag(GNode.FLAG_INVALID));
*/
        return dGraphTile;
    }


    // reduce Graph by dropping nNode, all Neighbours form nNode will get iNode as a Neighbour
//    private void reduceGraph(GGraphTile graph, GNode iNode, GNode nNode){
//        // iterate over al neighbours from nNode
//        GNeighbour nextNeighbour = null;
//        while (graph.getNextNeighbour(nNode, nextNeighbour) != null) {
//            nextNeighbour = graph.getNextNeighbour(nNode, nextNeighbour);
//            // remove nNode as a Neighbour
//            graph.removeNeighbourTo(nextNeighbour.getNeighbourNode(), nNode);
//            graph.addSegment(nextNeighbour.getWayAttributs(),iNode, nextNeighbour.getNeighbourNode());
//        }
//        nNode.setFlag(GNode.FLAG_INVALID, true);
//        nNode.getNeighbour().setNextNeighbour(null);
//    }

    private void smoothGGraphTile(DGraphTile tile){
//        double smoothingDistance = PointModelUtil.getCloseThreshold()/2d;
//        try {
//            smoothingDistance = Double.parseDouble(prefSmoothDistance.getValue());
//        } catch (NumberFormatException e) {
//            mgLog.w(e.getMessage());
//        }
//        mgLog.d(String.format(Locale.ENGLISH, "Smoothing distance: %.1fm",smoothingDistance));
//        ArrayList<GNeighbour> smoothNeighbourList = new ArrayList<>();
//        for (GNode aNode : tile.getGNodes()){
//            boolean fix = true;
//            GNeighbour firstNeighbour = tile.getNextNeighbour(aNode, null);
//            if (firstNeighbour != null){
//                GNeighbour secondNeighbour = tile.getNextNeighbour(aNode, firstNeighbour);
//                if (secondNeighbour != null){
//                    if ((tile.getNextNeighbour(aNode, secondNeighbour) == null) &&
//                            (firstNeighbour.getWayAttributs() == secondNeighbour.getWayAttributs()) &&
//                            (aNode.borderNode == 0)){
//                        fix = false;
//                    }
//                }
//            }
//            aNode.setFlag(GNode.FLAG_FIX, fix);
//            aNode.setFlag(GNode.FLAG_VISITED, false);
//            aNode.setFlag(GNode.FLAG_HEIGHT_RELEVANT, fix);
//        }
//        for (GNode aNode : tile.getGNodes()) { // iterate over all nodes
//            if (aNode.isFlag(GNode.FLAG_FIX)){
//                GNeighbour aNeighbour = null;
//                while ((aNeighbour = tile.getNextNeighbour(aNode, aNeighbour)) != null) { // iterate over all neighbours
//
//                    GNeighbour neighbour = aNeighbour;
//                    GNode neighbourNode = neighbour.getNeighbourNode();
//                    if (neighbourNode.isFlag(GNode.FLAG_FIX) || neighbourNode.isFlag(GNode.FLAG_VISITED)){
//                        continue; // this path is already handled
//                    }
//                    int neighbourNodeIdx;
//                    // reset smoothNodeList
//                    smoothNeighbourList.clear();
//                    smoothNeighbourList.add(aNode.getNeighbour().getReverse()); // a neighbour with getNeighbourNode = aNode
//                    float minHeight = aNode.getEle();
//                    float maxHeight = aNode.getEle();
//                    GNode lastHeightRelevantPoint = aNode;
//                    int lastHeightRelevantPointIdx = 0;
//
//                    while (true){
//                        neighbourNode.setFlag(GNode.FLAG_VISITED, true);
//                        neighbourNodeIdx = smoothNeighbourList.size();
//                        smoothNeighbourList.add(neighbour);
//                        if (neighbourNode.getEle() < minHeight){
//                            minHeight = neighbourNode.getEle();
//                        }
//                        if (neighbourNode.getEle() > maxHeight){
//                            maxHeight = neighbourNode.getEle();
//                        }
//
//
//                        double distance2LastHeightRelevant = distance(smoothNeighbourList, lastHeightRelevantPointIdx, neighbourNodeIdx);
//                        if ((distance2LastHeightRelevant > smoothingDistance*5) || ((Math.abs(neighbourNode.getEle()-lastHeightRelevantPoint.getEle()) >= TrackLogStatistic.ELE_THRESHOLD_ELSE)   && (distance2LastHeightRelevant > smoothingDistance)  && !neighbourNode.isFlag(GNode.FLAG_FIX))){
//                            neighbourNode.setFlag(GNode.FLAG_HEIGHT_RELEVANT, true);
//                            lastHeightRelevantPoint = neighbourNode;
//                            lastHeightRelevantPointIdx = neighbourNodeIdx;
//                            minHeight = neighbourNode.getEle();
//                            maxHeight = neighbourNode.getEle();
//                        }
//                        if (neighbourNode.isFlag(GNode.FLAG_FIX)) break; // main exit from loop!
//
//                        neighbour = tile.oppositeNeighbour(neighbourNode, neighbour.getReverse());
//                        neighbourNode = neighbour.getNeighbourNode();
//                    } // while true
//                    if (!lastHeightRelevantPoint.isFlag(GNode.FLAG_FIX) &&
//                            (distance (smoothNeighbourList,  lastHeightRelevantPointIdx+1, neighbourNodeIdx) < smoothingDistance)){
//                        lastHeightRelevantPoint.setFlag(GNode.FLAG_HEIGHT_RELEVANT, false); // reset last heightRelevantPoint in this segment - otherwise the remaining might bee too short
//                    }
//
//                    mgLog.v(()->smoothNeighbourList.get(0).getNeighbourNode()+" --- "+smoothNeighbourList.get(smoothNeighbourList.size()-1).getNeighbourNode()+" ("+smoothNeighbourList.size()+")");
//
//                    if (smoothNeighbourList.size() <= 2) continue;
//
//                    int startIdx = 0;
//                    while (startIdx < smoothNeighbourList.size() -1){
//                        int endIdx = startIdx;
//                        while ((startIdx == endIdx) || !smoothNeighbourList.get(endIdx).getNeighbourNode().isFlag(GNode.FLAG_HEIGHT_RELEVANT)){
//                            endIdx++;
//                        }
//                        float startHeight = smoothNeighbourList.get(startIdx).getNeighbourNode().getEle();
//                        float endHeight = smoothNeighbourList.get(endIdx).getNeighbourNode().getEle();
//                        double totalDistance = distance(smoothNeighbourList, startIdx, endIdx);
//
//                        endIdx = startIdx;
//                        double distance = 0;
//                        while ((startIdx == endIdx) || !smoothNeighbourList.get(endIdx).getNeighbourNode().isFlag(GNode.FLAG_HEIGHT_RELEVANT)){
//                            endIdx++;
//                            GNode endNode = smoothNeighbourList.get(endIdx).getNeighbourNode();
//                            if (!endNode.isFlag(GNode.FLAG_HEIGHT_RELEVANT)){
//                                distance += smoothNeighbourList.get(endIdx).getDistance();
//                                float height = (float)PointModelUtil.interpolate (0, totalDistance, startHeight, endHeight, distance);
//                                endNode.fixEle( Math.round(height*PointModelUtil.ELE_FACTOR)/PointModelUtil.ELE_FACTOR );
//                            }
//                        }
//                        startIdx = endIdx;
//                    }
//                } // iterate over all neighbours
//            } // if (aNode.isFlag(GNode.FLAG_FIX))
//        } // iterate over all nodes
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public void reduceIntermediates(DGraphTile gGraphTile){
//        ArrayList<GNode> intermediatesList = new ArrayList<>();
//        for (GNode aNode : gGraphTile.getGNodes()) { // iterate over all nodes
//            if (aNode.isFlag(GNode.FLAG_FIX)){
//                GNeighbour aNeighbour = null;
//                while ((aNeighbour = gGraphTile.getNextNeighbour(aNode, aNeighbour)) != null) { // iterate over all neighbours
//                    if (aNeighbour.isPrimaryDirection()){ // do work only for one direction
//                        GNode startNode = aNode;
//                        GNeighbour startNeighbour = aNeighbour;
//                        GNeighbour neighbour = aNeighbour;
//                        while (!neighbour.getNeighbourNode().isFlag(GNode.FLAG_FIX)){
//                            intermediatesList.add(neighbour.getNeighbourNode());
//                            neighbour = gGraphTile.oppositeNeighbour(neighbour.getNeighbourNode(), neighbour.getReverse());
//                        }
//                        if (!intermediatesList.isEmpty()){
//                            GNode endNode = neighbour.getNeighbourNode();
//                            GNeighbour endNeighbour = neighbour.getReverse();
//                            int numIntermediates = intermediatesList.size();
//                            int[] intermediatePoints = new int[3*numIntermediates];
//                            int[] intermediatePointsReverse = new int[3*numIntermediates];
//                            for (int iIdx=0; iIdx<numIntermediates; iIdx++){
//                                intermediatePoints[iIdx*3    ] = intermediatesList.get(iIdx).getLa();
//                                intermediatePointsReverse[3*(numIntermediates-1) - iIdx*3     ] = intermediatesList.get(iIdx).getLa();
//                                intermediatePoints[iIdx*3 + 1] = intermediatesList.get(iIdx).getLo();
//                                intermediatePointsReverse[3*(numIntermediates-1) - iIdx*3 + 1 ] = intermediatesList.get(iIdx).getLo();
//                                intermediatePoints[iIdx*3 + 2] = intermediatesList.get(iIdx).getEl();
//                                intermediatePointsReverse[3*(numIntermediates-1) - iIdx*3 + 2 ] = intermediatesList.get(iIdx).getEl();
//                            }
//                            // be careful - removal of neighbours and adding of shortcutNeighbours is done during iteration - nevertheless it should work this way
//                            gGraphTile.removeNeighbourTo(startNode, startNeighbour.getNeighbourNode());
//                            gGraphTile.removeNeighbourTo(endNode, endNeighbour.getNeighbourNode());
//                            GNeighbour shortcutNeighbour = gGraphTile.bidirectionalConnect(startNode, endNode, startNeighbour);
//                            shortcutNeighbour.setIntermediatesPoints(intermediatePoints);
//                            shortcutNeighbour.getReverse().setIntermediatesPoints(intermediatePointsReverse);
//
//                            intermediatesList.clear();
//                        } // if (!intermediatesList.isEmpty()){
//                    } // primaryDirection
//                } // iterate over all neighbours
//            } // if (aNode.isFlag(GNode.FLAG_FIX))
//        } // iterate over all nodes
//        gGraphTile.getGNodes().removeIf(node -> !node.isFlag(GNode.FLAG_FIX));
    }

//    private double distance(ArrayList<GNeighbour> smoothNeighbourList, int idx1, int idx2){
//        if (idx1 > idx2){
//            return distance(smoothNeighbourList, idx2, idx1);
//        }
//        double distance = 0;
//        for (int idx=idx1+1; idx <= idx2; idx++){
//            distance += smoothNeighbourList.get(idx).getDistance();
//        }
//        return distance;
//    }
}
