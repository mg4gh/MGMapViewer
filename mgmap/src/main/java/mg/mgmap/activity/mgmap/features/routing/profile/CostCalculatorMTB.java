package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;

public class CostCalculatorMTB implements CostCalculator {
    protected double mUpCosts;
    protected double mDnCosts;// base costs in m per hm uphill;
    protected double mUpSlopeLimit; //  up to this slope base Costs
    protected double mDnSlopeLimit;
    protected double mUpAddCosts; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
    protected double mDnAddCosts;
//    double mUpCosts2;
    double mudf;
    double mddf;

    private CostCalculatorHeuristicTwoPieceFunc mProfileCalculator;
    public CostCalculatorMTB(WayAttributs wayTagEval, CostCalculatorHeuristicTwoPieceFunc profile,  double pful, double base_ul ,double base_us,double tful, double base_dl ,double base_ds) {
        mProfileCalculator = profile;
//        fb = mProfileCalculator.fb;
        base_ul = 1.2;
        base_us = 2.0;
        base_dl = 1.2;
        base_ds = 2.0;

        double upSlopeFactor ;
        double fud;
        double ful;
        double fus;

        double dnSlopeFactor ;
        double fdd;
        double fdl;
        double fds;

        double mtbUp = -2;
        double mtbDn = -2;
        if (wayTagEval.mtbscaleUp != null) {
            switch (wayTagEval.mtbscaleUp) {
                case "mtbu_0": mtbUp = 0; break;
                case "mtbu_1": mtbUp = 1; break;
                case "mtbu_2": mtbUp = 2; break;
                case "mtbu_3": mtbUp = 3; break;
                case "mtbu_4": mtbUp = 4; break;
                default:       mtbUp = 5;
            }
        }
        if (wayTagEval.mtbscale != null) {
            switch (wayTagEval.mtbscale ) {
                case "mtbs_0": mtbDn = 0; break;
                case "mtbs_1": mtbDn = 1; break;
                case "mtbs_2": mtbDn = 2; break;
                case "mtbs_3": mtbDn = 3; break;
                default:       mtbDn = 4;
            }
        }
        if ( mtbUp <= 0 ) mtbUp = mtbDn + 1;
        double deltaUp;
        double deltaDn;
        if ("path".equals(wayTagEval.highway)) {
            deltaUp = ( mtbUp >= 0) ? mProfileCalculator.mKlevel - mtbUp : -1;
            fud = 0;
            ful = Math.max(0.8,Math.min(1, Math.pow(base_ul, deltaUp)));
            fus = Math.min(3.0,Math.pow(base_us, -deltaUp-1) * 2.5);

            deltaDn = ( mtbDn >= 0) ? mProfileCalculator.mKlevel - mtbDn : -1;
            fdd = 0;
            fdl = Math.max(0.8,Math.min(1, Math.pow(base_dl, deltaDn)));
            fds = Math.min(4,Math.pow(base_ds, -deltaDn-1) * 1.0);
        } else { //if ("track".equals(wayTagEval.highway)){
            deltaUp = ( mtbUp >= 0) ? mProfileCalculator.mKlevel - mtbUp : 0;
            fud = 0.3;
            ful = Math.max(0.8,Math.min(1, Math.pow(base_ul, deltaUp)));
            fus = Math.min(6,Math.pow(base_us, -deltaUp-1) * 1.6);
            // deltaUp = 0 -> fud = 0.3;ful = 1.0; fus = 0.8;

            deltaDn = ( mtbDn >= 0) ? mProfileCalculator.mKlevel - mtbDn : 0;
            fdd = 0.3;
            fdl = Math.max(0.5,Math.min(1, Math.pow(base_dl, deltaDn)));
            fds = Math.min(6,Math.pow(base_ds, -deltaDn-1) * 1.6);
        }

        mUpSlopeLimit = profile.mUpSlopeLimit * ful;
        mUpCosts =  mProfileCalculator.fu /mUpSlopeLimit;
        upSlopeFactor = 1 + fus;
        mudf = fud + 1.0;//mSlopeShift = fud*fb + 1.0;;
        mUpAddCosts = upSlopeFactor/( mUpSlopeLimit * mUpSlopeLimit);

        mDnSlopeLimit = profile.mDnSlopeLimit * fdl;
        mDnCosts = mProfileCalculator.fd/mDnSlopeLimit ;// base costs in m per hm uphill;
        dnSlopeFactor = 1 + fds;
        mddf = fdd + 1.0;
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
                cost = dist* mudf + vertDist * mUpCosts; // mUpCosts2;
            else
                cost = dist* mudf + vertDist * ( mUpCosts + (slope - mUpSlopeLimit) * mUpAddCosts);
        } else {
            if (slope >= mDnSlopeLimit)
                cost = dist*mddf + vertDist * mDnCosts;
            else
                return dist*mddf + vertDist * ( mDnCosts + (slope - mDnSlopeLimit) * mDnAddCosts);
        }
        return cost + 0.0001;
    }

    @Override
    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }

}
