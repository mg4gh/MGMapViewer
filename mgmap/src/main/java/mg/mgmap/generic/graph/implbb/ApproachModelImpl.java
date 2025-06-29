package mg.mgmap.generic.graph.implbb;

import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.model.PointModel;

public class ApproachModelImpl implements ApproachModel {

    PointModel pmPos;
    PointModel approachNode;
    BNode node1;
    BNode node2;
    float distance;

    public ApproachModelImpl(PointModel pmPos, PointModel approachNode, BNode node1, BNode node2, float distance) {
        this.pmPos = pmPos;
        this.approachNode = approachNode;
        this.node1 = node1;
        this.node2 = node2;
        this.distance = distance;
    }

    @Override
    public PointModel getPmPos() {
        return pmPos;
    }

    @Override
    public PointModel getApproachNode() {
        return approachNode;
    }

    @Override
    public float getApproachDistance() {
        return distance;
    }

    @Override
    public boolean verifyApproach(PointModel node1, PointModel approachNode, PointModel node2) {
        if (this.getApproachNode() != approachNode) return false;
        if ((node1 == this.node1) && (node2 == this.node2)) return true;
        if ((node1 == this.node2) && (node2 == this.node1)) return true;
        return false;

    }
}
