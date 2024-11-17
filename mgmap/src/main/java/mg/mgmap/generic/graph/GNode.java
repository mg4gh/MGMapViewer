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

import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;

/**
 * This class is the basis for a node in a graph.
 */

public class GNode extends PointModelImpl {

    /**
     * This is the entry of a queue of neighbours. The first link refers to the node itself.
     */
    private final GNeighbour neighbour;

    // for Routing algorithms
    private GNodeRef nodeRef = null;
    private GNodeRef nodeReverseRef = null;

    int tileIdx;
    byte borderNode = 0;
    public static final byte BORDER_NODE_WEST  = 0x08;
    public static final byte BORDER_NODE_NORTH = 0x04;
    public static final byte BORDER_NODE_EAST  = 0x02;
    public static final byte BORDER_NODE_SOUTH = 0x01;

    byte flags = 0; // used for tile height smoothing
    public static final byte FLAG_FIX               = 0x01;
    public static final byte FLAG_VISITED           = 0x02;
    public static final byte FLAG_HEIGHT_RELEVANT   = 0x04;

    public static int deltaX(byte border){
        return switch (border) {
            case BORDER_NODE_WEST -> -1;
            case BORDER_NODE_EAST -> 1;
            default -> 0;
        };
    }
    public static int deltaY(byte border){
        return switch (border) {
            case BORDER_NODE_NORTH -> -1;
            case BORDER_NODE_SOUTH -> 1;
            default -> 0;
        };
    }
    public static byte oppositeBorder(byte border){
        return switch (border) {
            case BORDER_NODE_WEST -> BORDER_NODE_EAST;
            case BORDER_NODE_EAST -> BORDER_NODE_WEST;
            case BORDER_NODE_NORTH -> BORDER_NODE_SOUTH;
            case BORDER_NODE_SOUTH -> BORDER_NODE_NORTH;
            default -> 0;
        };
    }

    public GNode(double latitude, double longitude, float ele, float eleAcc, double cost){
        super(latitude, longitude, ele, eleAcc);
        this.neighbour = new GNeighbour(this, cost);
    }

    public GNeighbour getNeighbour() {
        return neighbour;
    }

    public void addNeighbour(GNeighbour neighbour) {
        getLastNeighbour().setNextNeighbour(neighbour);
    }

    GNeighbour getLastNeighbour(){
        GNeighbour nextNeighbour = this.neighbour;
        while (nextNeighbour.getNextNeighbour() != null) {
            nextNeighbour = nextNeighbour.getNextNeighbour();
        }
        return nextNeighbour;
    }

    int countNeighbours(){
        int cnt = 0;
        GNeighbour nextNeighbour = this.neighbour;
        while (nextNeighbour.getNextNeighbour() != null) {
            nextNeighbour = nextNeighbour.getNextNeighbour();
            cnt++;
        }
        return cnt;
    }

    public GNeighbour getNeighbour(GNode oNode){
        GNeighbour nextNeighbour = this.neighbour;
        while (nextNeighbour.getNextNeighbour() != null) {
            nextNeighbour = nextNeighbour.getNextNeighbour();
            if (nextNeighbour.getNeighbourNode() == oNode) return nextNeighbour;
        }
        return null;
    }

    public GNodeRef getNodeRef() {
        return nodeRef;
    }
    public GNodeRef getNodeRef(boolean reverse) {
        return reverse?nodeReverseRef:nodeRef;
    }

    public void setNodeRef(GNodeRef nodeRef) {
        if (nodeRef == null){
            resetNodeRefs();
        } else if (nodeRef.isReverse()){
            nodeReverseRef = nodeRef;
        } else {
            this.nodeRef = nodeRef;
        }
    }

    public void resetNodeRefs(){
        this.nodeRef = null;
        nodeReverseRef = null;
    }

    public void bidirectionalConnect(GNode neighbourNode, GNeighbour baseNeighbour){
        WayAttributs wayAttributs = baseNeighbour==null?null:baseNeighbour.getWayAttributs();
        GNeighbour neighbour = new GNeighbour(neighbourNode, wayAttributs).setPrimaryDirection(baseNeighbour == null || baseNeighbour.isPrimaryDirection());
        GNeighbour reverseNeighbour = new GNeighbour(this, wayAttributs).setPrimaryDirection(baseNeighbour == null || !baseNeighbour.isPrimaryDirection());
        neighbour.setReverse(reverseNeighbour);
        reverseNeighbour.setReverse(neighbour);
        this.addNeighbour(neighbour);
        neighbourNode.addNeighbour(reverseNeighbour);
        double distance = PointModelUtil.distance(this, neighbourNode);
        neighbour.setDistance(distance);
        reverseNeighbour.setDistance(distance);
    }

    public void removeNeighbourNode(GNode neighbourNode){
        GNeighbour nextNeighbour = this.neighbour;
        while (nextNeighbour.getNextNeighbour() != null) {
            GNeighbour lastNeighbour = nextNeighbour;
            nextNeighbour = nextNeighbour.getNextNeighbour();
            if (nextNeighbour.getNeighbourNode() == neighbourNode){
                lastNeighbour.setNextNeighbour(nextNeighbour.getNextNeighbour());
            }
        }
    }

    public void removeNeighbourNode(int tileIdx){
        GNeighbour nextNeighbour = this.neighbour;
        GNeighbour lastNeighbour = nextNeighbour;
        while (nextNeighbour.getNextNeighbour() != null) {
            nextNeighbour = nextNeighbour.getNextNeighbour();
            if (nextNeighbour.getNeighbourNode().tileIdx == tileIdx){
                lastNeighbour.setNextNeighbour(nextNeighbour.getNextNeighbour());
            } else {
                lastNeighbour = nextNeighbour;
            }
        }
    }


    public GGraphTile getTile(GGraphTileFactory gFactory){
        return gFactory.getGGraphTile(tileIdx>>16, tileIdx&0xFFFF);
    }

    public static boolean sameTile(GNode node1, GNode node2){
        return node1.tileIdx == node2.tileIdx;
    }

    public boolean isFlag(byte flag){
        return (flags & flag) != 0;
    }
    public void setFlag(byte flag, boolean value){
        if (value){
            flags |= flag;
        } else {
            flags &= (byte)(flag ^ 0xFF);
        }
    }

    public void fixEle(float ele){
        this.ele = ele;
    }
}
