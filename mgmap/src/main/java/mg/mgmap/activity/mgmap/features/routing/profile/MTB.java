package mg.mgmap.activity.mgmap.features.routing.profile;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB extends RoutingProfile {
    /** old implementation: After final Test Implementation needs to be exchanged.
     */
    private final VertDistCost mVertDistCost = new VertDistCost(2, 0.15);

    @Override
    public WayAttributs getWayAttributes(Way way) {
        return new WayAttributes4MTB(way);
    }

    @Override
    public void refreshWayAttributes(WayAttributs wayAttributs) {
        if (wayAttributs instanceof WayAttributes4MTB) {
            ((WayAttributes4MTB) wayAttributs).calcCostFactors();
        }
    }

    @Override
    protected double getCost(WayAttributs wayAttributs, double distance, float verticalDistance) {
        double genCostFactor = (wayAttributs instanceof WayAttributes4MTB)?((WayAttributes4MTB) wayAttributs).getGenCostFactor():1;
        return ( distance + mVertDistCost.getVertDistCosts(distance,verticalDistance) ) * genCostFactor + 0.0001;
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