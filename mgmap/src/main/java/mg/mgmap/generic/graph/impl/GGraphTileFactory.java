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
package mg.mgmap.generic.graph.impl;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.Way;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
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
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

public class GGraphTileFactory implements GraphFactory {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final static int CACHE_LIMIT = 2000;
    final byte ZOOM_LEVEL = 15;
    final int TILE_SIZE = MapViewerBase.TILE_SIZE;
    static final int LOW_MEMORY_THRESHOLD = 2;

    static int getKey(int tileX,int tileY){
        return ( tileX <<16) + tileY;
    }

    private WayProvider wayProvider = null;
    private ElevationProvider elevationProvider = null;
    private boolean wayDetails;
    Pref<String> prefRoutingAlgorithm;
    Pref<Boolean> prefSmooth4Routing;
    GTileCache gTileCache;

    public GGraphTileFactory(){}

    public GGraphTileFactory onCreate(WayProvider wayProvider, ElevationProvider elevationProvider, boolean wayDetails, Pref<String> prefRoutingAlgorithm, Pref<Boolean> prefSmooth4Routing){
        this.wayProvider = wayProvider;
        this.elevationProvider = elevationProvider;
        this.wayDetails = wayDetails;
        this.prefRoutingAlgorithm = prefRoutingAlgorithm;
        this.prefSmooth4Routing = prefSmooth4Routing;

        gTileCache = new GTileCache(CACHE_LIMIT);
        return this;
    }

    public void onDestroy(){
        wayProvider = null;
        gTileCache = null;
    }

    public void resetCosts(){
        if (gTileCache != null){
            for (GGraphTile graph : gTileCache.getAll()){
                for (GNode node : graph.getGNodes()){
                    GNeighbour neighbour = node.getNeighbour();
                    while ((neighbour = graph.getNextNeighbour(node, neighbour)) != null) {
                        neighbour.resetCost();
                        WayAttributs wayAttributs = neighbour.getWayAttributs();
                        if (wayAttributs != null) {
                            wayAttributs.setDerivedData(null);
                        }
                    }
                }
            }
        }
    }

    public void clearCache(){
        gTileCache.clear();
    }

    public void serviceCache(){
        try {
            gTileCache.service();
        } catch (Exception e) {
            mgLog.e(e);
        }
    }

    ArrayList<GGraphTile> getCached(){
        return gTileCache.getAll();
    }

    public ArrayList<? extends Graph> getGraphList(BBox bBox){
        return getGGraphTileList(bBox);
    }

    public ArrayList<GGraphTile> getGGraphTileList(BBox bBox){
        ArrayList<GGraphTile> tileList = new ArrayList<>();
        try {
            long mapSize = MercatorProjection.getMapSize(ZOOM_LEVEL, TILE_SIZE);
            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.minLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.maxLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.maxLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.minLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);

            int totalTiles = (tileXMax-tileXMin+1) * (tileYMax-tileYMin+1);
            mgLog.d(()->"create GGraphTileList with tileXMin=" + tileXMin + " tileYMin=" + tileYMin + " and tileXMax=" + tileXMax + " tileYMax=" + tileYMax +
                        " - total tiles=" + totalTiles);
            if (totalTiles < CACHE_LIMIT){
                for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                    for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                        GGraphTile graph = getGGraphTile(tileX,tileY);
                        tileList.add(graph);
                    }
                }
            } else {
                mgLog.e("totalTiles exceeds CACHE_LIMIT: totalTiles="+ totalTiles+" CACHE_LIMIT="+ CACHE_LIMIT);
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
        return tileList;
    }

    public GGraph getGraph(PointModel pm1, PointModel pm2){
        ArrayList<GGraphTile> gGraphTiles = getGGraphTileList (new BBox().extend(pm1).extend(PointModelUtil.getCloseThreshold()));
        gGraphTiles.addAll(getGGraphTileList(new BBox().extend(pm2).extend(PointModelUtil.getCloseThreshold())));
        return new GGraphMulti(this, gGraphTiles);
    }

    public GraphAlgorithm getAlgorithmForGraph(Graph graph, RoutingProfile routingProfile){
        GGraphAlgorithm gGraphAlgorithm = null;
        if (graph instanceof GGraph gGraph){
            try {
                Class<?> clazz = Class.forName("mg.mgmap.generic.graph.impl."+prefRoutingAlgorithm.getValue());
                Constructor<?> constructor = clazz.getConstructor(GGraph.class, RoutingProfile.class);
                Object object = constructor.newInstance(gGraph, routingProfile);
                if (object instanceof GGraphAlgorithm) {
                    gGraphAlgorithm = (GGraphAlgorithm) object;
                }
            } catch (Exception e) {
                mgLog.e(e);
                gGraphAlgorithm = new BidirectionalAStar(gGraph, routingProfile); // fallback
            }
        }
        return gGraphAlgorithm;
    }

    public ApproachModel validateApproachModel(ApproachModel approachModel){
        if (approachModel instanceof ApproachModelImpl am){
            GGraphTile gGraphTile = getGGraphTile(am.getTileX(), am.getTileY());
            am.setNode1( gGraphTile.getNode(am.getNode1().getLat(),am.getNode1().getLon()) );
            am.setNode2( gGraphTile.getNode(am.getNode2().getLat(),am.getNode2().getLon()) );
            if (am.getNode1() == null) mgLog.e("node1==null ->rework failed");
            if (am.getNode2() == null) mgLog.e("node2==null ->rework failed");
        } else {
            mgLog.e("unexpected approachModel type - expected "+ApproachModelImpl.class.getName()+"; found "+approachModel.getClass().getName());
        }
        return approachModel;
    }

    public ApproachModel calcApproach(PointModel pointModel, int closeThreshold){
        BBox mtlpBBox = new BBox()
                .extend(pointModel)
                .extend(closeThreshold);

        ApproachModel bestApproach = null;
        WriteablePointModel pmApproach = new TrackLogPoint();

        ArrayList<GGraphTile> tiles = getGGraphTileList(mtlpBBox);
        for (GGraphTile gGraphTile : tiles){
            for (GNode node : gGraphTile.getGNodes()) {

                GNeighbour neighbour = null;
                while ((neighbour = gGraphTile.getNextNeighbour(node, neighbour)) != null) {
                    GNode neighbourNode = neighbour.getNeighbourNode();
                    if (gGraphTile.sameGraph(node, neighbourNode) && (PointModelUtil.compareTo(node, neighbourNode) < 0)){ // neighbour relations exist in both direction - here we can reduce to one
                        BBox bBoxPart = new BBox().extend(node).extend(neighbourNode);
                        boolean bIntersects = mtlpBBox.intersects(bBoxPart);
                        if (bIntersects){ // ok, is candidate for close
                            if (PointModelUtil.findApproach(pointModel, node, neighbourNode, pmApproach , closeThreshold)) {
                                float distance = (float)(PointModelUtil.distance(pointModel, pmApproach)+0.0001);
                                if (distance < closeThreshold){ // ok, is close ==> new Approach found
                                    if ((bestApproach == null) || (distance < bestApproach.getApproachDistance())){
                                        GNode approachNode = new GNode(pmApproach.getLat(), pmApproach.getLon(), pmApproach.getEle(), pmApproach.getEleAcc()); // so we get a new node for the approach, since pmApproach will be overwritten in next cycle
                                        bestApproach = new ApproachModelImpl(gGraphTile.getTileX(),gGraphTile.getTileY() ,pointModel, node, neighbour, neighbourNode, approachNode, distance);
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

    @SuppressWarnings("unused")
    public void connectApproach2Graph(Graph graph, ApproachModel approachModel){
        if (graph instanceof GGraph gGraph){
            if (approachModel != null) {
                if (approachModel instanceof ApproachModelImpl am){
                    GNode approachNode = am.getApproachNode();
                    GNode node1 = am.getNode1();
                    GNode node2 = am.getNode2();
                    GNeighbour neighbour12 = gGraph.getNeighbour(node1, node2);
                    GNeighbour neighbour21 = neighbour12.getReverse();
                    gGraph.bidirectionalConnect(node1, approachNode, neighbour12);
                    gGraph.bidirectionalConnect(node2, approachNode, neighbour21);
                } else {
                    mgLog.e("Unexpected approachModel type: "+approachModel.getClass().getName());
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public void disconnectApproach2Graph(Graph graph, ApproachModel approachModel){
        if (graph instanceof GGraph gGraph) {
            if (approachModel != null){
                if (approachModel instanceof ApproachModelImpl am){
                    if (am.getNode1() != null){
                        gGraph.removeNeighbourTo(am.getNode1(), 0);
                    }
                    if (am.getNode2() != null){
                        gGraph.removeNeighbourTo(am.getNode2(), 0);
                    }
                    am.getApproachNode().setNeighbour(null);
                } else {
                    mgLog.e("Unexpected approachModel type: "+approachModel.getClass().getName());
                }
            }
        }
    }

    public void checkDirectConnectApproaches(Graph graph, ApproachModel sourceApproachModel, ApproachModel targetApproachModel) {
        if ((graph instanceof GGraph gGraph) && (sourceApproachModel instanceof ApproachModelImpl sami) && (targetApproachModel instanceof ApproachModelImpl tami)) { // expected to be always true
            if ((sami.getNode1() == tami.getNode1()) && (sami.getNode2() == tami.getNode2())) { // source and target have same approach segment
                if (PointModelUtil.distance(sami.getNode1(), sami.getApproachNode()) < PointModelUtil.distance(tami.getNode1(), tami.getApproachNode())) {
                    gGraph.bidirectionalConnect(sami.getApproachNode(), tami.getApproachNode(), sami.getNeighbour1To2());
                } else {
                    gGraph.bidirectionalConnect(sami.getApproachNode(), tami.getApproachNode(), sami.getNeighbour1To2().getReverse());
                }
            } // else approaches do not overlap
        }
    }

    public GGraphTile getGGraphTile(int tileX, int tileY){
        return getGGraphTile(tileX, tileY, true);
    }
    public GGraphTile getGGraphTile(int tileX, int tileY, boolean load){
        GGraphTile gGraphTile = gTileCache.get(tileX, tileY);
        if (load && (gGraphTile == null)){
            synchronized (this){  // prevent parallel access from routing thread and FSGraphDetails
                gGraphTile = gTileCache.get(tileX, tileY); // check, if tile is meanwhile in cache
                if (gGraphTile == null) {
                    gGraphTile = loadGGraphTile(tileX, tileY);
                    if (prefSmooth4Routing.getValue()){
                        smoothGGraphTile(gGraphTile);
                    }
                    gTileCache.put(tileX, tileY, gGraphTile);
                }
            }
        }
        return gGraphTile;
    }

    @SuppressWarnings("CommentedOutCode")
    public GGraphTile loadGGraphTile(int tileX, int tileY){
        mgLog.d(()->"Load tileX=" + tileX + " tileY=" + tileY + " (" + gTileCache.size() + ")");
        Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
        GGraphTile gGraphTile = new GGraphTile(elevationProvider, tile);
        for (Way way : wayProvider.getWays(tile)) {
            if (wayProvider.isWayForRouting(way)){

                WayAttributs wayAttributs = new WayAttributs(way);
                gGraphTile.addLatLongs( wayAttributs, way.latLongs[0]);

                // now setup rawWays
                if (wayDetails){
                    MultiPointModelImpl mpm = new MultiPointModelImpl();
                    for (LatLong latLong : way.latLongs[0] ){
                        // for points inside the tile use the GNodes as already allocated
                        // for points outside use extra Objects, don't pollute the graph with them
                        if (gGraphTile.bBox.contains(latLong.latitude, latLong.longitude)){
                            mpm.addPoint(gGraphTile.getAddNode(latLong.latitude, latLong.longitude));
                        } else {
                            mpm.addPoint(new PointModelImpl(latLong));
                        }
                    }
                    gGraphTile.getRawWays().add(mpm);
                }
            }
        }
        gGraphTile.timestamps.add(System.nanoTime());
        int latThreshold = LaLo.d2md( PointModelUtil.latitudeDistance(GGraph.CONNECT_THRESHOLD_METER) );
        int lonThreshold = LaLo.d2md( PointModelUtil.longitudeDistance(GGraph.CONNECT_THRESHOLD_METER, tile.getBoundingBox().getCenterPoint().getLatitude()) );
//            Log.v(MGMapApplication.LABEL, NameUtil.context()+" latThreshold="+latThreshold+" lonThreshold="+lonThreshold);
        //all highways are in the map ... try to correct data ...
        ArrayList<GNode> nodes = gGraphTile.getGNodes();
        for (int iIdx=0; iIdx<nodes.size(); iIdx++){
            GNode iNode = nodes.get(iIdx);
            if (iNode.isFlag(GNode.FLAG_INVALID)) continue; // invalid node
            int iNeighbours = gGraphTile.countNeighbours(iNode);
            for (int nIdx=iIdx+1; nIdx<nodes.size(); nIdx++ ) {
                GNode nNode = nodes.get(nIdx);
                if (nNode.isFlag(GNode.FLAG_INVALID)) continue; // invalid node
                if (iNode.laMdDiff(nNode) >= latThreshold) break; // go to next iIdx
                if (iNode.loMdDiff(nNode) >= lonThreshold)
                    continue; // goto next mIdx
                if (PointModelUtil.distance(iNode, nNode) > GGraph.CONNECT_THRESHOLD_METER)
                    continue;
                if (gGraphTile.getNeighbour(iNode,nNode)!=null)
                    continue; // is already neighbour

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

                int nNeighbours = gGraphTile.countNeighbours(nNode);
                if ((iNeighbours == 0) || (nNeighbours == 0)) { // don't connect, if a node has no neighbours (might occur due to former reduceGraph action)
                    continue;
                }
                if ((iNeighbours == 1) && (nNeighbours == 1)) { // 1:1 connect -> no routing hint problem
                    gGraphTile.addSegment(null,iNode, nNode);
                    continue;
                }
                if (isBorderPoint(gGraphTile.bBox, nNode) || isBorderPoint(gGraphTile.bBox, iNode)) { // border points must be kept for MultiTiles; accept potential routing hint problem
                    gGraphTile.addSegment(null,iNode, nNode);
                    continue;
                }
                if ((iNeighbours == 2) && (nNeighbours == 1)) { // 2:1 connect -> might give routing hint problem
                    reduceGraph(gGraphTile, iNode, nNode);  // drop nNode; move neighbour form nNode to iNode
                    continue;
                }
                if ((iNeighbours == 1) && (nNeighbours == 2)) { // 1:2 connect -> might give routing hint problem
                    reduceGraph(gGraphTile, nNode, iNode); // drop iNode; move neighbour form iNode to nNode
                    iNeighbours = 0; // just in case there is a second close nNode
                    continue;
                }
                // else (n:m) accept routing hint issue
                gGraphTile.addSegment(null,iNode, nNode);

            }
        }
        gGraphTile.getGNodes().removeIf(node -> node.isFlag(GNode.FLAG_INVALID));
        gGraphTile.timestamps.add(System.nanoTime());
        return gGraphTile;
    }

    private boolean isBorderPoint(BBox bBox, GNode node){
        return ((node.getLat() == bBox.maxLatitude) || (node.getLat() == bBox.minLatitude) ||
                (node.getLon() == bBox.maxLongitude) || (node.getLon() == bBox.minLongitude));
    }

    // reduce Graph by dropping nNode, all Neighbours form nNode will get iNode as a Neighbour
    private void reduceGraph(GGraphTile graph, GNode iNode, GNode nNode){
        // iterate over al neighbours from nNode
        GNeighbour nextNeighbour = null;
        while (graph.getNextNeighbour(nNode, nextNeighbour) != null) {
            nextNeighbour = graph.getNextNeighbour(nNode, nextNeighbour);
            // remove nNode as a Neighbour
            graph.removeNeighbourTo(nextNeighbour.getNeighbourNode(), nNode);
            graph.addSegment(nextNeighbour.getWayAttributs(),iNode, nextNeighbour.getNeighbourNode());
        }
        nNode.setFlag(GNode.FLAG_INVALID, true);
        nNode.getNeighbour().setNextNeighbour(null);
    }

    private void smoothGGraphTile(GGraphTile tile){
        ArrayList<GNeighbour> smoothNeighbourList = new ArrayList<>();
        for (GNode aNode : tile.getGNodes()){
            boolean fix = true;
            GNeighbour firstNeighbour = tile.getNextNeighbour(aNode, null);
            if (firstNeighbour != null){
                GNeighbour secondNeighbour = tile.getNextNeighbour(aNode, firstNeighbour);
                if (secondNeighbour != null){
                    if ((tile.getNextNeighbour(aNode, secondNeighbour) == null) &&
                            (firstNeighbour.getWayAttributs() == secondNeighbour.getWayAttributs()) &&
                            (aNode.borderNode == 0)){
                        fix = false;
                    }
                }
            }
            aNode.setFlag(GNode.FLAG_FIX, fix);
            aNode.setFlag(GNode.FLAG_VISITED, false);
            aNode.setFlag(GNode.FLAG_HEIGHT_RELEVANT, fix);
        }
        for (GNode aNode : tile.getGNodes()) { // iterate over all nodes
            if (aNode.isFlag(GNode.FLAG_FIX)){
                GNode minHeightPoint;
                GNode maxHeightPoint;
                GNeighbour aNeighbour = null;
                while ((aNeighbour = tile.getNextNeighbour(aNode, aNeighbour)) != null) { // iterate over all neighbours
//                    aNeighbour = tile.getNextNeighbour(aNode, aNeighbour);

                    GNeighbour neighbour = aNeighbour;
                    GNode neighbourNode = neighbour.getNeighbourNode();
                    if (neighbourNode.isFlag(GNode.FLAG_VISITED)) continue; // this path is already handled
                    int neighbourNodeIdx;
                    // reset smoothNodeList
                    smoothNeighbourList.clear();
                    smoothNeighbourList.add(aNode.getNeighbour().getReverse()); // a neighbour with getNeighbourNode = aNode
                    float minHeight = aNode.getEle();
                    float maxHeight = aNode.getEle();
                    minHeightPoint = aNode;
                    maxHeightPoint = aNode;
                    int minHeightPointIdx = 0;
                    int maxHeightPointIdx = 0;
                    GNode lastHeightRelevantPoint = aNode;
                    int lastHeightRelevantPointIdx = 0;
                    int signumLastHeightInterval = 0;
                    int signumHeightInterval = 0;

                    while (true){
                        neighbourNode.setFlag(GNode.FLAG_VISITED, true);
                        neighbourNodeIdx = smoothNeighbourList.size();
                        smoothNeighbourList.add(neighbour);
                        if (neighbourNode.getEle() < minHeight){
                            minHeightPoint = neighbourNode;
                            minHeight = neighbourNode.getEle();
                            minHeightPointIdx = neighbourNodeIdx;
                        }
                        if (neighbourNode.getEle() > maxHeight){
                            maxHeightPoint = neighbourNode;
                            maxHeight = neighbourNode.getEle();
                            maxHeightPointIdx = neighbourNodeIdx;
                        }

                        if ( (maxHeight - minHeight >= TrackLogStatistic.ELE_THRESHOLD_ELSE) && (distance(smoothNeighbourList, minHeightPointIdx, maxHeightPointIdx) > PointModelUtil.getCloseThreshold()/2d)){
                            neighbourNode.setFlag(GNode.FLAG_HEIGHT_RELEVANT, true);
                            if ( maxHeight == neighbourNode.getEle() ){
                                signumHeightInterval = 1;
                                if (minHeightPoint != lastHeightRelevantPoint){
                                    if (!lastHeightRelevantPoint.isFlag(GNode.FLAG_FIX)){
                                        minHeightPoint.setFlag(GNode.FLAG_HEIGHT_RELEVANT, true);
                                        if (distance (smoothNeighbourList, lastHeightRelevantPointIdx, minHeightPointIdx) <  PointModelUtil.getCloseThreshold()/2d){
                                            if ( signumLastHeightInterval == Math.signum( minHeightPoint.getEle() - lastHeightRelevantPoint.getEle() ) ) lastHeightRelevantPoint.setFlag(GNode.FLAG_HEIGHT_RELEVANT, false); // reset height relevance
                                        }
                                    }

                                }
                            }
                            if ( minHeight == neighbourNode.getEle() ){
                                signumHeightInterval = -1;
                                if (maxHeightPoint != lastHeightRelevantPoint){
                                    if (!lastHeightRelevantPoint.isFlag(GNode.FLAG_FIX)) {
                                        maxHeightPoint.setFlag(GNode.FLAG_HEIGHT_RELEVANT, true);
                                        if (distance (smoothNeighbourList, lastHeightRelevantPointIdx, maxHeightPointIdx) < PointModelUtil.getCloseThreshold()/2d){
                                            if ( signumLastHeightInterval == Math.signum( maxHeightPoint.getEle() - lastHeightRelevantPoint.getEle() ) ) lastHeightRelevantPoint.setFlag(GNode.FLAG_HEIGHT_RELEVANT, false); // reset height relevance
                                        }
                                    }
                                }
                            }
                            lastHeightRelevantPoint = neighbourNode;
                            lastHeightRelevantPointIdx = neighbourNodeIdx;
                            signumLastHeightInterval = signumHeightInterval;
                            minHeight = neighbourNode.getEle();
                            maxHeight = neighbourNode.getEle();
                            minHeightPoint = neighbourNode;
                            maxHeightPoint = neighbourNode;

                        }
                        if (neighbourNode.isFlag(GNode.FLAG_FIX)) break; // main exit from loop!

                        neighbour = tile.oppositeNeighbour(neighbourNode, neighbour.getReverse());
                        neighbourNode = neighbour.getNeighbourNode();
                    } // while true
                    if (!lastHeightRelevantPoint.isFlag(GNode.FLAG_FIX) &&
                            (distance (smoothNeighbourList,  lastHeightRelevantPointIdx, neighbourNodeIdx) < PointModelUtil.getCloseThreshold()/2d)){
                        lastHeightRelevantPoint.setFlag(GNode.FLAG_HEIGHT_RELEVANT, false); // reset last heightRelevantPoint in this segment - otherwise the remaining
                    }

                    mgLog.v(()->smoothNeighbourList.get(0).getNeighbourNode()+" --- "+smoothNeighbourList.get(smoothNeighbourList.size()-1).getNeighbourNode()+" ("+smoothNeighbourList.size()+")");

                    if (smoothNeighbourList.size() <= 2) continue;

                    int startIdx = 0;
                    while (startIdx < smoothNeighbourList.size() -1){
                        int endIdx = startIdx;
                        while ((startIdx == endIdx) || !smoothNeighbourList.get(endIdx).getNeighbourNode().isFlag(GNode.FLAG_HEIGHT_RELEVANT)){
                            endIdx++;
                        }
                        float startHeight = smoothNeighbourList.get(startIdx).getNeighbourNode().getEle();
                        float endHeight = smoothNeighbourList.get(endIdx).getNeighbourNode().getEle();
                        double totalDistance = distance(smoothNeighbourList, startIdx, endIdx);

                        endIdx = startIdx;
                        double distance = 0;
                        while ((startIdx == endIdx) || !smoothNeighbourList.get(endIdx).getNeighbourNode().isFlag(GNode.FLAG_HEIGHT_RELEVANT)){
                            endIdx++;
                            GNode endNode = smoothNeighbourList.get(endIdx).getNeighbourNode();
                            if (!endNode.isFlag(GNode.FLAG_HEIGHT_RELEVANT)){
                                distance += smoothNeighbourList.get(endIdx).getDistance();
                                float height = (float)PointModelUtil.interpolate (0, totalDistance, startHeight, endHeight, distance);
                                endNode.fixEle( Math.round(height*PointModelUtil.ELE_FACTOR)/PointModelUtil.ELE_FACTOR );
                            }
                        }

                        startIdx = endIdx;
                    }
                } // iterate over all neighbours
            } // if (aNode.isFlag(GNode.FLAG_FIX))
        } // iterate over all nodes
    }

    private double distance(ArrayList<GNeighbour> smoothNeighbourList, int idx1, int idx2){
        if (idx1 > idx2){
            return distance(smoothNeighbourList, idx2, idx1);
        }
        double distance = 0;
        for (int idx=idx1+1; idx <= idx2; idx++){
            distance += smoothNeighbourList.get(idx).getDistance();
        }
        return distance;
    }
}
