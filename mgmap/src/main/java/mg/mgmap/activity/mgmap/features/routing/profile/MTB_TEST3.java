package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB_TEST3 extends GenRoutingProfile {

    public MTB_TEST3( ) {
        super(new CostCalculatorForProfile(8.0, 0.10, 3, 0, -0.27, 2));
    }

    public void refreshWayAttributes(WayAttributs wayAttributs) {
        if (wayAttributs instanceof WayTagEval ) {
            WayTagEval wayTagEval = (WayTagEval) wayAttributs;
            wayTagEval.mCostCalculator = new CostCalculatorMTB(wayTagEval, mCostCalculatorForProfile);
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
