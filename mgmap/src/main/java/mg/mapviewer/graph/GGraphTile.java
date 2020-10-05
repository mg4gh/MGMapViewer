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
package mg.mapviewer.graph;

import android.util.Log;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.Way;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.BBox;
import mg.mapviewer.model.MultiPointModel;
import mg.mapviewer.model.MultiPointModelImpl;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.model.WriteablePointModel;
import mg.mapviewer.model.WriteablePointModelImpl;
import mg.mapviewer.util.AltitudeProvider;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PointModelUtil;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents the GGraph object for a particular tile (in terms of Mapsforge). These tile graph objects are kept in a cache for faster access.
 */

public class GGraphTile extends GGraph {

    private static final int CACHE_SIZE = 400;
    private static HashMap<Long,GGraphTile> cache = new HashMap<>();
    private static ArrayList<GGraphTile>  accessList = new ArrayList<>();

    private static final byte ZOOM_LEVEL = 15;
    private static final int TILE_SIZE = 256;

    private static long getKey(int tileX,int tileY){
        long key = tileX;
        return (key<<32) + tileY;
    }

    private static GGraphTile getGGraphTile(MapDataStore mapFile, int tileX, int tileY){
        long key = getKey(tileX,tileY);

        GGraphTile graph = cache.get(key);
        if (graph != null){
            accessList.remove(graph);
        } else {
            Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
            MapReadResult mapReadResult = mapFile.readMapData(tile);        // extractPoints relevant map data
            graph = new GGraphTile(tile);
//            BoundingBox b = tile.getBoundingBox();
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
            int threshold = GGraph.CONNECT_THRESHOLD / 2;
            //all highwas are in the map ... try to correct data ...
            for (int iIdx=0; iIdx<graph.nodes.size(); iIdx++){
                GNode iNode = graph.nodes.get(iIdx);
                for (int nIdx=iIdx+1; nIdx<graph.nodes.size(); nIdx++ ){
                    GNode nNode = graph.nodes.get(nIdx);
                    if (iNode.laMdDiff(nNode) >= threshold) break; // go to next iIdx
                    if ((iNode.laMdDiff(nNode)+iNode.loMdDiff(nNode)) >= threshold ) continue; // goto next mIdx

//                  This doesn't work well for routing hints
//                    graph.addSegment(iNode, nNode);

//                  // Try to simplify the graph by removing node nNode
                    // iterate over al neighbours from nNode
                    GNeighbour nextNeighbour = nNode.getNeighbour();
                    while (nextNeighbour.getNextNeighbour() != null) {
                        nextNeighbour = nextNeighbour.getNextNeighbour();
                        // remove nNode as a Neighbour
                        nextNeighbour.getNeighbourNode().removeNeighbourNode(nNode);
                        graph.addSegment(iNode, nextNeighbour.getNeighbourNode());
                    }
                }
            }
        }

        if (graph != null){
            while (cache.size() >= CACHE_SIZE) {
                GGraphTile old = accessList.remove(0);
                cache.remove(getKey(old.tile.tileX, old.tile.tileY));
                old.setChanged();
                old.notifyObservers();
                Log.d(MGMapApplication.LABEL,NameUtil.context()+ " remove tile x="+old.tile.tileX+" y="+old.tile.tileY+" Cache Size:"+cache.size());
            }
            accessList.add(graph);
            cache.put(key,graph);
            Log.d(MGMapApplication.LABEL,NameUtil.context()+ " Cache Size:"+cache.size()+" accessList Size:"+accessList.size());
        }
        return graph;
    }

    // be careful with this operation, it might crash a routing action
    public static void clearCache(){
        for (long key : cache.keySet()){
            cache.get(key).setChanged();
            cache.get(key).notifyObservers();
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
        return getAddNode(PointModelUtil.roundMD(latitude), PointModelUtil.roundMD(longitude), -1, nodes.size());
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
//        if ((latitude == 49.453486) && (longitude == 8.656784)){
//            Log.i(MGMapApplication.LABEL, NameUtil.context() + " low="+low+" high="+high);
//        }
        if (high - low == 1){
            float hgtAlt = AltitudeProvider.getAltitude(latitude, longitude);
            GNode node = new GNode(latitude, longitude, hgtAlt, 0);
            nodes.add(high, node);
            return node;
            // nothing more to compare, insert a new GNode at high
        } else {
            int mid = (high + low) /2;
            GNode gMid = nodes.get(mid);
            int cmp = PointModelUtil.compareTo(latitude, longitude, gMid.getLat(), gMid.getLon() );
            if (cmp == 0) return gMid;
            if (cmp < 0){
                return getAddNode(latitude, longitude, low, mid);
            } else {
                return getAddNode(latitude, longitude, mid, high);
            }
        }
    }

    @Override
    public ArrayList<GNode> getNodes() {
        return new ArrayList<>(nodes);
    }

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
