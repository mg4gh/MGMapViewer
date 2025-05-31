package mg.mgmap.generic.model;

public interface ApproachModel {

    PointModel getPmPos();
    PointModel getApproachNode();
    float getApproachDistance();

    boolean verifyApproach(PointModel node1, PointModel approachNode, PointModel node2);
}
