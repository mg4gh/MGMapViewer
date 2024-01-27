package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

public class CostCalculatorHeuristicTwoPieceFunc extends CostCalculatorTwoPieceFunc implements IfCostCalcHeuristic {


    protected double mUpSlopeFactor;
    protected double mDnSlopeFactor;
    protected CostCalculatorHeuristicTwoPieceFunc( double upSlopeLimit, double dnCosts, double dnSlopeLimit, double dnSlopeFactor) {
        mUpSlopeLimit = abs(upSlopeLimit); //mathematically not required, but only in this way meaningful. Uphill means additional costs.
        mUpCosts      = fb/mUpSlopeLimit; // required. Otherwise heuristic no longer correct
//        double upSlopeFactor = fa; //0.9/Math.sqrt(mUpSlopeLimit);
//        if (upSlopeFactor < 1 ) upSlopeFactor = 1; // required. Otherwise heuristic no longer correct
        mUpSlopeFactor = fa;
        mUpAddCosts = mUpSlopeFactor/( mUpSlopeLimit * mUpSlopeLimit);
        mDnCosts      = dnCosts;
        mDnSlopeLimit = -abs(dnSlopeLimit);
        if (dnSlopeFactor < 1 ) dnSlopeFactor = 1;
        // required. Otherwise heuristic no longer correct
        mDnAddCosts = dnSlopeFactor/( mDnSlopeLimit * mDnSlopeLimit) ;
        mDnSlopeFactor = dnSlopeFactor;
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

    public double heuristic(double dist, float vertDist){
        if (dist <= 0.0000001 ) {
            return 0.0;
        }
        double heuristic;
        double slope = vertDist / dist;
        if ( abs(slope) >= 10 )
            Log.e("Heuristic","Suspicious Slope in heuristic. Dist:" + dist + " VertDist:" + vertDist + " Slope:" + slope);
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


}
