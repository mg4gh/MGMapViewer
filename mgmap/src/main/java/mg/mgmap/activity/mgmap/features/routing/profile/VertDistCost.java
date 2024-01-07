package mg.mgmap.activity.mgmap.features.routing.profile;

import android.util.Log;

public class VertDistCost {
    private final double mBaseCosts; // base costs in m per hm uphill;
    private final double mMaxOptSlope; //  up to this slope base Costs
    private final double mAddCosts  = 10; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
    private final double mMaxSlope = 3.0 ; // Maximal Slope up to which additional costs increase costs per hm

    private final double downfactor = 1.8;
//    static double maxSlope;
    public VertDistCost( int powerLevel, double maxOptSlope){
        mMaxOptSlope = maxOptSlope;
        switch (powerLevel) {
            case 1:
                mBaseCosts = 10.0;
                break;
            case 2:
                mBaseCosts = 8.0;
                break;
            case 3:
                mBaseCosts = 6.0;
                break;
            default:
                mBaseCosts = 4.0;
        }

    }

    protected double getVertDistCosts(double dist, double vertDist) {
        double vertCost;
        double slope = vertDist / dist;
        if (slope > 0) {
            if (slope <= mMaxOptSlope)
                vertCost = vertDist * mBaseCosts;
            else if (slope <= mMaxSlope)
                vertCost = vertDist * mBaseCosts * ( 1 + (slope - mMaxOptSlope) * mAddCosts);
            else {
                vertCost = vertDist * mBaseCosts * ( 1 + (mMaxSlope - mMaxOptSlope) * mAddCosts);
            }
        } else {
            if (slope <= mMaxOptSlope * downfactor)
                vertCost = 0.0;
            else if (slope <= mMaxSlope * downfactor)
                vertCost = vertDist * mBaseCosts * (slope - downfactor*mMaxOptSlope) * mAddCosts;
            else {
                vertCost = vertDist * mBaseCosts * (mMaxSlope - downfactor*mMaxOptSlope) * mAddCosts;
            }
        }
        return vertCost;
    }
}
