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

import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;

/**
 * An Approach is a model object that represents a close part of a graph.
 * In particular it contains references to the nodes (node1 and node2) of the graph, where an originating position is close to the connection.
 * It contains also a reference to the closest point of this line (approachNode). Approaches are stored in the context of the originating point.
 */
public class ApproachModelImpl implements ApproachModel {

    private final int tileX;
    private final int tileY;
    private final PointModel pmPos;
    private GNode node1;
    private GNode node2;
    private final GNode approachNode;
    private final float approachDistance;

    public ApproachModelImpl(int tileX, int tileY, PointModel pmPos, GNode node1, GNode node2, GNode approachNode, float approachDistance) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.pmPos = new PointModelImpl(pmPos.getLat(), pmPos.getLon());
        this.node1 = node1;
        this.node2 = node2;
        this.approachNode = approachNode;
        this.approachDistance = approachDistance;
    }

    public int getTileX() {
        return tileX;
    }
    public int getTileY() {
        return tileY;
    }

    public GNode getNode1() {
        return node1;
    }
    public void setNode1(GNode node1) {
        this.node1 = node1;
    }

    public GNode getNode2() {
        return node2;
    }
    public void setNode2(GNode node2) {
        this.node2 = node2;
    }

    public GNode getApproachNode() {
        return approachNode;
    }

    public PointModel getPmPos() {
        return pmPos;
    }

    @Override
    public float getApproachDistance() {
        return approachDistance;
    }

    public boolean verifyApproach(PointModel node1, PointModel approachNode, PointModel node2){
        if (this.getApproachNode() != approachNode) return false;
        if ((node1 == this.getNode1()) && (node2 == this.getNode2())) return true;
        if ((node1 == this.getNode2()) && (node2 == this.getNode1())) return true;
        return false;
    }

    @Override
    public String toString() {
        return "ApproachModelImpl{" +
                "tileX=" + tileX +
                ", tileY=" + tileY +
                ", pmPos=" + pmPos +
                ", node1=" + node1 +
                ", node2=" + node2 +
                ", approachNode=" + approachNode +
                '}';
    }
}
