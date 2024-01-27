package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class TrekkingBike extends RoutingProfile {
    public TrekkingBike( ) {
        super(new CostCalculatorHeuristicTwoPieceFunc( 0.08,  0, -0.12, 2));
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalculatorTreckingBike(wayAttributs, (CostCalculatorHeuristicTwoPieceFunc) profileCalculator);
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
