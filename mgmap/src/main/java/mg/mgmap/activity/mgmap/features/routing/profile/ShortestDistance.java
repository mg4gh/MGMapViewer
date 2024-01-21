package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;

public class ShortestDistance extends GenRoutingProfile {

    public ShortestDistance( ) {
        super(new CostCalculatorShortestDist());
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
