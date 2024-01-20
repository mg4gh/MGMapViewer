package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public abstract class GenRoutingProfile extends RoutingProfile {

    protected final double mUpCosts;
    protected final double mDnCosts;// base costs in m per hm uphill;
    protected final double mUpSlopeLimit; //  up to this slope base Costs
    protected final double mDnSlopeLimit;
    protected final double mUpSlopeFactor;
    protected final double mDnSlopeFactor;
    protected final double mUpAddCosts; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
    protected final double mDnAddCosts;



    protected GenRoutingProfile(double upCosts, double upSlopeLimit, double upSlopeFactor, double dnCosts, double dnSlopeLimit, double dnSlopeFactor ){
        mUpCosts      = abs(upCosts); // required. Otherwise heuristic no longer correct
        mUpSlopeLimit = abs(upSlopeLimit); //mathematically not required, but only in this way meaningful. Uphill means additional costs.
        if (upSlopeFactor < 1 ) upSlopeFactor = 1; // required. Otherwise heuristic no longer correct
        mUpSlopeFactor = upSlopeFactor;
        mUpAddCosts = mUpSlopeFactor/( mUpSlopeLimit * mUpSlopeLimit);
        mDnCosts      = dnCosts;
        mDnSlopeLimit = -abs(dnSlopeLimit);
        if (dnSlopeFactor < 1 ) dnSlopeFactor = 1;
        mDnSlopeFactor = dnSlopeFactor;
        // required. Otherwise heuristic no longer correct
        mDnAddCosts = mDnSlopeFactor/( mDnSlopeLimit * mDnSlopeLimit) ;
    }


    @Override
    public final double getCost(WayAttributs wayAttributs, double dist, float vertDist) {
        if (dist <= 0.0000001 ) return 0.0001;
        double costs;
//        Log.d("Genrouting", "class" + wayAttributs.getClass().getName() + " " + wayAttributs );
        if ( !(wayAttributs instanceof WayTagEval))
            costs = calcCosts(dist, vertDist,
                    mUpCosts,mUpSlopeLimit,mUpAddCosts,
                    mDnCosts,mDnSlopeLimit,mDnAddCosts);
        else {
            WayTagEval wayTagEval = (WayTagEval) wayAttributs;
            costs = calcCosts(dist, vertDist,
                    wayTagEval.mUpCosts, wayTagEval.mUpSlopeLimit, wayTagEval.mUpAddCosts,
                    wayTagEval.mDnCosts, wayTagEval.mDnSlopeLimit, wayTagEval.mDnAddCosts)
                    * wayTagEval.mGenCostFactor;
//            if (wayTagEval.mGenCostFactor > 1 )
//              Log.e("traget path","costFactor:" + wayTagEval.mGenCostFactor + "  dist:" +dist + "  vertDist" + vertDist + "  cost:" + costs + "***");
        }

        return costs + 0.0001;
    }


    protected final double calcCosts(double dist, double vertDist, double upCosts, double upSlopeLimit, double upAddCosts,double dnCosts, double dnSlopeLimit, double dnAddCosts ){
        double slope = vertDist / dist;
        if ( abs(slope) > 10 ) Log.e("GenRoutingProfile","Suspicious Slope in calcCosts. Dist:" + dist + " VertDist:" + vertDist + " Slope:" + slope);
        double cost;
        if (slope > 0) {
            if (slope <= upSlopeLimit)
                cost = dist + vertDist * upCosts;
            else
                cost = dist + vertDist * ( upCosts + (slope - upSlopeLimit) * upAddCosts);
        } else {
            if (slope >= dnSlopeLimit)
               cost = dist + vertDist * dnCosts;
            else
                return dist + vertDist * ( dnCosts + (slope - dnSlopeLimit) * dnAddCosts);
        }
        return cost;
    }

    /* derivation of the heuristic only for positive slope values, so target is uphill (for negative in principle the same, but a bit more cumbersome )
    In general heuristic must always be smaller than the actual costs independent of the route taken to the target. Any route on any hypothetical surface can be taken,
    so basically any route in the three dimensional space. For the vertical direction the solution is trivial, since with the given cost function any route which includes up- and downhill
    will be more expensive than a route that is monotonously uphill. So I omit the formal derivation. For the horizontal component this is not that obvious, since cost function is designed in way
    so that cost increase with larger slope to prefer routes that don't follow the steep straight line to the target. So the optimal route should not be the straight line to the target!
    To find the function which represents the minimal costs (actually heuristic), I vary the distance of the route to the target. To do this, I introduce a new variable to vary the distance in the
    cost function and vary this variable:
    for slope >= upSlopeLimit: (slope = vertDist/dist)
    cost = dist * f + vertDist ( upCosts + ( vertDist/(dist * f) - upSlopeLimit) * upAddCosts)
    d ( cost( f ) ) / df = 0
    => dist - 1/f^2 * vertDist^2*upAddCosts/dist = 0 with the boundary condition that vertDist/(dist * f ) >= upSlopeLimit or f <=  slope/upSlopeLimit
    => f^2 = vertDist^2/dist^2 * 1/ upAddCosts  => f = slope/SQRT( upAddCosts )
    So taking into account the boundary condition:
    f = slope * Min( 1/upSlopeLimit, 1/SQRT(upAddCosts).
    a) For 1/upSlopeLimit <= 1/SQRT(upAddCosts) or upAddCosts >= 1/upSlopeLimit^2
    f = slope/upSlopeLimit
    now costs for this minimum, so basically the heuristic:
    cost = dist * slope/upSlopeLimit  * vertDist ( upCosts + ( vertDist/(dist*(slope/upSlopeLimit)) - upSlopeLimit ) * upAddCosts )
         = dist * vertDist/ ( dist*upSlopeLimit) + vertDist * upCosts  + vertDist^2*upAddCosts*upSlopeLimit/( dist * vertDist/dist) - vertDist*upSlopeLimit*upAddCosts
         = vertDist/upSlopeLimit + vertDist*upCosts + vertDist*upSlopeLimit*upAddCosts - vertDist*upSlopeLimit*upAddCosts
         = vertDist/upSlopeLimit + vertDist*upCosts
    This result now has a nice interpretation: vertDist/upSlopeLimit is the distance to the target for a route which runs exactly at the slope = upSlopeLimit. So the optimal costs are achieved if you find a route,
    which ascents to the target at a the constant slope of upSlopeLimit, which is exactly that, what we want to achieve with the cost function. The upAddCosts determine how fast costs grow beyond upSlopeLimit. With
    upAddCosts = 1/upSlopLimit^2, the right side curvature is zero with respect to a change of slope, so an infinitesimal larger slope of the optimal route would not change the cost.
    b) For 1/upSlopeLimit > 1/SQRT(upAddCosts)
    I omit this case for now and make sure that the constraint for case a is always full filled, so  upAddCosts >= 1/upSlopeLimit^2 (see constructor)
    In this case heuristic would be actually dependant on upAddCosts and the slope of the optimal route would be larger than upSlopeLimit.
    */

    protected final double heuristic(double dist, float vertDist){
        if (dist <= 0.0000001 ) {
            return 0.0;
        }
        double heuristic;
        double slope = vertDist / dist;
        if ( abs(slope) > 10 ) Log.e("GenRoutingProfile","Suspicious Slope in heuristic. Dist:" + dist + " VertDist:" + vertDist + " Slope:" + slope);
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

    /* Any modified cost function must be larger or equal to the base cost function, which is the foundation for the heuristic. Used to verify/correct cost factors derived from way tags */
    protected void checkSetCostFactors(WayTagEval wayTagEval,double upSlopeFactor, double dnSlopeFactor){
        if (wayTagEval.mUpCosts < mUpCosts) {
            wayTagEval.mUpCosts = mUpCosts;
//            Log.e("TagEval","UpCosts to small");
        }
        if (wayTagEval.mUpSlopeLimit < mUpSlopeLimit) {
            wayTagEval.mUpSlopeLimit = mUpSlopeLimit;
//            Log.e("TagEval","UpCosts to small");
        }
        if (wayTagEval.mDnCosts < mDnCosts) {
            wayTagEval.mDnCosts = mDnCosts;
//            Log.e("TagEval","UpCosts to small");
        }
        if (wayTagEval.mDnSlopeLimit > mDnSlopeLimit) {
            wayTagEval.mDnSlopeLimit = mDnSlopeLimit;
//            Log.e("TagEval","UpCosts to small");
        }
        if (upSlopeFactor < 1) upSlopeFactor =1;
        wayTagEval.mUpAddCosts = upSlopeFactor/( mUpSlopeLimit * mUpSlopeLimit);
        if (dnSlopeFactor < 1) dnSlopeFactor = 1;
        wayTagEval.mDnAddCosts = dnSlopeFactor/( mDnSlopeLimit * mDnSlopeLimit) ;
        if (wayTagEval.mGenCostFactor < 1 ) wayTagEval.mGenCostFactor = 1;
    }

}
