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
package mg.mgmap.generic.graph.impl2;

import android.os.Build;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.Way;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Locale;

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
import mg.mgmap.generic.model.PointNeighbour;
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
    ArrayList<PointModel> neighbourPoints = new ArrayList<>();
    Pref<String> prefSmoothDistance;

    public GGraphTileFactory(){}

    public GGraphTileFactory onCreate(WayProvider wayProvider, ElevationProvider elevationProvider, boolean wayDetails,
                                      Pref<String> prefRoutingAlgorithm, Pref<Boolean> prefSmooth4Routing, Pref<String> prefSmoothDistance){
        this.wayProvider = wayProvider;
        this.elevationProvider = elevationProvider;
        this.wayDetails = wayDetails;
        this.prefRoutingAlgorithm = prefRoutingAlgorithm;
        this.prefSmooth4Routing = prefSmooth4Routing;
        this.prefSmoothDistance = prefSmoothDistance;

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
                    GNeighbour neighbour = null;
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
                String packageName = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)?this.getClass().getPackageName():"mg.mgmap.generic.graph.impl2";
                Class<?> clazz = Class.forName(packageName+"."+prefRoutingAlgorithm.getValue());
                Constructor<?> constructor = clazz.getConstructor(GGraph.class, RoutingProfile.class);
                Object object = constructor.newInstance(gGraph, routingProfile);
                if (object instanceof GGraphAlgorithm) {
                    gGraphAlgorithm = (GGraphAlgorithm) object;
                }
            } catch (Exception e) {
                gGraphAlgorithm = new BidirectionalAStar(gGraph, routingProfile); // fallback
            }
        }
        return gGraphAlgorithm;
    }

    public ApproachModel validateApproachModel(ApproachModel approachModel){
        if (approachModel instanceof ApproachModelImpl am){
            GGraphTile gGraphTile = getGGraphTile(am.getTileX(), am.getTileY());

            am.setNode1(GNodeUtil.validateNode(gGraphTile, am.getNode1()));
            am.setNode2(GNodeUtil.validateNode(gGraphTile, am.getNode2()));
            am.setNeighbour1To2(GNodeUtil.validateNeighbour(gGraphTile, am.getNeighbour1To2()));
            am.setApproachNode(new GNode(am.getApproachNode().getLat(),am.getApproachNode().getLon(),am.getApproachNode().getEle(),am.getApproachNode().getEleAcc()));

            if ((am.getNode1() == null) || (am.getNode2() == null) || (am.getApproachNode() == null) || (am.getNeighbour1To2() == null)) mgLog.e("rework failed: "+am);
        } else {
            mgLog.e("unexpected approachModel type - expected "+ ApproachModelImpl.class.getName()+"; found "+approachModel.getClass().getName());
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
                    if (neighbour.isPrimaryDirection()){ // neighbour relations exist in both direction - here we can reduce to one
                        GNode neighbourNode = neighbour.getNeighbourNode();
                        if (node.tileIdx != neighbourNode.tileIdx) continue; // don't use neighbours that connect tiles (approach should belong fully to one tile)
                        int cntIntermediates = neighbour.cntIntermediates();
                        if (cntIntermediates == 0){
                            bestApproach = checkForBetterApproach(pointModel, closeThreshold, mtlpBBox, pmApproach, gGraphTile, node, neighbour, neighbourNode, bestApproach);
                        } else {
                            GIntermediateNode gin1;
                            GIntermediateNode gin2 = new GIntermediateNode(node, neighbour, 0);
                            bestApproach = checkForBetterApproach(pointModel, closeThreshold, mtlpBBox, pmApproach, gGraphTile, node, neighbour, gin2, bestApproach);
                            for (int pIdx = 0; pIdx < cntIntermediates-1; pIdx++){
                                gin1 = gin2;
                                gin2 = new GIntermediateNode(node, neighbour, pIdx+1);
                                bestApproach = checkForBetterApproach(pointModel, closeThreshold, mtlpBBox, pmApproach, gGraphTile, gin1, neighbour, gin2, bestApproach);
                            }
                            gin1 = gin2;
                            bestApproach = checkForBetterApproach(pointModel, closeThreshold, mtlpBBox, pmApproach, gGraphTile, gin1, neighbour, neighbourNode, bestApproach);
                        }
                    }
                }
            }
        }
        return bestApproach;
    }

    private ApproachModel checkForBetterApproach(PointModel pointModel, int closeThreshold, BBox mtlpBBox, WriteablePointModel pmApproach, GGraphTile gGraphTile, PointModel node, PointNeighbour neighbour, PointModel neighbourNode, ApproachModel bestApproach){
        BBox bBoxPart = new BBox().extend(node).extend(neighbourNode);
        boolean bIntersects = mtlpBBox.intersects(bBoxPart);
        if (bIntersects){ // ok, is candidate for close
            if (PointModelUtil.findApproach(pointModel, node, neighbourNode, pmApproach , closeThreshold)) {
                float distance = (float)(PointModelUtil.distance(pointModel, pmApproach)+0.0001);
                if (distance < closeThreshold){ // ok, is close ==> new Approach found
                    if ((bestApproach == null) || (distance < bestApproach.getApproachDistance())){
                        GNode approachNode = new GNode(pmApproach.getLat(), pmApproach.getLon(), Math.round(pmApproach.getEle()*PointModelUtil.ELE_FACTOR)/PointModelUtil.ELE_FACTOR, pmApproach.getEleAcc()); // so we get a new node for the approach, since pmApproach will be overwritten in next cycle
                        bestApproach = new ApproachModelImpl(gGraphTile.getTileX(),gGraphTile.getTileY() ,pointModel, node, neighbour, neighbourNode, approachNode, distance);
                    }
                }
            }
        }
        return bestApproach;
    }


    public void connectApproach2Graph(Graph graph, ApproachModel approachModel){
        if (graph instanceof GGraph gGraph){
            if (approachModel != null) {
                if (approachModel instanceof ApproachModelImpl am){
                    GNode approachNode = am.getApproachNode();
                    PointModel node1 = am.getNode1();
                    GNode gNode0 = null;
                    GNode gNode1 = null;
                    if (node1 instanceof GNode gNode){
                        gNode0 = gNode;
                        gNode1 = gNode;
                    } else if (node1 instanceof GIntermediateNode giNode){
                        gNode0 = giNode.node;
                        gNode1 = new GNode(giNode.getLat(), giNode.getLon(), giNode.getEle(), giNode.getEleAcc());
                        int intermediates = giNode.getPIdx();
                        if (intermediates > 0){
                            int[] intermediatePoints = new int[3*intermediates];
                            int[] intermediatePointsReverse = new int[3*intermediates];
                            System.arraycopy(giNode.neighbour.getIntermediatesPoints(), 0, intermediatePoints, 0, 3*intermediates);
                            System.arraycopy(giNode.neighbour.getReverse().getIntermediatesPoints(), (giNode.neighbour.cntIntermediates() - intermediates) * 3, intermediatePointsReverse, 0, 3*intermediates);
                            GNeighbour startNeighbour = gGraph.bidirectionalConnect(gNode0, gNode1, giNode.neighbour);
                            startNeighbour.setIntermediatesPoints(intermediatePoints);
                            startNeighbour.getReverse().setIntermediatesPoints(intermediatePointsReverse);
                        } else {
                            gGraph.bidirectionalConnect(gNode0, gNode1, giNode.neighbour);
                        }
                    }
                    PointModel node2 = am.getNode2();
                    GNode gNode2 = null;
                    GNode gNode3 = null;
                    if (node2 instanceof GNode gNode){
                        gNode2 = gNode;
                        gNode3 = gNode;
                    } else if (node2 instanceof GIntermediateNode giNode){
                        gNode2 = new GNode(giNode.getLat(), giNode.getLon(), giNode.getEle(), giNode.getEleAcc());
                        gNode3 = giNode.neighbour.getNeighbourNode();
                        int intermediates = giNode.neighbour.cntIntermediates() - giNode.getPIdx() - 1;
                        if (intermediates > 0){
                            int[] intermediatePoints = new int[3*intermediates];
                            int[] intermediatePointsReverse = new int[3*intermediates];
                            System.arraycopy(giNode.neighbour.getIntermediatesPoints(), (giNode.neighbour.cntIntermediates() - intermediates) * 3, intermediatePoints, 0, 3*intermediates);
                            System.arraycopy(giNode.neighbour.getReverse().getIntermediatesPoints(), 0, intermediatePointsReverse, 0, 3*intermediates);
                            GNeighbour endNeighbour = gGraph.bidirectionalConnect(gNode3, gNode2, giNode.neighbour.getReverse());
                            endNeighbour.setIntermediatesPoints(intermediatePointsReverse);
                            endNeighbour.getReverse().setIntermediatesPoints(intermediatePoints);
                        } else {
                            gGraph.bidirectionalConnect(gNode3, gNode2, giNode.neighbour.getReverse());
                        }
                    }
                    assert (gNode0 != null);
                    assert (gNode3 != null);
                    GNeighbour neighbour12 = (GNeighbour) (am.getNeighbour1To2()) ;
                    GNeighbour neighbour21 = neighbour12.getReverse();
                    gGraph.bidirectionalConnect(gNode1, approachNode, neighbour12);
                    gGraph.bidirectionalConnect(gNode2, approachNode, neighbour21);
                } else {
                    mgLog.e("Unexpected approachModel type: "+approachModel.getClass().getName());
                }
            }
        }
    }

    public void checkDirectConnectApproaches(Graph graph, ApproachModel sourceApproachModel, ApproachModel targetApproachModel) {
        if ((graph instanceof GGraph gGraph) && (sourceApproachModel instanceof ApproachModelImpl sami) && (targetApproachModel instanceof ApproachModelImpl tami) && (sami.getNeighbour1To2() instanceof GNeighbour neighbour12)) { // expected to be always true
            if ((sami.getNode1() instanceof GNode) && (sami.getNode2() instanceof GNode)) { // case 1: GNode node1 and GNode node2 in sami: source approach model segment has no intermediates
                if ((sami.getNode1() == tami.getNode1()) && (sami.getNode2() == tami.getNode2())) { // source and target have same approach segment
                    if (PointModelUtil.distance(sami.getNode1(), sami.getApproachNode()) < PointModelUtil.distance(tami.getNode1(), tami.getApproachNode())){
                        gGraph.bidirectionalConnect(sami.getApproachNode(), tami.getApproachNode(), neighbour12);
                    } else {
                        gGraph.bidirectionalConnect(sami.getApproachNode(), tami.getApproachNode(), neighbour12.getReverse());
                    }
                } // else approaches do not overlap
            } else { // sami point to segment with intermediates
                GNode nodeS = null, nodeT = null;
                GNeighbour neighbourS = null, neighbourT = null;
                int pIdx1S = 0, pIdx2S = 0, pIdx1T = 0, pIdx2T = 0;
                if (sami.getNode1() instanceof GIntermediateNode gin){
                    nodeS = gin.node;
                    neighbourS = gin.neighbour;
                    pIdx1S = gin.pIdx;
                    pIdx2S = gin.pIdx+1;
                }
                if (sami.getNode2() instanceof GIntermediateNode gin){
                    nodeS = gin.node;
                    neighbourS = gin.neighbour;
                    pIdx1S = gin.pIdx-1;
                    pIdx2S = gin.pIdx;
                }
                if (tami.getNode1() instanceof GIntermediateNode gin){
                    nodeT = gin.node;
                    neighbourT = gin.neighbour;
                    pIdx1T = gin.pIdx;
                    pIdx2T = gin.pIdx+1;
                }
                if (tami.getNode2() instanceof GIntermediateNode gin){
                    nodeT = gin.node;
                    neighbourT = gin.neighbour;
                    pIdx1T = gin.pIdx-1;
                    pIdx2T = gin.pIdx;
                }
                if ((nodeS == nodeT) && (neighbourS == neighbourT) && (neighbourS != null)) { // sami and tami address same segment with intermediates (third condition should always be true)
                    if ((pIdx1S > pIdx1T) || (pIdx1S == pIdx1T) && (PointModelUtil.distance(sami.getNode1(), sami.getApproachNode()) > PointModelUtil.distance(tami.getNode1(), tami.getApproachNode()))) { // reverse order of sami and tami in relation to node1->node2
                        // exchange sami and tami (for connect it is irrelevant, which node is source and which is target)
                        int tempPIdx1 = pIdx1S;
                        int tempPIdx2 = pIdx2S;
                        ApproachModelImpl tempAmi = sami;
                        pIdx1S = pIdx1T;
                        pIdx2S = pIdx2T;
                        sami = tami;
                        pIdx1T = tempPIdx1;
                        pIdx2T = tempPIdx2;
                        tami = tempAmi;
                    }
                    int intermediates = pIdx1T - pIdx1S;
                    if (intermediates > 0) {
                        int[] intermediatePoints = new int[3 * intermediates];
                        int[] intermediatePointsReverse = new int[3 * intermediates];
                        System.arraycopy(neighbourS.getIntermediatesPoints(), (pIdx2S) * 3, intermediatePoints, 0, 3 * intermediates);
                        System.arraycopy(neighbourS.getReverse().getIntermediatesPoints(), (neighbourS.getReverse().cntIntermediates() - pIdx2T) * 3, intermediatePointsReverse, 0, 3 * intermediates);
                        GNeighbour directNeighbour = gGraph.bidirectionalConnect(sami.getApproachNode(), tami.getApproachNode(), neighbourS);
                        directNeighbour.setIntermediatesPoints(intermediatePoints);
                        directNeighbour.getReverse().setIntermediatesPoints(intermediatePointsReverse);
                        mgLog.d();
                    } else { // no intermediates -> connect directly
                        gGraph.bidirectionalConnect(sami.getApproachNode(), tami.getApproachNode(), neighbourS);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    public void disconnectApproach2Graph(Graph graph, ApproachModel approachModel){
        if (graph instanceof GGraph gGraph) {
            if (approachModel != null){
                if (approachModel instanceof ApproachModelImpl am){
                    if (am.getNode1() instanceof GNode gNode1){
                        gGraph.removeNeighbourTo(gNode1, 0);
                    }
                    if (am.getNode1() instanceof GIntermediateNode giNode1){
                        gGraph.removeNeighbourTo(giNode1.node, 0);
                    }

                    if (am.getNode2() instanceof GNode gNode2){
                        gGraph.removeNeighbourTo(gNode2, 0);
                    }
                    if (am.getNode2() instanceof GIntermediateNode giNode2){
                        gGraph.removeNeighbourTo(giNode2.neighbour.getNeighbourNode(), 0);
                    }
                    am.getApproachNode().setNeighbour(null);
                } else {
                    mgLog.e("Unexpected approachModel type: "+approachModel.getClass().getName());
                }
            }
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
                        reduceIntermediates(gGraphTile);
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
                if (!neighbourPoints.isEmpty()) neighbourPoints.clear(); // reuse list object, but clear if necessary
                if (gGraphTile.getNeighbours(iNode, neighbourPoints).contains(nNode)){
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
        double smoothingDistance = PointModelUtil.getCloseThreshold()/2d;
        try {
            smoothingDistance = Double.parseDouble(prefSmoothDistance.getValue());
        } catch (NumberFormatException e) {
            mgLog.w(e.getMessage());
        }
        mgLog.d(String.format(Locale.ENGLISH, "Smoothing distance: %.1fm",smoothingDistance));
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
                GNeighbour aNeighbour = null;
                while ((aNeighbour = tile.getNextNeighbour(aNode, aNeighbour)) != null) { // iterate over all neighbours

                    GNeighbour neighbour = aNeighbour;
                    GNode neighbourNode = neighbour.getNeighbourNode();
                    if (neighbourNode.isFlag(GNode.FLAG_FIX) || neighbourNode.isFlag(GNode.FLAG_VISITED)){
                        continue; // this path is already handled
                    }
                    int neighbourNodeIdx;
                    // reset smoothNodeList
                    smoothNeighbourList.clear();
                    smoothNeighbourList.add(aNode.getNeighbour().getReverse()); // a neighbour with getNeighbourNode = aNode
                    float minHeight = aNode.getEle();
                    float maxHeight = aNode.getEle();
                    GNode lastHeightRelevantPoint = aNode;
                    int lastHeightRelevantPointIdx = 0;

                    while (true){
                        neighbourNode.setFlag(GNode.FLAG_VISITED, true);
                        neighbourNodeIdx = smoothNeighbourList.size();
                        smoothNeighbourList.add(neighbour);
                        if (neighbourNode.getEle() < minHeight){
                            minHeight = neighbourNode.getEle();
                        }
                        if (neighbourNode.getEle() > maxHeight){
                            maxHeight = neighbourNode.getEle();
                        }


                        double distance2LastHeightRelevant = distance(smoothNeighbourList, lastHeightRelevantPointIdx, neighbourNodeIdx);
                        if ((distance2LastHeightRelevant > smoothingDistance*5) || ((Math.abs(neighbourNode.getEle()-lastHeightRelevantPoint.getEle()) >= TrackLogStatistic.ELE_THRESHOLD_ELSE)   && (distance2LastHeightRelevant > smoothingDistance)  && !neighbourNode.isFlag(GNode.FLAG_FIX))){
                            neighbourNode.setFlag(GNode.FLAG_HEIGHT_RELEVANT, true);
                            lastHeightRelevantPoint = neighbourNode;
                            lastHeightRelevantPointIdx = neighbourNodeIdx;
                            minHeight = neighbourNode.getEle();
                            maxHeight = neighbourNode.getEle();
                        }
                        if (neighbourNode.isFlag(GNode.FLAG_FIX)) break; // main exit from loop!

                        neighbour = tile.oppositeNeighbour(neighbourNode, neighbour.getReverse());
                        neighbourNode = neighbour.getNeighbourNode();
                    } // while true
                    if (!lastHeightRelevantPoint.isFlag(GNode.FLAG_FIX) &&
                            (distance (smoothNeighbourList,  lastHeightRelevantPointIdx+1, neighbourNodeIdx) < smoothingDistance)){
                        lastHeightRelevantPoint.setFlag(GNode.FLAG_HEIGHT_RELEVANT, false); // reset last heightRelevantPoint in this segment - otherwise the remaining might bee too short
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

    @SuppressWarnings("UnnecessaryLocalVariable")
    public void reduceIntermediates(GGraphTile gGraphTile){
        ArrayList<GNode> intermediatesList = new ArrayList<>();
        for (GNode aNode : gGraphTile.getGNodes()) { // iterate over all nodes
            if (aNode.isFlag(GNode.FLAG_FIX)){
                GNeighbour aNeighbour = null;
                while ((aNeighbour = gGraphTile.getNextNeighbour(aNode, aNeighbour)) != null) { // iterate over all neighbours
                    if (aNeighbour.isPrimaryDirection()){ // do work only for one direction
                        GNode startNode = aNode;
                        GNeighbour startNeighbour = aNeighbour;
                        GNeighbour neighbour = aNeighbour;
                        while (!neighbour.getNeighbourNode().isFlag(GNode.FLAG_FIX)){
                            intermediatesList.add(neighbour.getNeighbourNode());
                            neighbour = gGraphTile.oppositeNeighbour(neighbour.getNeighbourNode(), neighbour.getReverse());
                        }
                        if (!intermediatesList.isEmpty()){
                            GNode endNode = neighbour.getNeighbourNode();
                            GNeighbour endNeighbour = neighbour.getReverse();
                            int numIntermediates = intermediatesList.size();
                            int[] intermediatePoints = new int[3*numIntermediates];
                            int[] intermediatePointsReverse = new int[3*numIntermediates];
                            for (int iIdx=0; iIdx<numIntermediates; iIdx++){
                                intermediatePoints[iIdx*3    ] = intermediatesList.get(iIdx).getLa();
                                intermediatePointsReverse[3*(numIntermediates-1) - iIdx*3     ] = intermediatesList.get(iIdx).getLa();
                                intermediatePoints[iIdx*3 + 1] = intermediatesList.get(iIdx).getLo();
                                intermediatePointsReverse[3*(numIntermediates-1) - iIdx*3 + 1 ] = intermediatesList.get(iIdx).getLo();
                                intermediatePoints[iIdx*3 + 2] = intermediatesList.get(iIdx).getEl();
                                intermediatePointsReverse[3*(numIntermediates-1) - iIdx*3 + 2 ] = intermediatesList.get(iIdx).getEl();
                            }
                            // be careful - removal of neighbours and adding of shortcutNeighbours is done during iteration - nevertheless it should work this way
                            gGraphTile.removeNeighbourTo(startNode, startNeighbour.getNeighbourNode());
                            gGraphTile.removeNeighbourTo(endNode, endNeighbour.getNeighbourNode());
                            GNeighbour shortcutNeighbour = gGraphTile.bidirectionalConnect(startNode, endNode, startNeighbour);
                            shortcutNeighbour.setIntermediatesPoints(intermediatePoints);
                            shortcutNeighbour.getReverse().setIntermediatesPoints(intermediatePointsReverse);

                            intermediatesList.clear();
                        } // if (!intermediatesList.isEmpty()){
                    } // primaryDirection
                } // iterate over all neighbours
            } // if (aNode.isFlag(GNode.FLAG_FIX))
        } // iterate over all nodes
        gGraphTile.getGNodes().removeIf(node -> !node.isFlag(GNode.FLAG_FIX));
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
