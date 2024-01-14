package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class ShortestDistance extends RoutingProfile {

    @Override
    protected double getCost(WayAttributs wayAttributs, double distance, float verticalDistance) {
        return distance+0.0001;
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
