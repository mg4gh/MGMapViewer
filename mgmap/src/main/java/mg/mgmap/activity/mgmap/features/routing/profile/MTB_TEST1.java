package mg.mgmap.activity.mgmap.features.routing.profile;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB_TEST1 extends RoutingProfile {
    /** implementation based on temp test class VertDistAlt: After final Test Implementation needs to be exchanged.
     */
    private final VertDistAlt mVertDistCost = new VertDistAlt( );

/*    @Override
    public WayAttributs getWayAttributes(Way way) {
        return new WayAttributes4Profile(way);
    } */

    @Override
    protected double getCost(WayAttributs wayAttributs, double distance, float verticalDistance) {
//        double genCostFactor = (wayAttributs instanceof WayAttributes4Profile) ? ((WayAttributes4Profile) wayAttributs).getGenCostFactor() : 1;
        double genCostFactor = 1;
        return (distance + mVertDistCost.getVertDistCosts(distance, verticalDistance)) * genCostFactor + 0.0001;
    }

    @Override
    protected double heuristic(double distance, float verticalDistance) {
        return (distance + mVertDistCost.getVertDistHeuristic(verticalDistance)) * 0.999;
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
