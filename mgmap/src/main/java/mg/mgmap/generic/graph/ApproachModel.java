package mg.mgmap.generic.graph;

import mg.mgmap.generic.model.PointModel;

public interface ApproachModel {

    PointModel getPmPos();
    PointModel getApproachNode();
    float getApproachDistance();

    boolean verifyApproach(PointModel node1, PointModel approachNode, PointModel node2);
}
