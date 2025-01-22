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

import java.util.ArrayList;

import mg.mgmap.generic.model.PointModelUtil;

/**
 * A basic graph implementation based on GNode and GNeighbour.
 */

public class GGraph{

    public static final double CONNECT_THRESHOLD_METER = 0.5; // means 0.5m

    private final ArrayList<GNode> nodes = new ArrayList<>();

    public ArrayList<GNode> getNodes(){
        return nodes;
    }

    /**
     * Determine neighbour via graph - allows redefinition for specific GGraph implementations
     * @param node node for that the neighbours can be iterated with this method
     * @param neighbour last neighbour of node in the iteration of neighbours
     * @return next neighbour in the graph
     */
    public GNeighbour getNextNeighbour(GNode node, GNeighbour neighbour){
        return neighbour.getNextNeighbour();
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
        if (secondNeighbour.getNextNeighbour() != null) return null; // found third neighbour
        if (firstNeighbour.getNeighbourNode() == neighbourNode){
            return secondNeighbour.getNeighbourNode();
        }
        if (secondNeighbour.getNeighbourNode() == neighbourNode){
            return firstNeighbour.getNeighbourNode();
        }
        return null; // should not happen (given neighbourNode is not neighbour to node
    }

    /* returns oppositeNeighbour - means also the node has exactly 2 neighbours, where givenNeighbour is one and the returned value is the other neighbour.
        returns null, if there is not exactly one other neighbour.
    */
    public GNeighbour oppositeNeighbour(GNode node, GNeighbour givenNeighbour){
        GNeighbour neighbour = node.getNeighbour();
        GNeighbour firstNeighbour = getNextNeighbour(node, neighbour);
        if (firstNeighbour == null) return null; // found no neighbour
        GNeighbour secondNeighbour = getNextNeighbour(node, firstNeighbour);
        if (secondNeighbour == null) return null; // found just one neighbour
        if (secondNeighbour.getNextNeighbour() != null) return null; // found third neighbour
        if (firstNeighbour == givenNeighbour){
            return secondNeighbour;
        }
        if (secondNeighbour == givenNeighbour){
            return firstNeighbour;
        }
        return null; // should not happen (given neighbourNode is not neighbour to node
    }

    public ArrayList<GNode> segmentNodes(GNode node1, GNode node2, int closeThreshold, boolean limitToTile){
        ArrayList<GNode> segmentNodes = new ArrayList<>();
        segmentNodes.add(node1);
        segmentNodes.add(node2);
        GNode nodeA = node1;
        GNode nodeB = node2;
        GNode nodeC;
        double distance = 0;
        while ( (nodeC = oppositeNode(nodeB, nodeA)) != null){
            if (segmentNodes.contains(nodeC)) break;
            if (limitToTile && !GNode.sameTile(nodeC,nodeA)) break;
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
            if (limitToTile && !GNode.sameTile(nodeC,nodeA)) break;
            nodeA = nodeB;
            nodeB = nodeC;
            segmentNodes.add(0, nodeC);
            distance += PointModelUtil.distance(nodeA, nodeB);
            if (distance >= closeThreshold) break;
        }
        return segmentNodes;
    }

}
