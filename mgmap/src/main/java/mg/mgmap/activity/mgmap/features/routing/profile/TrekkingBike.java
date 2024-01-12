package mg.mgmap.activity.mgmap.features.routing.profile;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.model.PointModelUtil;

public class TrekkingBike extends RoutingProfile {

    @Override
    public double getCost(Way way, GNode node1, GNode node2){
        double distance = PointModelUtil.distance(node1, node2);
        double vDistance = node2.getEleD() - node1.getEleD();

        if (vDistance > 0){
            return distance + vDistance*10;
        }
        return distance;
    }


    @Override
    protected int getIconIdActive() {
        return R.drawable.rp_trekking1;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_trekking2;
    }

}
