package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class TrekkingBike extends RoutingProfile {

    @Override
    protected double getCost(WayAttributs wayAttributs, double distance, float verticalDistance) {
        if (verticalDistance > 0){
            return distance + verticalDistance*10;
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
