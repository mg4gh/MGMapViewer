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

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.model.PointModelUtil;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.TreeMap;


/**
 * Realisation of a graph based on multiple tiles (GGraphTile) objects.
 */

public class GGraphMulti extends GGraph {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final GGraphTileFactory gGraphTileFactory;
    private final TreeMap<Integer, GGraphTile> gGraphTileMap = new TreeMap<>();
    ArrayList<GOverlayNeighbour> overlayNeighbours = new ArrayList<>(); // used for neighbour tile connections and for approaches

    public GGraphMulti(GGraphTileFactory gGraphTileFactory, ArrayList<GGraphTile> gGraphTiles){
        this.gGraphTileFactory = gGraphTileFactory;
        for (GGraphTile gGraphTile : gGraphTiles){
            gGraphTileMap.put(GGraphTileFactory.getKey(gGraphTile.tile.tileX,gGraphTile.tile.tileY), gGraphTile);
        }
    }

    public int getTileCount(){
        return gGraphTileMap.values().size();
    }

    /**
     * Redefines getNodes() implementation og GGraph, which simply returns the ArrayList of its nodes.
     * Here we get a new ArrayList Object with all nodes from each included GGraphTile plus additionally the
     * nodes that were created due to connecting neighbour GGraphTile instances, plus the additional
     * overlay nodes from approaches for start and end point of a routing access.
     * @return all node ot the multi graph
     */
    @Override
    public ArrayList<GNode> getNodes() {
        ArrayList<GNode> nodes = new ArrayList<>( super.getNodes() );
        for (GGraphTile gGraphTile : gGraphTileMap.values()){
            nodes.addAll( gGraphTile.getNodes() );
        }
        return nodes;
    }

    /**
     * Redefines simple implementation of GGraph under consideration of overlayNeighbours
     * @param node node for that the neighbours can be iterated with this method
     * @param neighbour last neighbour of node in the iteration of neighbours
     * @return next neighbour
     */
    public GNeighbour getNextNeighbour(GNode node, GNeighbour neighbour){
        GNeighbour res = neighbour.getNextNeighbour();
        if (res == null){
            for (GOverlayNeighbour overlayNeighbour : overlayNeighbours){
                if ((overlayNeighbour.node == node) && (overlayNeighbour.neighbour == neighbour)){
                    res = overlayNeighbour.nextNeighbour;
                }
            }
        }
        return res;
    }

    public void addNextNeighbour(GNode node, GNeighbour neighbour, GNeighbour nextNeighbour){
        overlayNeighbours.add(new GOverlayNeighbour(node, neighbour, nextNeighbour));
    }

    void preNodeRelax(RoutingProfile routingProfile, GNode node){
        if ((node.borderNode != 0) && (gGraphTileMap.size() < GGraphTileFactory.CACHE_LIMIT)){ // add lazy expansion of GGraphMulti
            GGraphTile gGraphTile = gGraphTileMap.get(node.tileIdx);
            assert(gGraphTile != null);
            checkGGraphTileNeighbour(routingProfile,node,GNode.BORDER_NODE_WEST, gGraphTile.getTileX()-1, gGraphTile.getTileY());
            checkGGraphTileNeighbour(routingProfile,node,GNode.BORDER_NODE_NORTH, gGraphTile.getTileX(), gGraphTile.getTileY()-1);
            checkGGraphTileNeighbour(routingProfile,node,GNode.BORDER_NODE_EAST, gGraphTile.getTileX()+1, gGraphTile.getTileY());
            checkGGraphTileNeighbour(routingProfile,node,GNode.BORDER_NODE_SOUTH, gGraphTile.getTileX(), gGraphTile.getTileY()+1);
        }
    }

    private void checkGGraphTileNeighbour(RoutingProfile routingProfile, GNode node, byte border, int tileX, int tileY){
        if ( (node.borderNode & border) != 0 ){
            Integer neighbourIdx = GGraphTileFactory.getKey(tileX, tileY);
            GGraphTile gGraphTileNeighbour = gGraphTileMap.get(neighbourIdx);
            if (gGraphTileNeighbour == null){
                gGraphTileNeighbour = gGraphTileFactory.getGGraphTile(routingProfile, tileX, tileY);
                gGraphTileMap.put(neighbourIdx, gGraphTileNeighbour);
                gGraphTileNeighbour.resetNodeRefs();
                connectGGraphTile(gGraphTileNeighbour);
            }
        }

    }

    private void connectGGraphTile(GGraphTile newGGraphTile){
        connectTiles(gGraphTileMap.get(GGraphTileFactory.getKey(newGGraphTile.getTileX()-1,newGGraphTile.getTileY())), newGGraphTile, true);
        connectTiles(newGGraphTile, gGraphTileMap.get(GGraphTileFactory.getKey(newGGraphTile.getTileX()+1,newGGraphTile.getTileY())), true);
        connectTiles(gGraphTileMap.get(GGraphTileFactory.getKey(newGGraphTile.getTileX(),newGGraphTile.getTileY()-1)), newGGraphTile, false);
        connectTiles(newGGraphTile, gGraphTileMap.get(GGraphTileFactory.getKey(newGGraphTile.getTileX(),newGGraphTile.getTileY()+1)), false);
    }

    private void connectTiles(GGraphTile gGraphTile1, GGraphTile gGraphTile2,boolean horizontal){ // horizontal true: gGraphTile1 is left, gGraphTile2 is right - false: gGraphTile1 is above, gGraphTile2 is below
        if ((gGraphTile1 == null) || (gGraphTile2 == null)) return;

        double connectAt = (horizontal)?gGraphTile1.tbBox.maxLongitude:gGraphTile1.tbBox.minLatitude;
        if (horizontal? (connectAt!=gGraphTile2.tbBox.minLongitude):(connectAt!=gGraphTile2.tbBox.maxLatitude)){
            throw new IllegalArgumentException("cannot connectHorizontal Tiles with BB " + gGraphTile1.tbBox +" and "+gGraphTile2.tbBox);
        }
        ArrayList<GNode> borderNodes1 = new ArrayList<>();
        for (GNode node : gGraphTile1.getNodes()){
            if ((horizontal)?(node.getLon() == connectAt):(node.getLat() == connectAt)){
                borderNodes1.add(node);
            }
        }
        ArrayList<GNode> borderNodes2 = new ArrayList<>();
        for (GNode node : gGraphTile2.getNodes()){
            if ((horizontal)?(node.getLon() == connectAt):(node.getLat() == connectAt)){
                borderNodes2.add(node);
            }
        }
        final ArrayList<GNode> remainingNodes1 = new ArrayList<>(borderNodes1);
        final ArrayList<GNode> remainingNodes2 = new ArrayList<>(borderNodes2);
        double threshold = (horizontal)?PointModelUtil.latitudeDistance(CONNECT_THRESHOLD_METER):PointModelUtil.longitudeDistance(CONNECT_THRESHOLD_METER, connectAt);
        for (GNode node1 : borderNodes1){
            for (GNode node2 : borderNodes2){
                if ( (horizontal?Math.abs( node1.getLat() - node2.getLat() ):Math.abs( node1.getLon() - node2.getLon() )) <= threshold){ //distance less than 0.5m -> connect nodes
                    connect(node1, node2);
                    remainingNodes1.remove(node1);
                    remainingNodes2.remove(node2);
                }
            }
        }
        if ((remainingNodes1.size() > 0) && ((remainingNodes2.size() > 0))){
            ArrayList<GNode> stillRemainingNodes1 = new ArrayList<>(remainingNodes1);
            ArrayList<GNode> stillRemainingNodes2 = new ArrayList<>(remainingNodes2);
            mgLog.v(remainingNodes1::toString);
            mgLog.v(remainingNodes2::toString);
            for (GNode node1 : remainingNodes1){
                for (GNode node2 : remainingNodes2){
                    if ((node1.countNeighbours() == 1) && (node2.countNeighbours() == 1) && (PointModelUtil.distance(node1,node2)<CONNECT_THRESHOLD_METER*20)){
                        GNode node1Neighbour = node1.getNeighbour().getNextNeighbour().getNeighbourNode();
                        GNode node2Neighbour = node2.getNeighbour().getNextNeighbour().getNeighbourNode();
                        WriteablePointModel approchPoint = new WriteablePointModelImpl();
                        if (!PointModelUtil.findApproach(node1,node1Neighbour,node2Neighbour,approchPoint,0)) continue; // approach not found try next ponits
                        if (PointModelUtil.distance(approchPoint,node1) > CONNECT_THRESHOLD_METER) continue;
                        if (!PointModelUtil.findApproach(node2,node1Neighbour,node2Neighbour,approchPoint,0)) continue; // approach not found try next ponits
                        if (PointModelUtil.distance(approchPoint,node2) > CONNECT_THRESHOLD_METER) continue;
                        mgLog.d(()->"OK, connect: node1 " + node1 + " node1neighbour " + node1Neighbour + " node2 " + node2 + " node2neighbour " + node2Neighbour);
                        connect(node1Neighbour, node2Neighbour);
                        stillRemainingNodes1.remove(node1);
                        stillRemainingNodes2.remove(node2);
                    }
                }
            }
            remainingNodes1.clear();
            remainingNodes1.addAll(stillRemainingNodes1);
            remainingNodes2.clear();
            remainingNodes2.addAll(stillRemainingNodes2);
        }
        if ((remainingNodes1.size() > 0) || ((remainingNodes2.size() > 0))){
            mgLog.d(()->"remainings1 " + remainingNodes1);
            mgLog.d(()->"remainings2 " + remainingNodes2);
        }
    }

    private void connect(GNode node1, GNode node2){
//        double cost = PointModelUtil.distance(node1, node2)+0.001;
        addNextNeighbour(node1,getLastNeighbour(node1),new GNeighbour(node2,GGraphTileFactory.defaultWayAttributes));
        addNextNeighbour(node2,getLastNeighbour(node2),new GNeighbour(node1,GGraphTileFactory.defaultWayAttributes));
    }

    public void createOverlaysForApproach(ApproachModel approach){
        super.getNodes().add(approach.getApproachNode()); // take super.getNodes() to get the node list of the multi graph
        connect(approach.getNode1(), approach.getApproachNode());
        connect(approach.getNode2(), approach.getApproachNode());
    }
}
