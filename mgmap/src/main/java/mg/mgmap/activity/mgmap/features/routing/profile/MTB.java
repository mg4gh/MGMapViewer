package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;

public class MTB extends GenRoutingProfile {
    public MTB( ) {
        super(new CostCalculatorHeuristicTwoPieceFunc( 0.09, 0, -0.27, 2));
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