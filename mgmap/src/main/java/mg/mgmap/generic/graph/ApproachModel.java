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

import androidx.annotation.NonNull;

import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * An Approach is a model object that represents a close part of a graph.
 * In particular it contains references to the nodes (node1 and node2) of the graph, where an originating position is close to the connection.
 * It contains also a reference to the closest point of this line (approachNode). Approaches are stored in the context of the originating point.
 */
public class ApproachModel implements MultiPointModel, Comparable<ApproachModel> {

    private final int tileX;
    private final int tileY;
    private final PointModel pmPos;
    private GNode node1;
    private GNode node2;
    private final GNode approachNode;
    private final ArrayList<PointModel> visiblePoints = new ArrayList<>();

    public ApproachModel(int tileX, int tileY, PointModel pmPos, GNode node1, GNode node2, GNode approachNode) {
        this.tileX = tileX;
        this.tileY = tileY;
        this.pmPos = new PointModelImpl(pmPos.getLat(), pmPos.getLon());
        this.node1 = node1;
        this.node2 = node2;
        this.approachNode = approachNode;
        visiblePoints.add(this.pmPos);
        visiblePoints.add(approachNode);
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
    public int compareTo(@NonNull ApproachModel approach) {
        double distance = approachNode.getNeighbour().getCost();
        double otherDistance = approach.approachNode.getNeighbour().getCost();
        if (distance < otherDistance) return -1;
        if (distance > otherDistance) return 1;
        int res = (PointModelUtil.compareTo(approachNode, approach.approachNode));
        if (res == 0) res = (PointModelUtil.compareTo(node1, approach.node1));
        if (res == 0) res = (PointModelUtil.compareTo(node2, approach.node2));
        return res;
    }

    @Override
    public int size() {
        return visiblePoints.size();
    }

    @Override
    public PointModel get(int i) {
        return visiblePoints.get(i);
    }

    @NonNull
    @Override
    public Iterator<PointModel> iterator() {
        return visiblePoints.iterator();
    }

    @Override
    public BBox getBBox() {
        BBox bBox = new BBox();
        for (PointModel pm : visiblePoints){
            bBox.extend(pm);
        }
        return bBox;
    }
}
