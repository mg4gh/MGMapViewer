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

import androidx.annotation.NonNull;

import mg.mapviewer.model.BBox;
import mg.mapviewer.model.MultiPointModel;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.util.PointModelUtil;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * An Approach is a model object that represents a close part of a graph.
 * In particular it contains references to the nodes (node1 and node2) of the graph, where an originating position is close to the connection.
 * It contains also a reference to the closest point of this line (approachNode). Approaches are stored in the context of the originating point.
 */
public class ApproachModel implements MultiPointModel, Comparable<ApproachModel> {

    private PointModel pmPos;
    private GNode node1;
    private GNode node2;
    private GNode approachNode;
    private ArrayList<PointModel> visiblePoints = new ArrayList<>();

    public ApproachModel(PointModel pmPos, GNode node1, GNode node2, GNode approachNode) {
        this.pmPos = new PointModelImpl(pmPos.getLat(), pmPos.getLon());
        this.node1 = node1;
        this.node2 = node2;
        this.approachNode = approachNode;
        visiblePoints.add(this.pmPos);
        visiblePoints.add(approachNode);
    }

    public GNode getNode1() {
        return node1;
    }

    public GNode getNode2() {
        return node2;
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

    public boolean approachedLineMatch(PointModel pm1, PointModel pm2){
        boolean b1 = (PointModelUtil.compareTo(node1, pm1) == 0);
        b1 &=  (PointModelUtil.compareTo(node2, pm2) == 0);
        boolean b2 = (PointModelUtil.compareTo(node2, pm1) == 0);
        b2 &=  (PointModelUtil.compareTo(node1, pm2) == 0);
        return b1 || b2;
    }
    public boolean approachedLineMatch(ApproachModel otherApproach){ // optimization, since node1.lalo < node2.lalo for each approach
        Boolean bRes =  ( (PointModelUtil.compareTo(node1, otherApproach.getNode1())) == 0);
        bRes &= ( (PointModelUtil.compareTo(node2, otherApproach.getNode2())) == 0);
        return bRes;
    }

    public static boolean approachedSequenceMatch(PointModel pmStart, ApproachModel approach1, ApproachModel approach2, PointModel pmEnd){
        double distTotal = PointModelUtil.distance(pmStart, pmEnd);
        double distParts = PointModelUtil.distance(pmStart, approach1.getApproachNode())+
                PointModelUtil.distance(approach1.getApproachNode(),approach2.getApproachNode())+
                PointModelUtil.distance(approach2.getApproachNode(),pmEnd);
        return Math.abs(distParts - distTotal) < 0.1;
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
