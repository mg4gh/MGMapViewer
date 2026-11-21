package mg.mgmap.generic.graph.impl5;

import androidx.annotation.NonNull;

import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.model.PointModel;

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

    @NonNull
    @Override
    public String toString() {
        return "DApproach{" +
                "tileIdx=" + (tileIdx>>16) + ":" + (tileIdx&0xFFFF) +
                ", node1Idx=" + node1Idx +
                ", node2Idx=" + node2Idx +
                ", neighbour1to2=" + neighbour1to2 +
                ", pos=" + pos +
                ", approachNode=" + approachNode +
                ", distance=" + distance +
                '}';
    }
}
