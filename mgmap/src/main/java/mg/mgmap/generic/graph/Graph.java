package mg.mgmap.generic.graph;

import java.util.ArrayList;

import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointNeighbour;

public interface Graph {

    ArrayList<? extends PointModel> getNodes();

    PointNeighbour getNeighbour(PointModel node, PointModel neighbourNode);

    PointNeighbour getNextNeighbour(PointModel node, PointNeighbour neighbour);

    ArrayList<PointModel> segmentNodes(PointModel node1, PointModel node2, int closeThreshold, boolean limitToTile);

    String getRefDetails(PointModel node);

    float getCost(PointNeighbour neighbour);

    default void finalizeUsage(){};

    default BBox getBBox(){
        return null;
    }

    default ArrayList<MultiPointModel> getRawWays(){
        return null;
    }

    default Boolean sameGraph(PointModel node1, PointModel node2){
        return null;
    }

}
