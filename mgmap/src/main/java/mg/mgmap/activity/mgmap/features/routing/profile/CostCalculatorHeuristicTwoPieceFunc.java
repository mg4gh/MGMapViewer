package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;

public class CostCalculatorHeuristicTwoPieceFunc implements CostCalculator {




    private final static double bs_u = 1.2;// between 1.1 and 1.3; with 1 -> fus will be one , with 1.3 fus ~ 3 (depending on fu). Defines curve shape.
    // With 1 no kink in the curve at up slope limit (Not robust with respect to heurisic!). With 1.3 sharp kink. Factor has little to no effect on overall routing results!
    private final static double bref_ul = 0.077; // base ref up limit slope for bs_u = 1; between 0.06 and 0.09
    protected final static double fu = 1.2; //  product of costs per hm uphill slope limit; between 0.8 and 1.4
    private final static double base_ul = 1.3; // factor of change in uphill slope limit per mtbscaleUp
    private final static double bb_ul = 1/bs_u;
    protected final static double fus = (fu + 1 - bb_ul*fu)/(2*bb_ul*bb_ul-bb_ul ); // up slope limit. See comment bs_u
    private final static double ref_ul = bref_ul*bs_u; // reference up slope limit for given value of bs_u. See comment of bs_u
    // factor increase base limit


    protected final static double fd = -0.2; // between 0 and -0.3. With smaller (bigger absolute) value, lower slope in general preferred
    private final static double bref_dl = -0.14; // reference down slope limit. Starting point of down slope limit  for fd = 0 and slevel = 1;
    private final static double bref_fds = 1.75; // between 1.5 and 3, defines slope beyond down slope limit
    protected final double base_dl = 1.35; // factor of change in downhill limit per mtbscale
    private static double aprox_fds_fact = 2.333; // do not change!!
    protected final static double fds = bref_fds + aprox_fds_fact*fd; // derived scaling factor for slope
    protected final static double corr_dl_fact = fds/(fds-fd); // make sure that intersection with cost = 1 is kept at the same slope with smaller fd
    private final static double ref_dl = corr_dl_fact*bref_dl; // derived down limit slope at fd < 0, effective slope after down slope limit is kept constant
    protected double mUpCosts;
    protected double mDnCosts;// base costs in m per hm uphill;
    protected double mUpSlopeLimit; //  up to this slope base Costs
    protected double mDnSlopeLimit;
    protected double mUpAddCosts; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
    protected double mDnAddCosts;

    protected double mUpSlopeFactor;
    protected double mDnSlopeFactor;
    protected double mKlevel;

    protected double mSlevel;

    protected CostCalculatorHeuristicTwoPieceFunc( double kLevel, double sLevel, double dnSlopeLimit, double dnSlopeFactor) {
        mKlevel = kLevel;
        mSlevel = sLevel;
        mUpSlopeLimit = ref_ul * Math.pow(base_ul,mKlevel-1);
        mUpCosts      = fu /mUpSlopeLimit;
        mUpSlopeFactor = fus;
        mUpAddCosts = mUpSlopeFactor/( mUpSlopeLimit * mUpSlopeLimit);

        mDnSlopeLimit = ref_dl * Math.pow(base_dl,mSlevel-1);
        mDnCosts      = fd/mDnSlopeLimit;
        mDnSlopeFactor = fds;
        mDnAddCosts = mDnSlopeFactor/( mDnSlopeLimit * mDnSlopeLimit) ;

    }

    public double calcCosts(double dist, float vertDist){
        if (dist <= 0.0000001 ) {
            return 0.0001;
        }
        double slope = vertDist / dist;
        if ( abs(slope) >= 10 ) Log.e("CostCalculatorTwoPieceFunc","Suspicious Slope in calcCosts. Dist:" + dist + " VertDist:" + vertDist + " Slope:" + slope);
        double cost;
        if (slope >= 0) {
            if (slope <= mUpSlopeLimit)
                cost = dist + vertDist * mUpCosts;
            else
                cost = dist + vertDist * ( mUpCosts + (slope - mUpSlopeLimit) * mUpAddCosts);
        } else {
            if (slope >= mDnSlopeLimit)
                cost = dist + vertDist * mDnCosts;
            else
                return dist + vertDist * ( mDnCosts + (slope - mDnSlopeLimit) * mDnAddCosts);
        }
        return cost + 0.0001;
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
        if ( abs(slope) >= 10 ) Log.e("CostCalculatorTwoPieceFunc","Suspicious Slope in heuristic. Dist:" + dist + " VertDist:" + vertDist + " Slope:" + slope);
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
