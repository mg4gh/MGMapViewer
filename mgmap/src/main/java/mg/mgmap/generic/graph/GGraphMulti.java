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

import android.util.Log;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.basic.NameUtil;
import mg.mgmap.generic.model.PointModelUtil;

import java.util.ArrayList;


/**
 * Realisation of a graph based on multiple tiles (GGraphTile) objects.
 */

public class GGraphMulti extends GGraph {


    private final ArrayList<GGraphTile> gGraphTiles = new ArrayList<>();
    ArrayList<GOverlayNeighbour> overlayNeighbours = new ArrayList<>(); // used for neighbour tile connections and for approaches

    public GGraphMulti(ArrayList<GGraphTile> gGraphTiles){
        for (GGraphTile gGraphTile : gGraphTiles){
            connectGGraphTile(gGraphTile);
        }
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
        for (GGraphTile gGraphTile : gGraphTiles){
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


    private void connectGGraphTile(GGraphTile newGGraphTile){
        for (GGraphTile gGraphTile : gGraphTiles){
            if (gGraphTile.tile.equals(newGGraphTile.tile.getLeft())){
                connectTiles(gGraphTile, newGGraphTile,true);
            }
            if (gGraphTile.tile.equals(newGGraphTile.tile.getRight())){
                connectTiles(newGGraphTile, gGraphTile,true);
            }
            if (gGraphTile.tile.equals(newGGraphTile.tile.getAbove())){
                connectTiles(gGraphTile, newGGraphTile,false);
            }
            if (gGraphTile.tile.equals(newGGraphTile.tile.getBelow())){
                connectTiles(newGGraphTile, gGraphTile,false);
            }
        }
        gGraphTiles.add(newGGraphTile);
    }


    private void connectTiles(GGraphTile gGraphTile1, GGraphTile gGraphTile2,boolean horizontal){ // horizontal true: gGraphTile1 is left, gGraphTile2 is right - false: gGraphTile1 is above, gGraphTile2 is below

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
        ArrayList<GNode> remainingNodes1 = new ArrayList<>(borderNodes1);
        ArrayList<GNode> remainingNodes2 = new ArrayList<>(borderNodes2);
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
            Log.v(MGMapApplication.LABEL, NameUtil.context()+" "+remainingNodes1);
            Log.v(MGMapApplication.LABEL, NameUtil.context()+" "+remainingNodes2);
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
                        Log.d(MGMapApplication.LABEL, NameUtil.context()+" OK, connect: node1 "+node1+" node1neighbour "+node1Neighbour+" node2 "+node2+" node2neighbour "+node2Neighbour);
                        connect(node1Neighbour, node2Neighbour);
                        stillRemainingNodes1.remove(node1);
                        stillRemainingNodes2.remove(node2);
                    }
                }
            }
            remainingNodes1 = stillRemainingNodes1;
            remainingNodes2 = stillRemainingNodes2;
        }
        if ((remainingNodes1.size() > 0) || ((remainingNodes2.size() > 0))){
            Log.d(MGMapApplication.LABEL, NameUtil.context()+" remainings1 "+remainingNodes1);
            Log.d(MGMapApplication.LABEL, NameUtil.context()+" remainings2 "+remainingNodes2);
        }
    }

    private void connect(GNode node1, GNode node2){
        double cost = PointModelUtil.distance(node1, node2)+0.001;
        addNextNeighbour(node1,getLastNeighbour(node1),new GNeighbour(node2,cost));
        addNextNeighbour(node2,getLastNeighbour(node2),new GNeighbour(node1,cost));
    }

    public void createOverlaysForApproach(ApproachModel approach){
        super.getNodes().add(approach.getApproachNode()); // take super.getNodes() to get the node list of the multi graph
        connect(approach.getNode1(), approach.getApproachNode());
        connect(approach.getNode2(), approach.getApproachNode());
    }
}
