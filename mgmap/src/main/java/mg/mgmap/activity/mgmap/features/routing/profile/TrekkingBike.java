package mg.mgmap.activity.mgmap.features.routing.profile;

import android.content.Context;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.model.PointModelUtil;

public class TrekkingBike extends RoutingProfile {

    public TrekkingBike(Context context) {
        super(context);
    }

    @Override
    public double getCost(Way way, GNode node1, GNode node2){
        return PointModelUtil.distance(node1, node2);
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
