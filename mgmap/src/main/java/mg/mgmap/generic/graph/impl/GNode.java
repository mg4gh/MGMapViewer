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

import mg.mgmap.generic.model.PointModelImpl;

/**
 * This class is the basis for a node in a graph.
 */

public class GNode extends PointModelImpl {

    /**
     * This is the entry of a queue of neighbours. The first link refers to the node itself.
     */
    private GNeighbour neighbour = null;

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
    public static final byte FLAG_INVALID           = 0x40;

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

    public GNode(double latitude, double longitude, float ele, float eleAcc){
        super(latitude, longitude, ele, eleAcc);
    }

    public GNeighbour getNeighbour() {
        return neighbour;
    }
    public void setNeighbour(GNeighbour neighbour){
        this.neighbour = neighbour;
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
