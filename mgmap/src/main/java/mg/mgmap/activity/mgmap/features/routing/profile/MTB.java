package mg.mgmap.activity.mgmap.features.routing.profile;

import android.content.Context;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.model.PointModelUtil;

public class MTB extends RoutingProfile {

    public MTB(Context context) {
        super(context);
    }

    @Override
    public double getCost(Way way, GNode node1, GNode node2){
        double distance = PointModelUtil.distance(node1, node2);
        double vDistance = node2.getEleD() - node1.getEleD();
        double ascend = vDistance/distance * 100;

        if (ascend > 0){
            return distance + distance*ascend/10;
        }
        return distance;
    }

    @Override
    protected int getIconIdActive() {
        return R.drawable.rp_mtb1;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_mtb2;
    }

}
