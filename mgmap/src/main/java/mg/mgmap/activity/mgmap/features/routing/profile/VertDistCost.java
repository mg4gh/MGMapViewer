package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

public class VertDistCost {
    private final double mUpCosts;
    private final double mDnCosts;// base costs in m per hm uphill;
    private final double mUpSlopeLimit; //  up to this slope base Costs
    private final double mDnSlopeLimit;
    private final double mUpAddCosts; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
    //    private final double mMaxSlope = 3.0 ; // Maximal Slope up to which additional costs increase costs per hm
    private final double mDnAddCosts;
    //    static double maxSlope;
    public VertDistCost( int kLevel, int sLevel ){

/*        switch (kLevel) {
            case 1:
                mBaseUpCosts = 6.0;
                mMaxOptUpSlope = 0.2;
                break;
            case 2:
                mBaseUpCosts = 0.0;
                mMaxOptUpSlope = 0.2;
                break;
            case 3:
                mBaseUpCosts =  -0.0001;
                mMaxOptUpSlope = 0.2;
                break;
            default:
                mBaseUpCosts = -1.0;
                mMaxOptUpSlope = 0.2;
        }
        mBaseDownCosts = -mBaseUpCosts;
        mMaxOptDownSlope = -mMaxOptUpSlope; */

        switch (kLevel) {
            case 1:
                mUpCosts = 10.0;
                mUpSlopeLimit = 0.1;
                break;
            case 2:
                mUpCosts = 8.0;
                mUpSlopeLimit = 0.15;
                break;
            case 3:
                mUpCosts = 6.0;
                mUpSlopeLimit = 0.2;
                break;
            default:
                mUpCosts = 4.0;
                mUpSlopeLimit = 0.25;
        }

        switch (sLevel) {
            case 1:
                mDnSlopeLimit = -0.15;
                break;
            case 2:
                mDnSlopeLimit = -0.20;
                break;
            case 3:
                mDnSlopeLimit = -0.25;
                break;
            default:
                mDnSlopeLimit = -0.30;
        }

        mDnCosts = 0.0;

        // reduces costs for down hill, must be smaller than -1/mMaxOptDownSlope. Otherwise negative heuristic!
        mUpAddCosts = 2/( mUpSlopeLimit * mUpSlopeLimit);
        mDnAddCosts = 2/( mDnSlopeLimit * mDnSlopeLimit) ;

    }

    public double getVertDistCosts(double dist, double vertDist) {
        double vertCost;
        double slope = vertDist / dist;
        if (slope > 0) {
            if (slope <= mUpSlopeLimit)
                vertCost = vertDist * mUpCosts;
            else
                vertCost = vertDist * ( mUpCosts + (slope - mUpSlopeLimit) * mUpAddCosts);
        } else {
            if (slope >= mDnSlopeLimit)
                vertCost = vertDist * mDnCosts;
            else
                vertCost = vertDist * ( mDnCosts + (slope - mDnSlopeLimit) * mDnAddCosts);
        }
        return vertCost;
    }


    public double getHeuristic(double dist, double vertDist){
        double heuristic = 0;
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
        return heuristic;
    }

    public double getHeuristicSimple(double dist, double vertDist){
        double slope = vertDist / dist;
        if (slope >= 0){
            return dist + vertDist * mUpCosts;
        } else {
            return dist + vertDist * mDnCosts;
        }
    }

}
