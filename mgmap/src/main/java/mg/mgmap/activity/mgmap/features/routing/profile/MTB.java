package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;

public class MTB extends RoutingProfile {
    public MTB( ) {
        super(new CostCalculatorHeuristicTwoPieceFunc(8.0, 0.10, 3, 0, -0.27, 2));
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