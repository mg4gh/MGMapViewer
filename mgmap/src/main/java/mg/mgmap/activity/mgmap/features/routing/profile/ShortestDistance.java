package mg.mgmap.activity.mgmap.features.routing.profile;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.model.PointModelUtil;

public class ShortestDistance extends RoutingProfile {


    GNode lastNode1 = null;
    GNode lastNode2 = null;
    double lastCost = 0;
    @Override
    public double getCost(Way way, GNode node1, GNode node2){
        if ((node1==lastNode2) && (node2==lastNode1)) return lastCost;
        lastNode1 = node1;
        lastNode2 = node2;
        lastCost = PointModelUtil.distance(node1, node2)+0.0001;
        return lastCost;


    }


    @Override
    protected int getIconIdActive() {
        return R.drawable.rp_shortest_path1;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_shortest_path2;
    }


}
