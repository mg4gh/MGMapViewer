package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;

public class TrekkingBike extends RoutingProfile {
    public TrekkingBike( ){
        super( new CostCalculatorSimple(10.0));
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
