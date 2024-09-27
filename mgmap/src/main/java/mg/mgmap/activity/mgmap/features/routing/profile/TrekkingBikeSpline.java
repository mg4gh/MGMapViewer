package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class TrekkingBikeSpline extends RoutingProfile {
    public TrekkingBikeSpline( ) {
        super(new CostCalcSplineProfileTreckingBike( ));
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalcSplineTreckingBike(wayAttributs, (CostCalcSplineProfileTreckingBike) profileCalculator);
    }
    @Override
    public int getIconIdActive() {
        return R.drawable.rp_trekking_a;
    }
    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_trekking_i;
    }
    protected int getIconIdCalculating() {
        return R.drawable.rp_trekking_c;
    }

}
