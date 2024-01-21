package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class TrekkingBike extends GenRoutingProfile {
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
