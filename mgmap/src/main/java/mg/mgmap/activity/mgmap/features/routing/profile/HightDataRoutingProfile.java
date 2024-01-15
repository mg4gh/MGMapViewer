package mg.mgmap.activity.mgmap.features.routing.profile;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.RoutingContext;
import mg.mgmap.activity.mgmap.features.routing.RoutingEngine;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;

public abstract class HightDataRoutingProfile extends RoutingProfile {

    protected final double mUpCosts;
    protected final double mDnCosts;// base costs in m per hm uphill;
    protected final double mUpSlopeLimit; //  up to this slope base Costs
    protected final double mDnSlopeLimit;
    protected final double mUpAddCosts; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
    //    private final double mMaxSlope = 3.0 ; // Maximal Slope up to which additional costs increase costs per hm
    protected final double mDnAddCosts;


    protected HightDataRoutingProfile( double upCosts, double upSlopeLimit, double upSlopeFactor,double dnCosts, double dnSlopeLimit, double dnSlopeFactor ){
        mUpCosts      = upCosts;
        mUpSlopeLimit = upSlopeLimit;
        mUpAddCosts = upSlopeFactor/( upSlopeLimit * upSlopeLimit);
        mDnCosts      = dnCosts;
        mDnSlopeLimit = dnSlopeLimit;
        mDnAddCosts = dnSlopeFactor/( dnSlopeLimit * dnSlopeLimit) ;
    }

    public WayTagEval getWayTagEval(){
        return null;
    }

    /** supposed to be redefined. Default implementation ignores way */
    protected double calcCosts( Way way, double dist, double vertDist){
        return calcCosts(dist,vertDist, mUpCosts,mUpSlopeLimit,mUpAddCosts,mDnCosts,mDnSlopeLimit,mDnAddCosts);
    }
    @Override
    public final double getCost(Way way, GNode node1, GNode node2) {
        double dist = PointModelUtil.distance(node1, node2) + 0.0001;
        double vertDist = node2.getEleD() - node1.getEleD();
        return calcCosts(way,dist,vertDist);
    }

    protected final double calcCosts(double dist, double vertDist){
        return calcCosts(dist,vertDist, mUpCosts,mUpSlopeLimit,mUpAddCosts,mDnCosts,mDnSlopeLimit,mDnAddCosts);
    }
    protected final double calcCosts(double dist, double vertDist, double upCosts, double upSlopeLimit, double upAddCosts,double dnCosts, double dnSlopeLimit, double dnAddCosts ){
        double slope = vertDist / dist;
        if (slope > 0) {
            if (slope <= upSlopeLimit)
                return dist + vertDist * upCosts;
            else
                return dist + vertDist * ( upCosts + (slope - upSlopeLimit) * upAddCosts);
        } else {
            if (slope >= dnSlopeLimit)
                return dist + vertDist * dnCosts;
            else
                return dist + vertDist * ( dnCosts + (slope - dnSlopeLimit) * dnAddCosts);
        }
    }
    protected final double heuristic(GNode node, GNode target){
        double dist = PointModelUtil.distance(node, target);
        double vertDist = target.getEleD() - node.getEleD();
        double heuristic;
        double slope = vertDist / dist;
        if (slope >= 0){
            if (slope <= mUpSlopeLimit)
                heuristic = dist + vertDist * mUpCosts;
            else
                heuristic = vertDist/ mUpSlopeLimit + vertDist * mUpCosts; // vertDist/mMaxOptUpSlope is the distance to the target if slope = mMaxOptUpSlope!
        } else {
            if (slope >= mDnSlopeLimit)
                heuristic = dist + vertDist * mDnCosts;
            else
                heuristic = vertDist/ mDnSlopeLimit + vertDist * mDnCosts; // => heuristic strictly >= 0 => 1/mMaxOptDownSlope + mBaseDownCosts > 0 =>  1/mMaxOptDownSlope > -mBaseDownCosts => -1/mMaxOptDownSlope > mBaseDownCosts
        }
        return heuristic * 0.999;
    }

    protected double acceptedRouteDistance(RoutingEngine routingEngine, PointModel pmStart, PointModel pmEnd){
        double dist = PointModelUtil.distance(pmStart, pmEnd) + 0.0001;
        double vertDist = pmStart.getEleD() - pmEnd.getEleD();
        double distance = calcCosts(dist,vertDist);
        double res = 0;
        RoutingContext routingContext = routingEngine.getRoutingContext();
        if (distance < routingContext.maxRoutingDistance){ // otherwise it will take too long
            res = Math.min (routingContext.maxRouteLengthFactor * distance + 2 * PointModelUtil.getCloseThreshold(), routingContext.maxRoutingDistance);
        }
        return res;
    }


    @Override
    protected int getIconIdActive() {
        return R.drawable.rp_mtb1;
    }

    @Override
    protected int getIconIdInactive() { return R.drawable.rp_mtb2;  }
}
