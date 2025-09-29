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

import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.PointNeighbour;

/**
 * A basic graph implementation based on GNode and GNeighbour.
 */

public abstract class GGraph implements Graph {

    public static final double CONNECT_THRESHOLD_METER = 0.5; // means 0.5m

    private final ArrayList<GNode> gNodes = new ArrayList<>();

    public ArrayList<GNode> getGNodes(){
        return gNodes;
    }

    /**
     * Determine neighbour via graph - allows redefinition for specific GGraph implementations
     * @param node node for that the neighbours can be iterated with this method
     * @param neighbour last neighbour of node in the iteration of neighbours
     * @return next neighbour in the graph
     */
    @SuppressWarnings("unused")
    public PointNeighbour getNextNeighbour(PointModel node, PointNeighbour neighbour){
        if (node instanceof GNode gNode){
            return getNextNeighbour(gNode, (GNeighbour)neighbour);
        }
        return null;
    }
    @SuppressWarnings("unused")
    public GNeighbour getNextNeighbour(GNode node, GNeighbour neighbour){
        if (neighbour != null){
            return neighbour.getNextNeighbour();
        } else {
            return node.getNeighbour();
        }
    }

    GNeighbour getNeighbour(GNode node, GNode neighbourNode){
        GNeighbour nextNeighbour = node.getNeighbour();
        while (nextNeighbour != null) {
            if (nextNeighbour.getNeighbourNode() == neighbourNode) return nextNeighbour;
            nextNeighbour = getNextNeighbour(node, nextNeighbour);
        }
        return null;
    }

    public void addNeighbour(GNode node, GNeighbour neighbour){
        neighbour.setNextNeighbour(node.getNeighbour());
        node.setNeighbour(neighbour);
    }

    void removeNeighbourTo(GNode node, GNode neighbourNode) {
        GNeighbour nextNeighbour = node.getNeighbour();
        if (nextNeighbour != null){
            if (nextNeighbour.getNeighbourNode() == neighbourNode){
                node.setNeighbour(nextNeighbour.getNextNeighbour());
            } else {
                while (getNextNeighbour(node, nextNeighbour) != null) {
                    GNeighbour lastNeighbour = nextNeighbour;
                    nextNeighbour = getNextNeighbour(node, nextNeighbour);
                    if (nextNeighbour.getNeighbourNode() == neighbourNode){
                        lastNeighbour.setNextNeighbour(getNextNeighbour(node, nextNeighbour));
                        break;
                    }
                }
            }
        }
    }

    void removeNeighbourTo(GNode node, int tileIdx) {
        GNeighbour nextNeighbour = node.getNeighbour();
        GNeighbour lastNeighbour = null;
        while (nextNeighbour != null) {
            if (nextNeighbour.getNeighbourNode().tileIdx == tileIdx){
                if (lastNeighbour == null){
                    node.setNeighbour(getNextNeighbour(node, nextNeighbour));
                } else {
                    lastNeighbour.setNextNeighbour(getNextNeighbour(node, nextNeighbour));
                }
            } else {
                lastNeighbour = nextNeighbour;
            }
            nextNeighbour = getNextNeighbour(node, nextNeighbour);
        }
    }

    public int countNeighbours(GNode node){
        GNeighbour neighbour = getNextNeighbour(node, null);
        int cnt = 0;
        while (neighbour != null){
            cnt++;
            neighbour = getNextNeighbour(node, neighbour);
        }
        return cnt;
    }

    /* returns oppositeNode - means the node has exactly 2 neighbours, where neighbourNode is one and the returned value is the other neighbour.
        returns null, if there is not exactly one other neighbour.
    */
    public GNode oppositeNode(GNode node, GNode neighbourNode){
        GNeighbour firstNeighbour = node.getNeighbour();
        if (firstNeighbour == null) return null; // found no neighbour
        GNeighbour secondNeighbour = getNextNeighbour(node, firstNeighbour);
        if (secondNeighbour == null) return null; // found just one neighbour
        GNeighbour thirdNeighbour = getNextNeighbour(node, secondNeighbour);
        if (thirdNeighbour != null) return null; // found third neighbour
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
        GNeighbour firstNeighbour = node.getNeighbour();
        if (firstNeighbour == null) return null; // found no neighbour
        GNeighbour secondNeighbour = getNextNeighbour(node, firstNeighbour);
        if (secondNeighbour == null) return null; // found just one neighbour
        GNeighbour thirdNeighbour = getNextNeighbour(node, secondNeighbour);
        if (thirdNeighbour != null) return null; // found third neighbour
        if (firstNeighbour == givenNeighbour){
            return secondNeighbour;
        }
        if (secondNeighbour == givenNeighbour){
            return firstNeighbour;
        }
        return null; // should not happen (given neighbourNode is not neighbour to node
    }

    public ArrayList<PointModel> segmentNodes(PointModel node1, PointModel node2){
        ArrayList<PointModel> segmentNodes = new ArrayList<>();
        if ((node1 instanceof  GNode gNode1) && (node2 instanceof GNode gNode2)){
            segmentNodes.add(node1);
            segmentNodes.add(node2);
            GNode nodeA = gNode1;
            GNode nodeB = gNode2;
            GNode nodeC;
            while ( (nodeC = oppositeNode(nodeB, nodeA)) != null){
                if (segmentNodes.contains(nodeC)) break;
                if (!sameGraph(nodeC,nodeA)) break;
                nodeA = nodeB;
                nodeB = nodeC;
                segmentNodes.add(nodeC);
            }
            nodeA = gNode2;
            nodeB = gNode1;
            while ( (nodeC = oppositeNode(nodeB, nodeA)) != null){
                if (segmentNodes.contains(nodeC)) break;
                if (!sameGraph(nodeC,nodeA)) break;
                nodeA = nodeB;
                nodeB = nodeC;
                segmentNodes.add(0, nodeC);
            }
        }
        return segmentNodes;
    }

    boolean preNodeRelax(GNode node) {
        return false;
    }

    public abstract int getTileCount();

    public void bidirectionalConnect(GNode node, GNode neighbourNode, GNeighbour baseNeighbour){
        WayAttributs wayAttributs = baseNeighbour==null?null:baseNeighbour.getWayAttributs();
        GNeighbour neighbour = new GNeighbour(neighbourNode, wayAttributs).setPrimaryDirection(baseNeighbour == null || baseNeighbour.isPrimaryDirection());
        GNeighbour reverseNeighbour = new GNeighbour(node, wayAttributs).setPrimaryDirection(baseNeighbour == null || !baseNeighbour.isPrimaryDirection());
        neighbour.setReverse(reverseNeighbour);
        reverseNeighbour.setReverse(neighbour);
        addNeighbour(node, neighbour);
        addNeighbour(neighbourNode, reverseNeighbour);
        float distance = (float)PointModelUtil.distance(node, neighbourNode);
        neighbour.setDistance(distance);
        reverseNeighbour.setDistance(distance);
    }


    Boolean sameGraph(PointModel node1, PointModel node2){
        if ((node1 instanceof GNode gNode1) && (node2 instanceof GNode gNode2)){
            return gNode1.tileIdx == gNode2.tileIdx;
        }
        return null;
    }

    @Override
    public String getRefDetails(PointModel node) {
        String res = "";
        if (node instanceof GNode gNode){
            res = getRefDetails(gNode.getNodeRef()) + getRefDetails(gNode.getNodeRef(true));
        }
        return res;
    }

    private String getRefDetails(GNodeRef ref){
        if (ref == null) return "";
        return String.format(Locale.ENGLISH, " %s settled=%b cost=%.2f heuristic=%.2f hcost=%.2f",ref.isReverse()?"rv":"fw",ref.isSetteled(),ref.getCost(),ref.getHeuristic(),ref.getHeuristicCost());
    }
}
