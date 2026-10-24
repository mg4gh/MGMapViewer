package mg.mgmap.generic.graph.impl3;

import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;

public class DApproach implements ApproachModel {

    int tileIdx = 0;
    short node1Idx = 0;
    short node2Idx = 0;
    short neighbour1to2 = 0;
    PointModel pos = null;
    DNode approachNode = new DNode();
    float distance = Float.MAX_VALUE;

    @Override
    public PointModel getPmPos() {
        return pos;
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
        return true;
    }
}
