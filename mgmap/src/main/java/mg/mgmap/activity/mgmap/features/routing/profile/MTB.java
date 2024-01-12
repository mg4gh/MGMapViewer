package mg.mgmap.activity.mgmap.features.routing.profile;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingContext;
import mg.mgmap.activity.mgmap.features.routing.RoutingEngine;
import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;

public class MTB extends RoutingProfileTagEval {

    private Way mWay;

    private final VertDistCost mVertDistCost = new VertDistCost(2, 0.15);


    private double distance(PointModel start, PointModel target){
        double dist = PointModelUtil.distance(start, target);
        double vertDist = target.getEleD() - start.getEleD();
        return ( dist + mVertDistCost.getVertDistCosts(dist,vertDist) )+0.0001;
    }


    @Override
    public double getCost(Way way, GNode node1, GNode node2){
        setWayCosts(way);
        return distance(node1,node2) * mGenCostFactor;
    }
    protected double heuristic(GNode node, GNode target){
//        return distance(target, node) * 0.999;
        double dist = PointModelUtil.distance(node, target);
        double vertDist = target.getEleD() - node.getEleD();
        return (dist + mVertDistCost.getVertDistHeuristic(vertDist)) * 0.999;
    }

    protected double acceptedRouteDistance(RoutingEngine routingEngine, PointModel pmStart, PointModel pmEnd){
        double distance = distance(pmStart,pmEnd);
        double res = 0;
        RoutingContext routingContext = routingEngine.getRoutingContext();
        if (distance < routingContext.maxRoutingDistance){ // otherwise it will take too long
            res = Math.min (routingContext.maxRouteLengthFactor * distance + 2 * PointModelUtil.getCloseThreshold(), routingContext.maxRoutingDistance);
        }
        return res;
    }
    private void setWayCosts(Way way){
        if (mWay != way) {
            mWay = way;
            setWayTags(way);
            if (accessable) {
                if ("path".equals(highway)) {
                    if (mtbscale != null) {
                        switch (mtbscale) {
                            case "mtbs_0":
                            case "mtbs_1":
                                break;
                            case "mtbs_2":
                                mGenCostFactor = 1.5;
                                break;
                            case "mtbs_3":
                            default:
                                mGenCostFactor = 3;
                        }
                    } else
                        mGenCostFactor = 3;
                    if (trail_visibility != null) {
                        switch (trail_visibility) {
                            case "bad":
                                mMultCostFactor = 1.5;
                                break;
                            case "horrible":
                            case "no":
                                mMultCostFactor = 2;
                                break;
                        }
                    }
                } else if ("track".equals(highway)) {
                    mGenCostFactor = 1;
                } else if ("primary".equals(highway)) {
                    if ("bic_no".equals(bicycle))
                        accessable = false;
                    else if (cycleway != null)
                        mGenCostFactor = 2;
                    else
                        mGenCostFactor = 3;
                } else if ("secondary".equals(highway)) {
                    if (cycleway != null)
                        mGenCostFactor = 1.5;
                    else
                        mGenCostFactor = 2;
                } else if ("tertiary".equals(highway)) {
                    mGenCostFactor = 1.5;
                } else if ("steps".equals(highway)) {
                    mGenCostFactor = 4;
//                setFixUpDistParameter(8);
//                setFixDownDistParameter(8);
                } else if ("footway".equals(highway) ) {
                    if ( "bic_yes".equals(bicycle) )
                        mGenCostFactor = 1;
                    else
                        mGenCostFactor = 4;
                }
                else if ("bic_no".equals(bicycle))
                    mGenCostFactor = 4;
                else
                    mGenCostFactor = 1;
            } else
                mGenCostFactor = 10;

            mGenCostFactor = Math.max( mGenCostFactor * mMultCostFactor, 1);
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
