package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;

public class CostCalculatorMTB extends CostCalculatorTwoPieceFunc implements CostCalculator {
    protected double mUpCosts;
    protected double mDnCosts;// base costs in m per hm uphill;
    protected double mUpSlopeLimit; //  up to this slope base Costs
    protected double mDnSlopeLimit;
    protected double mUpAddCosts; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
    protected double mDnAddCosts;
    double mUpCosts2;
    double mSlopeShift;
    private CostCalculatorHeuristicTwoPieceFunc mProfileCalculator;
    public CostCalculatorMTB(WayAttributs wayTagEval, CostCalculatorHeuristicTwoPieceFunc profile,  double pful, double pfud ,double pfus,double tful, double tfud ,double tfus) {
        mProfileCalculator = profile;
        double upSlopeFactor = 1;
        double fud;
        double ful;
        double fus;
        double dnSlopeFactor = profile.mDnSlopeFactor;
        double mtbUp = -1;
        if ("path".equals(wayTagEval.highway)) {
            if (wayTagEval.mtbscaleUp != null) {
                switch (wayTagEval.mtbscaleUp) {
                    case "mtbu_0": mtbUp = 0; break;
                    case "mtbu_1": mtbUp = 1; break;
                    case "mtbu_2": mtbUp = 2; break;
                    case "mtbu_3": mtbUp = 3; break;
                    case "mtbu_4": mtbUp = 4; break;
                    default:       mtbUp = 5;
                }
            } else if (wayTagEval.mtbscale != null) {
                switch (wayTagEval.mtbscale ) {
                    case "mtbs_0": mtbUp = 1; break;
                    case "mtbs_1": mtbUp = 2; break;
                    case "mtbs_2": mtbUp = 3; break;
                    case "mtbs_3": mtbUp = 4; break;
                    default:       mtbUp = 5;
                }
            } else {
                mtbUp = mProfileCalculator.mKlevel + 2;
            }
            double deltaLevel = mProfileCalculator.mKlevel - mtbUp;
            fud = 0;
            ful = Math.min(1, Math.pow(pful, deltaLevel));
            fus = Math.pow(pfus, -deltaLevel-1) * ( fa - 1 );
        } else { //if ("track".equals(wayTagEval.highway)){
           fud = tfud;
           ful = tful;
           fus = tfus;
        }

        mUpSlopeLimit = profile.mUpSlopeLimit * ful;
        mUpCosts = fb/mUpSlopeLimit;
        mUpCosts2 = (fb-fud*fb)/mUpSlopeLimit;
        upSlopeFactor = 1 + fus;
        mSlopeShift = fud*fb + 1.0;;

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
