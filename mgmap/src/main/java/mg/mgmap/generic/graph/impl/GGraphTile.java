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

import androidx.annotation.NonNull;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;

import java.util.ArrayList;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;

/**
 * Represents the GGraph object for a particular tile.
 */

public class GGraphTile extends GGraph {

    final ElevationProvider elevationProvider;

    private final ArrayList<MultiPointModel> rawWays = new ArrayList<>();
    final Tile tile;
    final int tileIdx;
    final BBox bBox;
    private final WriteablePointModel clipRes = new WriteablePointModelImpl();
    private final WriteablePointModel hgtTemp = new WriteablePointModelImpl();
    final GGraphTile[] neighbourTiles = new GGraphTile[GNode.BORDER_NODE_WEST+1];//use BORDER constants as index, although some entries stay always null
    boolean used = false; // used for cache - do not delete from cache
    long accessTime = 0; // used for cache
    public ArrayList<Long> timestamps = new ArrayList<>();

    public GGraphTile(ElevationProvider elevationProvider, Tile tile){
        this.elevationProvider = elevationProvider;
        this.tile = tile;
        this.tileIdx = GGraphTileFactory.getKey(getTileX(),getTileY());
        bBox = BBox.fromBoundingBox(this.tile.getBoundingBox());
    }

    public void addLatLongs(WayAttributs wayAttributs, LatLong[] latLongs){
        for (int i=1; i<latLongs.length; i++){
            double lat1 = PointModelUtil.roundMD(latLongs[i-1].latitude);
            double lon1 = PointModelUtil.roundMD(latLongs[i-1].longitude);
            double lat2 = PointModelUtil.roundMD(latLongs[i].latitude);
            double lon2 = PointModelUtil.roundMD(latLongs[i].longitude);

            bBox.clip(lat1, lon1, lat2, lon2, clipRes); // clipRes contains the clip result
            lat2 = clipRes.getLat();
            lon2 = clipRes.getLon();
            bBox.clip(lat2, lon2, lat1, lon1, clipRes); // clipRes contains the clip result
            lat1 = clipRes.getLat();
            lon1 = clipRes.getLon();
            if (bBox.contains(lat1, lon1) && bBox.contains(lat2, lon2)){
                addSegment(wayAttributs, lat1, lon1 ,lat2, lon2);
            }
        }
    }

    void addSegment(WayAttributs wayAttributs, double lat1, double long1, double lat2, double long2){
        GNode node1 = getAddNode( lat1, long1);
        GNode node2 = getAddNode( lat2, long2);
        addSegment(wayAttributs, node1, node2);
    }

    void addSegment(WayAttributs wayAttributs, GNode node1, GNode node2){
        GNeighbour n12 = new GNeighbour(node2, wayAttributs );
        GNeighbour n21 = new GNeighbour(node1, wayAttributs ).setPrimaryDirection(false);
        n12.setReverse(n21);
        n21.setReverse(n12);
        addNeighbour(node1, n12);
        addNeighbour(node2, n21);
        float distance = (float)PointModelUtil.distance(node1, node2);
        n12.setDistance(distance);
        n21.setDistance(distance);
    }

    public GNode getNode(double latitude, double longitude){
        return getAddNode(latitude, longitude, false);
    }
    GNode getAddNode(double latitude, double longitude){
        return getAddNode(latitude, longitude, true);
    }
    GNode getAddNode(double latitude, double longitude, boolean allowAdd){
        return getAddNode(PointModelUtil.roundMD(latitude), PointModelUtil.roundMD(longitude), -1, getGNodes().size(), allowAdd);
    }

    /**
     * !!! GNode object in ArrayList nodes are stored sorted to retrieve already existing points fast !!!
     * This is only done in GGraphTile during setup of the graph.
     * @param latitude latitude of the point to be stored
     * @param longitude longitude of the point to be stored
     * @param low nodes.get(low) is strict less (or low is -1)
     * @param high nodes.get(high) is strict greater than (or high is nodes.size() )
     * @param allowAdd if set, then add a new node, if there is none with these lat/lon values (false is used for pure search)
     * @return GNode with latitude an longitude, either already existing point or newly created one
     */
    private GNode getAddNode(double latitude, double longitude, int low, int high, boolean allowAdd){
        if (high - low == 1){ // nothing more to compare, insert a new GNode at high index
            if (allowAdd){
                // use hgtTemp as parameter by reference to ElevationProvider, that return the hgtEle and the hgtEleAcc values via this reference - Since GNode is no WritablePointModel, it cannot be used directly for this.
                hgtTemp.setLat(latitude);
                hgtTemp.setLon(longitude);
                elevationProvider.setElevation(hgtTemp);
                GNode node = new GNode(latitude, longitude, hgtTemp.getEle(), hgtTemp.getEleAcc());
                if (longitude == bBox.minLongitude) node.borderNode |= GNode.BORDER_NODE_WEST;
                if (longitude == bBox.maxLongitude) node.borderNode |= GNode.BORDER_NODE_EAST;
                if (latitude == bBox.minLatitude)  node.borderNode |= GNode.BORDER_NODE_SOUTH;
                if (latitude == bBox.maxLatitude)  node.borderNode |= GNode.BORDER_NODE_NORTH;
                node.tileIdx = tileIdx;
                getGNodes().add(high, node);
                return node;
            } else {
                return null;
            }
        } else {
            int mid = (high + low) /2;
            GNode gMid = getGNodes().get(mid);
            int cmp = PointModelUtil.compareTo(latitude, longitude, gMid.getLat(), gMid.getLon() );
            if (cmp == 0) return gMid;
            if (cmp < 0){
                return getAddNode(latitude, longitude, low, mid, allowAdd);
            } else {
                return getAddNode(latitude, longitude, mid, high, allowAdd);
            }
        }
    }

    public BBox getBBox(){
        return bBox;
    }

    public ArrayList<MultiPointModel> getRawWays() {
        return rawWays;
    }

    public int getTileX(){
        return tile.tileX;
    }
    public int getTileY(){
        return tile.tileY;
    }

    void resetNodeRefs(){
        for (GNode node : getGNodes()){
            node.resetNodeRefs();
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "GGraphTile-("+getTileX()+","+getTileY()+")";
    }

    @Override
    public ArrayList<PointModel> getNeighbours(PointModel pointModel, ArrayList<PointModel> neighbourPoints) {
        if (this.bBox.contains(pointModel)){
            GNode gNode = getNode(pointModel.getLat(), pointModel.getLon());
            if (gNode != null){
                GNeighbour neighbour = null;
                while ((neighbour = (neighbour==null)?gNode.getNeighbour():neighbour.getNextNeighbour()) != null){
                    neighbourPoints.add(new PointModelImpl(neighbour.getPoint()));
                }
            }
        }
        return neighbourPoints;
    }

    @Override
    public int getTileCount() {
        return 1;
    }
}
