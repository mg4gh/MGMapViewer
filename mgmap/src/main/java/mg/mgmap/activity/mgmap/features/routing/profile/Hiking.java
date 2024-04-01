package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class Hiking extends RoutingProfile {

    public Hiking( ) {
        super(new CostCalculatorHiking());
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalculatorHiking(wayAttributs);
    }
    @Override
    public int getIconIdActive() {
        return R.drawable.rp_hiking_a;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_hiking_i;
    }

    @Override
    protected int getIconIdCalculating() {
        return R.drawable.rp_hiking_c;
    }
}