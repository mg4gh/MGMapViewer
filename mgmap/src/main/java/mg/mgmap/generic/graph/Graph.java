package mg.mgmap.generic.graph;

import java.util.ArrayList;

import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;

public interface Graph {

    ArrayList<PointModel> segmentNodes(PointModel node1, PointModel node2);

    String getRefDetails(PointModel node);

    default void finalizeUsage(){}

    default BBox getBBox(){
        return null;
    }

    default ArrayList<MultiPointModel> getRawWays(){
        return null;
    }

    ArrayList<PointModel> getNeighbours(PointModel pointModel, ArrayList<PointModel> resList);
}
