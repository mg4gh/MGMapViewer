/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mgmap.graph;

import android.util.Log;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.Way;

import mg.mgmap.MGMapApplication;
import mg.mgmap.model.BBox;
import mg.mgmap.model.MultiPointModel;
import mg.mgmap.model.MultiPointModelImpl;
import mg.mgmap.model.PointModelImpl;
import mg.mgmap.model.WriteablePointModel;
import mg.mgmap.model.WriteablePointModelImpl;
import mg.mgmap.util.AltitudeProvider;
import mg.mgmap.util.LaLo;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.PointModelUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Represents the GGraph object for a particular tile (in terms of Mapsforge). These tile graph objects are kept in a cache for faster access.
 */

public class GGraphTile extends GGraph {

    public static final int CACHE_LIMIT = 1000;

    private static LinkedHashMap<Long, GGraphTile> cache = new LinkedHashMap <Long, GGraphTile>(100, 0.6f, true) {
        @Override
        protected boolean removeEldestEntry(Entry<Long, GGraphTile> eldest) {
            boolean bRes = (size() > CACHE_LIMIT);
            if (bRes){
                GGraphTile old = eldest.getValue();
                old.setChanged();
                old.notifyObservers();
                Log.d(MGMapApplication.LABEL, NameUtil.context() + " remove from cache: tile x=" + old.tile.tileX + " y=" + old.tile.tileY + " Cache Size:" + cache.size());
            }
            return bRes;
        }
    };

    private static final byte ZOOM_LEVEL = 15;
    private static final int TILE_SIZE = 256;

    private static long getKey(int tileX,int tileY){
        long key = tileX;
        return (key<<32) + tileY;
    }

    private static GGraphTile getGGraphTile(MapDataStore mapFile, int tileX, int tileY){
        long key = getKey(tileX,tileY);

        GGraphTile graph = cache.get(key);
        if (graph == null){
            Log.d(MGMapApplication.LABEL, NameUtil.context()+" Load tileX="+tileX+" tileY="+tileY+" ("+cache.size()+")");
            Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
            MapReadResult mapReadResult = mapFile.readMapData(tile);        // extractPoints relevant map data
            graph = new GGraphTile(tile);
            for (Way way : mapReadResult.ways) {
                if (isHighway(way)){

                    graph.addLatLongs( way.latLongs[0]);

                    MultiPointModelImpl mpm = new MultiPointModelImpl();
                    for (LatLong latLong : way.latLongs[0] ){
                        // for points inside the tile use the GNodes as already allocated
                        // for points outside use extra Objects, don't pollute the graph with them
                        if (graph.tbBox.contains(latLong.latitude, latLong.longitude)){
                            mpm.addPoint(graph.getAddNode(latLong.latitude, latLong.longitude));
                        } else {
                            mpm.addPoint(new PointModelImpl(latLong));
                        }
                    }
                    graph.rawWays.add(mpm);
                }
            }
            int latThreshold = LaLo.d2md( PointModelUtil.latitudeDistance(GGraph.CONNECT_THRESHOLD_METER) );
            int lonThreshold = LaLo.d2md( PointModelUtil.longitudeDistance(GGraph.CONNECT_THRESHOLD_METER, tile.getBoundingBox().getCenterPoint().getLatitude()) );
//            Log.v(MGMapApplication.LABEL, NameUtil.context()+" latThreshold="+latThreshold+" lonThreshold="+lonThreshold);
            //all highwas are in the map ... try to correct data ...
            ArrayList<GNode> nodes = graph.getNodes();
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
                    if (iNode.hasNeighbour(nNode))
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

//And this is still not good: (Hollmuth,Heiligkreuzsteinach) there are 2 neighbours at on end ... and one on the other ... and this doesn't work
//                    if (nNode.countNeighbours() != 1) continue; // connect only end points
//                    // Third solution approach: connect only point with exactly 1 neighbour
//                    // Therefore this shouldn't be a Problem for routing hints, since both connected points have now 2 neighbours - so they are no routing points
//                    graph.addSegment(iNode, nNode);

                    int nNeighbours = nNode.countNeighbours();
                    if ((iNeighbours == 1) && (nNeighbours == 1)) { // 1:1 connect -> no routing hint problem
                        graph.addSegment(iNode, nNode);
                        continue;
                    }
                    if (isBorderPoint(graph.tbBox, nNode) || isBorderPoint(graph.tbBox, iNode)) { // border points must be kept for MultiTiles; accept potential routing hint problem
                        graph.addSegment(iNode, nNode);
                        continue;
                    }
                    if ((iNeighbours == 2) && (nNeighbours == 1)) { // 2:1 connect -> might give routing hint problem
                        reduceGraph(graph, iNode, nNode);  // drop nNode; move neighbour form nNode to iNode
                        continue;
                    }
                    if ((iNeighbours == 1) && (nNeighbours == 2)) { // 1:2 connect -> might give routing hint problem
                        reduceGraph(graph, nNode, iNode); // drop iNode; move neighbour form iNode to nNode
                        continue;
                    }
                    // else (n:m) accept routing hint issue
                    graph.addSegment(iNode, nNode);

                }
            }
            cache.put(key,graph);
        }
        return graph;
    }

    static boolean isBorderPoint(BBox bBox, GNode node){
        return ((node.getLat() == bBox.maxLatitude) || (node.getLat() == bBox.minLatitude) ||
                (node.getLon() == bBox.maxLongitude) || (node.getLon() == bBox.minLongitude));
    }

    // reduce Graph by dropping nNode, all Neighbours form nNode will get iNode as a Neighbour
    static void reduceGraph(GGraphTile graph, GNode iNode, GNode nNode){
        // iterate over al neighbours from nNode
        GNeighbour nextNeighbour = nNode.getNeighbour();
        while (nextNeighbour.getNextNeighbour() != null) {
            nextNeighbour = nextNeighbour.getNextNeighbour();
            // remove nNode as a Neighbour
            nextNeighbour.getNeighbourNode().removeNeighbourNode(nNode);
            graph.addSegment(iNode, nextNeighbour.getNeighbourNode());
        }
        graph.getNodes().remove(nNode);
    }

    // be careful with this operation, it might crash a routing action
    public static void clearCache(){
        for (GGraphTile gGraphTile : cache.values()){
            gGraphTile.setChanged();
            gGraphTile.notifyObservers();
        }
        cache.clear();
    }

    private static boolean isHighway(Way way){
        for (Tag tag : way.tags){
            if (tag.key.equals("highway")){
                return true;
            }
        }
        return false;
    }

    public static ArrayList<GGraphTile> getGGraphTileList(MapDataStore mapFile, BBox bBox){

        ArrayList<GGraphTile> tileList = new ArrayList<>();
        try {
            byte ZOOM_LEVEL = 15;
            int tileSize = 256;
            long mapSize = MercatorProjection.getMapSize(ZOOM_LEVEL, tileSize);
            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.minLongitude , mapSize) , ZOOM_LEVEL, tileSize);
            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.maxLongitude , mapSize) , ZOOM_LEVEL, tileSize);
            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.maxLatitude , mapSize) , ZOOM_LEVEL, tileSize); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.minLatitude , mapSize) , ZOOM_LEVEL, tileSize);

            Log.d(MGMapApplication.LABEL, NameUtil.context()+" create GGraphTileList with tileXMin="+tileXMin+" tileYMin="+tileYMin+" and tileXMax="+tileXMax+" tileYMax="+tileYMax+
                    " - total tiles="+ ((tileXMax-tileXMin+1)*(tileYMax-tileYMin+1)));
            for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                    GGraphTile graph = GGraphTile.getGGraphTile(mapFile,tileX,tileY);
                    tileList.add(graph);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tileList;
    }






    private ArrayList<MultiPointModel> rawWays = new ArrayList<>();
    Tile tile;
    BBox tbBox;
    private WriteablePointModel clipRes = new WriteablePointModelImpl();

    private GGraphTile(Tile t){
        this.tile = t;
        tbBox = BBox.fromBoundingBox(tile.getBoundingBox());
    }

    private void addLatLongs(LatLong[] latLongs){


        for (int i=1; i<latLongs.length; i++){
            double lat1 = PointModelUtil.roundMD(latLongs[i-1].latitude);
            double lon1 = PointModelUtil.roundMD(latLongs[i-1].longitude);
            double lat2 = PointModelUtil.roundMD(latLongs[i].latitude);
            double lon2 = PointModelUtil.roundMD(latLongs[i].longitude);

            tbBox.clip(lat1, lon1, lat2, lon2, clipRes);
            lat2 = clipRes.getLat();
            lon2 = clipRes.getLon();
            tbBox.clip(lat2, lon2, lat1, lon1, clipRes);
            lat1 = clipRes.getLat();
            lon1 = clipRes.getLon();
            if (tbBox.contains(lat1, lon1) && tbBox.contains(lat2, lon2)){
                addSegment(lat1, lon1 ,lat2, lon2);
            }


        }
    }

    void addSegment(double lat1, double long1, double lat2, double long2){
        GNode node1 = getAddNode( lat1, long1);
        GNode node2 = getAddNode( lat2, long2);
        addSegment(node1, node2);
    }

    void addSegment(GNode node1, GNode node2){
        double cost = PointModelUtil.distance(node1, node2);
        node1.addNeighbour(new GNeighbour(node2, cost));
        node2.addNeighbour(new GNeighbour(node1, cost));
    }

    private GNode getAddNode(double latitude, double longitude){
        return getAddNode(PointModelUtil.roundMD(latitude), PointModelUtil.roundMD(longitude), -1, getNodes().size());
    }

    /**
     * !!! GNode object in ArrayList nodes are stored sorted to retrieve already existing points fast !!!
     * This is only done in GGraphTile during setup of the graph.
     * @param latitude
     * @param longitude
     * @param low nodes.get(low) is strict less (or low is -1)
     * @param high nodes.get(high) is strict greater than (or high is nodes.size() )
     * @return
     */
    private GNode getAddNode(double latitude, double longitude, int low, int high){
        if (high - low == 1){
            float hgtAlt = AltitudeProvider.getAltitude(latitude, longitude);
            GNode node = new GNode(latitude, longitude, hgtAlt, 0);
            getNodes().add(high, node);
            return node;
            // nothing more to compare, insert a new GNode at high
        } else {
            int mid = (high + low) /2;
            GNode gMid = getNodes().get(mid);
            int cmp = PointModelUtil.compareTo(latitude, longitude, gMid.getLat(), gMid.getLon() );
            if (cmp == 0) return gMid;
            if (cmp < 0){
                return getAddNode(latitude, longitude, low, mid);
            } else {
                return getAddNode(latitude, longitude, mid, high);
            }
        }
    }

//    @Override
//    public ArrayList<GNode> getNodes() {
//        return new ArrayList<>(nodes);
//    }

    public BBox getTileBBox(){
        return tbBox;
    }

    public ArrayList<MultiPointModel> getRawWays() {
        return rawWays;
    }

    @Override
    public String toString() {
        return tbBox.toString();
    }

}
