package mg.mgmap.activity.mgmap.features.routing.profile;

import android.content.Context;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.model.PointModelUtil;

public class TrekkingBike extends RoutingProfile {
    private final VertDistCost mVertDistCost = new VertDistCost(2, 0.15);
    public TrekkingBike(Context context) {
        super(context);
    }

    @Override
    public double getCost(Way way, GNode node1, GNode node2){
        double dist = PointModelUtil.distance(node1, node2);
        double vertDist = node2.getEleD() - node1.getEleD();
        return dist + mVertDistCost.getVertDistCosts(dist,vertDist);
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
