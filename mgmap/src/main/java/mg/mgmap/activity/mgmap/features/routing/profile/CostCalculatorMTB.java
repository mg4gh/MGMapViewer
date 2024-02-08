package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;

public class CostCalculatorMTB implements CostCalculator {

    private static final double ful = 1 - 0.833; // reduction of uphill slope limit with delta mtbscaleUp
    private static final double fdl = 1 - 0.833; // reduction of downhill slope limit with delta mtbscale
/*    protected double mUpCosts;
    protected double mDnCosts;// base costs in m per hm uphill;
    protected double mUpAddCosts; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
    protected double mDnAddCosts; */
    private final double mUpSlopeLimit; //  up to this slope base Costs
    private final double mDnSlopeLimit;
    private final double mDelta;
    private final double mfud;
    private final double mfdd;

    private CostCalculatorTwoPieceFunc mProfileCalculator;
    public CostCalculatorMTB(WayAttributs wayTagEval, CostCalculatorTwoPieceFunc profile) {
        mProfileCalculator = profile;
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
        double deltaUp = Math.min(1.5,Math.max(0,mtbUp - mProfileCalculator.mKlevel));
        double deltaDn = Math.min(1.5,Math.max(0,mtbDn - mProfileCalculator.mSlevel));
        if ("path".equals(wayTagEval.highway)) {
            mfud = 1;
            if (mtbUp<0) deltaUp = 1;
            mfdd = 1;
            if (mtbDn<0) deltaDn = 1;
        } else { //if ("track".equals(wayTagEval.highway)){
            mfud = 1.3;
            mfdd = 1.65;
        }

        mUpSlopeLimit = profile.mUpSlopeLimit * (1-ful*deltaUp);
/*        mUpAddCosts = mProfileCalculator.fus/( mUpSlopeLimit * mUpSlopeLimit);
        mUpCosts =  mProfileCalculator.fu /mUpSlopeLimit; //
*/
        mDelta =  (1-fdl*deltaDn);
        mDnSlopeLimit = profile.mDnSlopeLimit * mDelta;
/*        mDnAddCosts = mProfileCalculator.fds/( mDnSlopeLimit * mDnSlopeLimit) ;
        mDnCosts = mProfileCalculator.mDnCosts ;// attention! Need to be different approach than upCosts to avoid conflict with heuristic
*/
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
                cost = dist* ( mfud + relslope * mProfileCalculator.fu);
            else
                cost = dist* ( mfud + relslope * ( mProfileCalculator.fu + (relslope - 1) * mProfileCalculator.fus));
        } else {
            relslope = slope / mDnSlopeLimit;
            if (relslope <= 1)
                cost = dist* ( mfdd + relslope * mDelta * mProfileCalculator.fd);
            else
                cost = dist* ( mfdd + relslope * mDelta *( mProfileCalculator.fd + (relslope - 1) * mProfileCalculator.fds));
        }
        return cost + 0.0001;
    }

/*    public double calcCostsOld(double dist, float vertDist){
        if (dist <= 0.0000001 ) {
            return 0.0001;
        }
        double slope = vertDist / dist;
        if ( abs(slope) >=  10.0 ) Log.e("CostCalcMTB","Suspicious Slope in calcCosts. Dist:" + dist + " VertDist:" + vertDist + " Slope:" + slope);
        double cost;
        if (slope >= 0) {
            if (slope <= mUpSlopeLimit)
                cost = dist* mfud + vertDist * mUpCosts; // mUpCosts2;
            else
                cost = dist* mfud + vertDist * ( mUpCosts + (slope - mUpSlopeLimit) * mUpAddCosts);
        } else {
            if (slope >= mDnSlopeLimit)
                cost = dist* mfdd + vertDist * mDnCosts;
            else
                cost = dist* mfdd + vertDist * ( mDnCosts + (slope - mDnSlopeLimit) * mDnAddCosts);
        }
        return cost + 0.0001;
    } */


    @Override
    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }

}
