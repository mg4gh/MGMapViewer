package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;

public class CostCalculatorTwoPieceFunc implements CostCalculator {

    // uphill Parameters
    // parameters which determine curve shape
    protected final static double fu = 1.2; //  product of costs per hm uphill slope limit; between 0.8 and 1.4
    private final static double bs_u = 1.2;// between 1.1 and 1.3; with 1 -> fus will be one , with 1.3 fus ~ 3 (depending on fu). Defines curve shape.
    // With 1 no kink in the curve at up slope limit (Not robust with respect to heuristic!). With 1.3 sharp kink. Factor has little to no effect on overall routing results!
    protected final static double fus = (fu + 1 - fu/bs_u)/(2/(bs_u*bs_u)-1/bs_u); // up slope limit. See comment bs_u

    // parameters, which determine kLevel
    private final static double bref_ul = 0.077; // base ref up limit slope for bs_u = 1; between 0.06 and 0.09
    protected final static double base_ul = 1.3; // factor of change in uphill slope limit per mtbscaleUp
    protected final static double ref_ul =bs_u * bref_ul; // reference up slope limit for given value of bs_u. See comment of bs_u
    // factor increase base limit

    //downhill Parameters
    // parameters which determine curve shape
    protected final static double fd = -0.2; // between 0 and -0.3. With smaller (bigger absolute) value, lower slope in general preferred
    private final static double bref_fds = 1.75; // between 1.5 and 3, defines slope beyond down slope limit
    protected final static double fds = bref_fds + 2.333*fd; // don't change! derived scaling factor for slope, correction with 2.3333*fd achieves a approximately constant slope independent of fd. It is a linear approximation


    // parameters, which determine sLevel
    private final static double bref_dl = -0.14; // reference down slope limit. Starting point of down slope limit  for fd = 0 and slevel = 1;
    protected final static double base_dl = 1.35; // factor of change in downhill limit per mtbscale
    protected final static double ref_dl = fds/(fds-fd)*bref_dl; // derived down limit slope at fd < 0, effective slope after down slope limit is kept constant
    // make sure that intersection with cost = 1 is kept at the same slope with smaller fd

    protected final double mUpSlopeLimit; //  up to this slope base Costs
    protected final double mDnSlopeLimit;
    protected final double mKlevel;
    protected final double mSlevel;
/*
    protected double mUpCosts;
    protected double mDnCosts;// base costs in m per hm uphill;
    protected double mUpAddCosts; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
    protected double mDnAddCosts;
    protected double mUpSlopeFactor;
    protected double mDnSlopeFactor; */


    protected CostCalculatorTwoPieceFunc(double kLevel, double sLevel) {
        mKlevel = kLevel;
        mSlevel = sLevel;
        mUpSlopeLimit = ref_ul * Math.pow(base_ul,mKlevel-1);
        mDnSlopeLimit = ref_dl * Math.pow(base_dl,mSlevel-1);
    }



    public double calcCosts(double dist, float vertDist){
        if (dist <= 0.0000001 ) {
            return 0.0001;
        }
        double slope = vertDist / dist;
        if ( abs(slope) >=  10.0 ) Log.e("CostCalcMTB","Suspicious Slope in calcCosts. Dist:" + dist + " VertDist:" + vertDist + " Slope:" + slope);
        double cost;
        double relslope;
        if (slope >= 0) {
            relslope = slope / mUpSlopeLimit;
            if (relslope <= 1)
                cost = dist* ( 1 + relslope * fu);
            else
                cost = dist* ( 1 + relslope * ( fu + (relslope - 1) * fus));
        } else {
            relslope = slope / mDnSlopeLimit;
            if (relslope <= 1)
                cost = dist* ( 1 + relslope * fd);
            else
                cost = dist* ( 1 + relslope * ( fd + (relslope - 1) * fds));
        }
        return cost + 0.0001;
    }


/*      in original constructor
        mUpCosts      = fu /mUpSlopeLimit;
        mUpSlopeFactor = fus;
        mUpAddCosts = mUpSlopeFactor/( mUpSlopeLimit * mUpSlopeLimit);
        mDnCosts      = fd/mDnSlopeLimit;
        mDnSlopeFactor = fds;
        mDnAddCosts = mDnSlopeFactor/( mDnSlopeLimit * mDnSlopeLimit) ;

        public double calcCostsold(double dist, float vertDist){
        if (dist <= 0.0000001 ) {
            return 0.0001;
        }
        double slope = vertDist / dist;
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
    derivation of the heuristic only for positive slope values, so target is uphill (for negative in principle the same, but a bit more cumbersome )
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

    public double heuristicold(double dist, float vertDist){
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
    } */

    public double heuristic(double dist, float vertDist){
        if (dist <= 0.0000001 ) {
            return 0.0;
        }
        double heuristic;
        double slope = vertDist / dist;
        double relslope;
        if ( abs(slope) >= 10 ) Log.e("CostCalculatorTwoPieceFunc","Suspicious Slope in heuristic. Dist:" + dist + " VertDist:" + vertDist + " Slope:" + slope);
        if (slope >= 0){
            relslope = slope / mUpSlopeLimit;
            if (relslope <= 1)
                heuristic = dist*(1+relslope*fu);
            else
                heuristic = dist*relslope*(1+fu); // vertDist/mMaxOptUpSlope is the distance to the target if slope = mMaxOptUpSlope!
        } else {
            relslope = slope / mDnSlopeLimit;
            if (relslope <= 1)
                heuristic = dist*(1+relslope*fd);
            else
                heuristic = dist*relslope*(1+fd);  // => heuristic strictly >= 0 => 1/mMaxOptDownSlope + mBaseDownCosts > 0 =>  1/mMaxOptDownSlope > -mBaseDownCosts => -1/mMaxOptDownSlope > mBaseDownCosts
        }
        return heuristic * 0.999;
    }




}
