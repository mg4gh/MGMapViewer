package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;

public class CostCalculatorMTB extends CostCalculatorTwoPieceFunc implements CostCalculator {
    static double fbs = 0.5;
    protected double mUpCosts;
    protected double mDnCosts;// base costs in m per hm uphill;
    protected double mUpSlopeLimit; //  up to this slope base Costs
    protected double mDnSlopeLimit;
    protected double mUpAddCosts; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
    protected double mDnAddCosts;
    double mUpCosts2;
    double mSlopeShift;
    private CostCalculatorHeuristicTwoPieceFunc mProfileCalculator;
    public CostCalculatorMTB(WayAttributs wayTagEval, CostCalculatorHeuristicTwoPieceFunc profile, double pathSlopeShift, double fbs) {
        mProfileCalculator = profile;
        double upSlopeFactor = 1;
        double dnSlopeFactor = profile.mDnSlopeFactor;
        if ("path".equals(wayTagEval.highway)) {
            mUpSlopeLimit = profile.mUpSlopeLimit * fbs;
            mUpCosts = fb/mUpSlopeLimit;
            mUpCosts2 = mUpCosts;
            upSlopeFactor = fa;
            mSlopeShift = 1.0;
        } else { //if ("track".equals(wayTagEval.highway)){
            mUpSlopeLimit = profile.mUpSlopeLimit;
            mUpCosts = profile.mUpCosts; // * ( ( 1- slopeShift)/(profile.mUpCosts*profile.mUpSlopeLimit) + 1);
            mUpCosts2 = mUpCosts*(1 - pathSlopeShift/fb);//mUpCosts*(1 + (1 - ( mSlopeShift + 1))/fb);
            upSlopeFactor = ( fa + (1-fa)/fb*pathSlopeShift);
            mSlopeShift = pathSlopeShift + 1.0;
        }

        mDnCosts = profile.mDnCosts ;// base costs in m per hm uphill;
        mDnSlopeLimit = profile.mDnSlopeLimit;
        if (upSlopeFactor <1) upSlopeFactor = 1;
        mUpAddCosts = upSlopeFactor/( mUpSlopeLimit * mUpSlopeLimit);
        mDnAddCosts = dnSlopeFactor/( mDnSlopeLimit * mDnSlopeLimit) ;

/*        for ( float vd = 0.0f; vd < 0.3f; vd = vd+0.001f){
            Log.d("CostCalcMTB", "slopeShift:" + mSlopeShift + " vertDist:" + vd + " Cost:" + calcCosts(1.0,vd));
        } */

//        if (genCostFactor > 1) Log.e("Genrouting","genCostFactor" + genCostFactor + " " + this );
    }

    public double calcCosts(double dist, float vertDist){
        if (dist <= 0.0000001 ) {
            return 0.0001;
        }
        double slope = vertDist / dist;
        if ( abs(slope) >=  10.0 ) Log.e("CostCalcMTB","Suspicious Slope in calcCosts. Dist:" + dist + " VertDist:" + vertDist + " Slope:" + slope);
        double cost;
        if (slope >= 0) {
            if (slope <= mUpSlopeLimit)
                cost = dist*mSlopeShift + vertDist * mUpCosts2;
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

    @Override
    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }

}
