package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

public class CostCalculatorForProfile extends TwoPieceFuncCostCalculator {
    protected double mUpSlopeFactor;
    protected double mDnSlopeFactor;
    protected CostCalculatorForProfile(double upCosts, double upSlopeLimit, double upSlopeFactor, double dnCosts, double dnSlopeLimit, double dnSlopeFactor) {
        mUpCosts      = abs(upCosts); // required. Otherwise heuristic no longer correct
        mUpSlopeLimit = abs(upSlopeLimit); //mathematically not required, but only in this way meaningful. Uphill means additional costs.
        if (upSlopeFactor < 1 ) upSlopeFactor = 1; // required. Otherwise heuristic no longer correct
        mUpAddCosts = upSlopeFactor/( mUpSlopeLimit * mUpSlopeLimit);
        mDnCosts      = dnCosts;
        mDnSlopeLimit = -abs(dnSlopeLimit);
        if (dnSlopeFactor < 1 ) dnSlopeFactor = 1;
        // required. Otherwise heuristic no longer correct
        mDnAddCosts = dnSlopeFactor/( mDnSlopeLimit * mDnSlopeLimit) ;
        mUpSlopeFactor = upSlopeFactor;
        mDnSlopeFactor = dnSlopeFactor;
    }

}
