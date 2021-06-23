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

import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.application.util.AltitudeProvider;
import mg.mgmap.generic.model.PointModelUtil;

import java.util.ArrayList;

/**
 * Represents the GGraph object for a particular tile.
 */

public class GGraphTile extends GGraph {

    AltitudeProvider altitudeProvider;

    private final ArrayList<MultiPointModel> rawWays = new ArrayList<>();
    final Tile tile;
    final BBox tbBox;
    private final WriteablePointModel clipRes = new WriteablePointModelImpl();

    GGraphTile(AltitudeProvider altitudeProvider, Tile tile){
        this.altitudeProvider = altitudeProvider;
        this.tile = tile;
        tbBox = BBox.fromBoundingBox(this.tile.getBoundingBox());
    }

    void addLatLongs(LatLong[] latLongs){
        for (int i=1; i<latLongs.length; i++){
            double lat1 = PointModelUtil.roundMD(latLongs[i-1].latitude);
            double lon1 = PointModelUtil.roundMD(latLongs[i-1].longitude);
            double lat2 = PointModelUtil.roundMD(latLongs[i].latitude);
            double lon2 = PointModelUtil.roundMD(latLongs[i].longitude);

            tbBox.clip(lat1, lon1, lat2, lon2, clipRes); // clipRes contains the clip result
            lat2 = clipRes.getLat();
            lon2 = clipRes.getLon();
            tbBox.clip(lat2, lon2, lat1, lon1, clipRes); // clipRes contains the clip result
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
        double cost = PointModelUtil.distance(node1, node2); //replace cost calculation with cost provider (Need access to CostProvoder2)
        node1.addNeighbour(new GNeighbour(node2, cost));
        node2.addNeighbour(new GNeighbour(node1, cost));
    }

    public GNode getNode(double latitude, double longitude){
        return getAddNode(latitude, longitude, false);
    }
    GNode getAddNode(double latitude, double longitude){
        return getAddNode(latitude, longitude, true);
    }
    GNode getAddNode(double latitude, double longitude, boolean allowAdd){
        return getAddNode(PointModelUtil.roundMD(latitude), PointModelUtil.roundMD(longitude), -1, getNodes().size(), allowAdd);
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
                float hgtAlt = altitudeProvider.getAltitude(latitude, longitude);
                GNode node = new GNode(latitude, longitude, hgtAlt, 0);
                getNodes().add(high, node);
                return node;
            } else {
                return null;
            }
        } else {
            int mid = (high + low) /2;
            GNode gMid = getNodes().get(mid);
            int cmp = PointModelUtil.compareTo(latitude, longitude, gMid.getLat(), gMid.getLon() );
            if (cmp == 0) return gMid;
            if (cmp < 0){
                return getAddNode(latitude, longitude, low, mid, allowAdd);
            } else {
                return getAddNode(latitude, longitude, mid, high, allowAdd);
            }
        }
    }

    public BBox getTileBBox(){
        return tbBox;
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

    @Override
    public String toString() {
        return "GGraphTile-"+tbBox.toString();
    }
}
