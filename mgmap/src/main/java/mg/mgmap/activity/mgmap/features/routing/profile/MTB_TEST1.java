package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB_TEST1 extends GenRoutingProfile {
    public MTB_TEST1( ) {
        super(new CostCalculatorHeuristicTwoPieceFunc( 0.13,  0, -0.27, 2));
    }

    public void refreshWayAttributes(WayAttributs wayAttributs) {
        if (wayAttributs instanceof WayTagEval ) {
            WayTagEval wayTagEval = (WayTagEval) wayAttributs;
            wayTagEval.setCostCalculator(new CostCalculatorMTB(wayTagEval, (CostCalculatorHeuristicTwoPieceFunc) mCostCalculatorForProfile, 0.4,0.9));
        }
    }

    @Override
    protected int getIconIdActive() {
        return R.drawable.rp_mtb1;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_mtb2;
    }
}
