package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;

public class MTB_TEST2 extends GenRoutingProfile {
    public MTB_TEST2( ) {
        super(new CostCalculatorHeuristicTwoPieceFunc(8.0, 0.12, 3, 0, -0.27, 2));
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
