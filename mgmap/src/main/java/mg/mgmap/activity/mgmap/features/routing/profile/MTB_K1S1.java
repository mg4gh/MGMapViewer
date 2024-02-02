package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB_K1S1 extends RoutingProfile {
    public MTB_K1S1( ) {
        super(new CostCalculatorHeuristicTwoPieceFunc( 1,  0, -0.27, 2));
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalculatorMTB(wayAttributs, (CostCalculatorHeuristicTwoPieceFunc) profileCalculator, 1.1,0.0,1.2,1.0,0.8,0.5);
    }

    @Override
    public int getIconIdActive() {
        return R.drawable.rp_mtb_k1s1_a;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_mtb_k1s1_i;
    }
    @Override
    protected int getIconIdCalculating() {
        return R.drawable.rp_mtb_k1s1_c;
    }
}
