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
package mg.mgmap.generic.graph.impl2;

import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;

/**
 * A basic graph implementation based on GNode and GNeighbour.
 */

public abstract class GGraph implements Graph {

    public static final double CONNECT_THRESHOLD_METER = 0.5; // means 0.5m

    private final ArrayList<GNode> gNodes = new ArrayList<>();

    public ArrayList<GNode> getGNodes(){
        return gNodes;
    }

    GNeighbour getNextNeighbour(GNode node, GNeighbour neighbour){
        if (neighbour != null){
            return neighbour.getNextNeighbour();
        } else {
            return node.getNeighbour();
        }
    }

    public void addNeighbour(GNode node, GNeighbour neighbour){
        neighbour.setNextNeighbour(node.getNeighbour());
        node.setNeighbour(neighbour);
    }

    public void removeNeighbourTo(GNode node, GNode neighbourNode) {
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

    public void removeNeighbourTo(GNode node, int tileIdx) {
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
        ArrayList<PointModel> pms = new ArrayList<>();
        GIntermediateNode giNode = (node1 instanceof GIntermediateNode giNode1)?giNode1:null;
        giNode = (node2 instanceof GIntermediateNode giNode2)?giNode2:giNode;
        if (giNode == null){ // both nodes are not GIntermediateNode -> so the are GNode
            pms.add(node1);
            pms.add(node2);
        } else {
            pms.add(giNode.neighbour.getReverse().getNeighbourNode());
            for (int pIdx=0; pIdx<giNode.neighbour.cntIntermediates(); pIdx++){
                int[] intermediates = giNode.neighbour.getIntermediatesPoints();
                pms.add(PointModelImpl.createFromLaLo(intermediates[3*pIdx],intermediates[3*pIdx+1],intermediates[3*pIdx+2]));
            }
            pms.add(giNode.neighbour.getNeighbourNode());
        }
        return pms;
    }

    boolean preNodeRelax(GNode node) {
        return false;
    }

    public int getTileCount(){
        return -1;
    }

    public GNeighbour bidirectionalConnect(GNode node, GNode neighbourNode, GNeighbour baseNeighbour){
        WayAttributs wayAttributs = baseNeighbour==null?null:baseNeighbour.getWayAttributs();
        GNeighbour neighbour = new GNeighbour(neighbourNode, wayAttributs).setPrimaryDirection(baseNeighbour == null || baseNeighbour.isPrimaryDirection());
        GNeighbour reverseNeighbour = new GNeighbour(node, wayAttributs).setPrimaryDirection(baseNeighbour == null || !baseNeighbour.isPrimaryDirection());
        neighbour.setReverse(reverseNeighbour);
        reverseNeighbour.setReverse(neighbour);
        addNeighbour(node, neighbour);
        addNeighbour(neighbourNode, reverseNeighbour);
        return neighbour;
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
