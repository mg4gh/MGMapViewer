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
package mg.mgmap.generic.graph;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;

import org.mapsforge.map.datastore.Way;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.WayProvider;

public class GGraphTileFactory {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final static int CACHE_LIMIT = 1000;
    private final byte ZOOM_LEVEL = 15;
    private final int TILE_SIZE = 256;

    static int getKey(int tileX,int tileY){
        return ( tileX <<16) + tileY;
    }

    private WayProvider wayProvider = null;
    private ElevationProvider elevationProvider = null;
    private LinkedHashMap<Integer, GGraphTile> cache = null;

    public GGraphTileFactory(){}

    public GGraphTileFactory onCreate(WayProvider wayProvider, ElevationProvider elevationProvider){
        this.wayProvider = wayProvider;
        this.elevationProvider = elevationProvider;

        cache = new LinkedHashMap<>(100, 0.6f, true) {
            @Override
            protected boolean removeEldestEntry(Entry<Integer, GGraphTile> eldest) {
                boolean bRes = (size() > CACHE_LIMIT);
                if (bRes) {
                    GGraphTile old = eldest.getValue();
                    mgLog.d(() -> "remove from cache: tile x=" + old.tile.tileX + " y=" + old.tile.tileY + " Cache Size:" + cache.size());
                }
                return bRes;
            }
        };
        return this;
    }

    public void onDestroy(){
        wayProvider = null;
        cache = null;
    }

    public void resetCosts(){
        for (GGraphTile graph : cache.values()){
             for (GNode node : graph.getNodes()){
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
            e.printStackTrace();
        }
        return tileList;
    }

    public ApproachModel validateApproachModel(ApproachModel am){
        GGraphTile gGraphTile = getGGraphTile(am.getTileX(), am.getTileY());
        am.setNode1( gGraphTile.getNode(am.getNode1().getLat(),am.getNode1().getLon()) );
        am.setNode2( gGraphTile.getNode(am.getNode2().getLat(),am.getNode2().getLon()) );
        if (am.getNode1() == null) mgLog.e("node1==null ->rework failed");
        if (am.getNode2() == null) mgLog.e("node2==null ->rework failed");
        return am;
    }

    public ElevationProvider getElevationProvider() {
        return elevationProvider;
    }

    @SuppressWarnings("CommentedOutCode")
    public GGraphTile getGGraphTile(int tileX, int tileY){
        int key = getKey(tileX,tileY);

        GGraphTile gGraphTile = cache.get(key);
        if (gGraphTile == null){
            mgLog.d(()->"Load tileX=" + tileX + " tileY=" + tileY + " (" + cache.size() + ")");
            Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
            gGraphTile = new GGraphTile(elevationProvider, tile);
            for (Way way : wayProvider.getWays(tile)) {
                if (wayProvider.isHighway(way)){

                    WayAttributs wayAttributs = new WayAttributs(way);
                    gGraphTile.addLatLongs( wayAttributs, way.latLongs[0]);

                    // now setup rawWays
                    MultiPointModelImpl mpm = new MultiPointModelImpl();
                    for (LatLong latLong : way.latLongs[0] ){
                        // for points inside the tile use the GNodes as already allocated
                        // for points outside use extra Objects, don't pollute the graph with them
                        if (gGraphTile.tbBox.contains(latLong.latitude, latLong.longitude)){
                            mpm.addPoint(gGraphTile.getAddNode(latLong.latitude, latLong.longitude));
                        } else {
                            mpm.addPoint(new PointModelImpl(latLong));
                        }
                    }
                    gGraphTile.getRawWays().add(mpm);
                }
            }
            int latThreshold = LaLo.d2md( PointModelUtil.latitudeDistance(GGraph.CONNECT_THRESHOLD_METER) );
            int lonThreshold = LaLo.d2md( PointModelUtil.longitudeDistance(GGraph.CONNECT_THRESHOLD_METER, tile.getBoundingBox().getCenterPoint().getLatitude()) );
//            Log.v(MGMapApplication.LABEL, NameUtil.context()+" latThreshold="+latThreshold+" lonThreshold="+lonThreshold);
            //all highways are in the map ... try to correct data ...
            ArrayList<GNode> nodes = gGraphTile.getNodes();
            for (int iIdx=0; iIdx<nodes.size(); iIdx++){
                GNode iNode = nodes.get(iIdx);
                int iNeighbours = iNode.countNeighbours();
                for (int nIdx=iIdx+1; nIdx<nodes.size(); nIdx++ ) {
                    GNode nNode = nodes.get(nIdx);
                    if (iNode.laMdDiff(nNode) >= latThreshold) break; // go to next iIdx
                    if (iNode.loMdDiff(nNode) >= lonThreshold)
                        continue; // goto next mIdx
                    if (PointModelUtil.distance(iNode, nNode) > GGraph.CONNECT_THRESHOLD_METER)
                        continue;
                    if (iNode.getNeighbour(nNode)!=null)
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

                    int nNeighbours = nNode.countNeighbours();
                    if ((iNeighbours == 1) && (nNeighbours == 1)) { // 1:1 connect -> no routing hint problem
                        gGraphTile.addSegment(null,iNode, nNode);
                        continue;
                    }
                    if (isBorderPoint(gGraphTile.tbBox, nNode) || isBorderPoint(gGraphTile.tbBox, iNode)) { // border points must be kept for MultiTiles; accept potential routing hint problem
                        gGraphTile.addSegment(null,iNode, nNode);
                        continue;
                    }
                    if ((iNeighbours == 2) && (nNeighbours == 1)) { // 2:1 connect -> might give routing hint problem
                        reduceGraph(gGraphTile, iNode, nNode);  // drop nNode; move neighbour form nNode to iNode
                        continue;
                    }
                    if ((iNeighbours == 1) && (nNeighbours == 2)) { // 1:2 connect -> might give routing hint problem
                        reduceGraph(gGraphTile, nNode, iNode); // drop iNode; move neighbour form iNode to nNode
                        continue;
                    }
                    // else (n:m) accept routing hint issue
                    gGraphTile.addSegment(null,iNode, nNode);

                }
            }
            cache.put(key,gGraphTile);
        }
        return gGraphTile;
    }

    private boolean isBorderPoint(BBox bBox, GNode node){
        return ((node.getLat() == bBox.maxLatitude) || (node.getLat() == bBox.minLatitude) ||
                (node.getLon() == bBox.maxLongitude) || (node.getLon() == bBox.minLongitude));
    }

    // reduce Graph by dropping nNode, all Neighbours form nNode will get iNode as a Neighbour
    private void reduceGraph(GGraphTile graph, GNode iNode, GNode nNode){
        // iterate over al neighbours from nNode
        GNeighbour nextNeighbour = nNode.getNeighbour();
        while (nextNeighbour.getNextNeighbour() != null) {
            nextNeighbour = nextNeighbour.getNextNeighbour();
            // remove nNode as a Neighbour
            nextNeighbour.getNeighbourNode().removeNeighbourNode(nNode);
            graph.addSegment(nextNeighbour.getWayAttributs(),iNode, nextNeighbour.getNeighbourNode());
        }
        graph.getNodes().remove(nNode);
    }

}
