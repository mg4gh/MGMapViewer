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

import java.util.ArrayList;
import java.util.Observable;
import java.util.TreeSet;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.BBox;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PointModelUtil;

/**
 * A basic graph implementation based on GNode and GNeighbour.
 */

public class GGraph extends Observable {

    public static final int CONNECT_THRESHOLD = 8;

    ArrayList<GNode> nodes = new ArrayList<>();

    ArrayList<GOverlayNeighbour> overlayNeighbours = new ArrayList<>();

    public ArrayList<GNode> getNodes(){
        return nodes;
    }

    /**
     * Determine neighbour via graph to allow redefinition for specific GGraph implementations
     * @param node
     * @param neighbour
     * @return next neighbour in the graph
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

    void resetNodeRefs(){
        for (GNode node : getNodes()){
            node.setNodeRef(null);
        }
    }

    public GNeighbour getLastNeighbour(GNode node) {
        GNeighbour neighbour = node.getNeighbour();
        GNeighbour lastNeighbour = neighbour;
        while ((neighbour = getNextNeighbour(node, neighbour)) != null) {
            lastNeighbour = neighbour;
        }
        return lastNeighbour;
    }

    /* returns oppositeNode - means the node has exactly 2 neighbours, where neighbourNode is one and the returned value is the other neighbour.
        returns null, if there is not exactly one other neighbour.
    */
    public GNode oppositeNode(GNode node, GNode neighbourNode){
        GNeighbour neighbour = node.getNeighbour();
        GNeighbour firstNeighbour = getNextNeighbour(node, neighbour);
        if (firstNeighbour == null) return null; // found no neighbour
        GNeighbour secondNeighbour = getNextNeighbour(node, firstNeighbour);
        if (secondNeighbour == null) return null; // found just one neighbour
        if (getNextNeighbour(node, secondNeighbour) != null) return null; // found third neighbour
        if (firstNeighbour.getNeighbourNode() == neighbourNode){
            return secondNeighbour.getNeighbourNode();
        }
        if (secondNeighbour.getNeighbourNode() == neighbourNode){
            return firstNeighbour.getNeighbourNode();
        }
        return null; // should not happen (given neighbourNode is not neighbour to node
    }


    public ArrayList<GNode> segmentNodes(GNode node1, GNode node2, int closeThreshold){
        ArrayList<GNode> segmentNodes = new ArrayList<>();
        segmentNodes.add(node1);
        segmentNodes.add(node2);
        GNode nodeA = node1;
        GNode nodeB = node2;
        GNode nodeC;
        double distance = 0;
        while ( (nodeC = oppositeNode(nodeB, nodeA)) != null){
            if (segmentNodes.contains(nodeC)) break;
            nodeA = nodeB;
            nodeB = nodeC;
            segmentNodes.add(nodeC);
            distance += PointModelUtil.distance(nodeA, nodeB);
            if (distance >= closeThreshold) break;
        }
        nodeA = node2;
        nodeB = node1;
        distance = 0;
        while ( (nodeC = oppositeNode(nodeB, nodeA)) != null){
            if (segmentNodes.contains(nodeC)) break;
            nodeA = nodeB;
            nodeB = nodeC;
            segmentNodes.add(0, nodeC);
            distance += PointModelUtil.distance(nodeA, nodeB);
            if (distance >= closeThreshold) break;
        }
        return segmentNodes;
    }
    
    public void createOverlaysForApproach(ApproachModel approach){
        nodes.add(approach.getApproachNode());
        createOverlaysForApproach(approach.getNode1(), approach.getApproachNode());
        createOverlaysForApproach(approach.getNode2(), approach.getApproachNode());
    }
    private void createOverlaysForApproach(GNode node1, GNode node2){
        double cost = PointModelUtil.distance(node1,node2)*1.000001+0.000001;
        addNextNeighbour(node1, getLastNeighbour(node1), new GNeighbour(node2,cost));
        addNextNeighbour(node2, getLastNeighbour(node2), new GNeighbour(node1,cost));
    }

}
