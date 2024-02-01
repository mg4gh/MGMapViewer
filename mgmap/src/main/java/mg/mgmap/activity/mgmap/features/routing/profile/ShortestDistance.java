package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;

public class ShortestDistance extends RoutingProfile {

    public ShortestDistance( ) {
        super(new CostCalculatorShortestDist());
    }

    @Override
    public int getIconIdActive() {
        return R.drawable.rp_shortest_path_a;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_shortest_path_i;
    }

    @Override
    protected int getIconIdCalculating() {
        return R.drawable.rp_shortest_path_c;
    }
}
