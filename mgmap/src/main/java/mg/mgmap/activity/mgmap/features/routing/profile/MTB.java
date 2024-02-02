package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB extends RoutingProfile {
    public MTB( ) {
        super(new CostCalculatorHeuristicTwoPieceFunc( 1, 0, -0.27, 2));
    }

/*    public void refreshWayAttributes(WayAttributs wayAttributs) {
        if (wayAttributs instanceof WayTagEval ) {
            WayTagEval wayTagEval = (WayTagEval) wayAttributs;
            wayTagEval.setCostCalculator(new CostCalculatorMTB(wayTagEval, (CostCalculatorHeuristicTwoPieceFunc) mCostCalculatorForProfile, 1.0,0.4));
        }
    } */

    @Override
    protected int getIconIdActive() {
        return R.drawable.rp_mtb1;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_mtb2;
    }
}