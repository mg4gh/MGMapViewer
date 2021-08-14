package mg.mgmap.application.util;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.datastore.Way;

import mg.mgmap.generic.graph.GNeighbour;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;

public class CostProviderShortDist extends  CostProvider{

    public void initializeSegment(Way way) {
        accessable=false;
        for (Tag tag : way.tags) {
            if (tag.key.equals("highway")) {
                accessable = true;
                return;
            }
        }

    }

    public void setNodes(PointModel node1, GNeighbour neighbour12, PointModel node2, GNeighbour neighbour21) {
        double d  = PointModelUtil.distance(node1, node2);
        neighbour12.setCost(d);
        neighbour21.setCost(d);
    }

//    @Override
//    public double getHeuristicCosts(PointModel node1, PointModel node2) {
//        return PointModelUtil.distance(node1, node2);
//    }

    @Override
    public void finalizeSegment() {

    }

    @Override
    public void clearSegment() {

    }
}
