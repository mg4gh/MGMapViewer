package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

public class VertDistAlt {

    protected final double mUpCosts;
    protected final double mDnCosts;// base costs in m per hm uphill;
    protected final double mUpSlopeLimit; //  up to this slope base Costs
    protected final double mDnSlopeLimit;
    protected final double mUpAddCosts; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
    protected final double mDnAddCosts;
    //    static double maxSlope;
    public VertDistAlt( ){
        mUpCosts      = abs(8); // required. Otherwise heuristic no longer correct
        mUpSlopeLimit = abs(0.15); //mathematically not required, but only in this way meaningful. Uphill means additional costs.
        mUpAddCosts = 2/( mUpSlopeLimit * mUpSlopeLimit);
        mDnCosts      = 0; // required. Otherwise heuristic no longer correct
        mDnSlopeLimit = -0.27; //can be positive if costs of downhill route should be smaller than distance. If negative, costs are higher than distance.
        mDnAddCosts = 2/( mDnSlopeLimit * mDnSlopeLimit) ;

    }

    public double getVertDistCosts(double dist, double vertDist) {
        if (dist == 0.0 ) return 0.0;
        double vertCost;
        double slope = vertDist / dist;
        if (slope > 0) {
            if (slope <= mUpSlopeLimit)
                vertCost = vertDist * mUpCosts;
            else
                vertCost = vertDist * ( mUpCosts + (slope - mUpSlopeLimit) * mUpAddCosts);
        } else {
//            vertCost = vertDist * mDnCosts;

            if (slope >= mDnSlopeLimit)
                vertCost = vertDist * mDnCosts;
            else {
                vertCost = vertDist * (mDnCosts + (slope - mDnSlopeLimit) * mDnAddCosts);
                Log.e("VertDistAlt","dist:" + dist + " vertDist:" + vertDist + " slope:" + slope + " cost:" + vertCost + "+++");
            }
        }
//        Log.d("VertDistAlt","dist:" + dist + " vertDist:" + vertDist + " slope:" + slope + " VDcost:" + vertCost + "+++");
        return vertCost;
    }

    public double getVertDistHeuristic(double vertDist){
        double vertHeuristic = 0;
        if (vertDist > 0){
            vertHeuristic = vertDist * mUpCosts;
        }
        return vertHeuristic;
    }


}
