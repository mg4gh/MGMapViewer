package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;

public class CostCalculatorTreckingBike implements CostCalculator {

    private static final double fl = 1 - 1/1.3; // reduction of uphill slope limit with deltaSlope

    /*    protected double mUpCosts;
        protected double mDnCosts;// base costs in m per hm uphill;
        protected double mUpAddCosts; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
        protected double mDnAddCosts; */
    private final double mUpSlopeLimit; //  up to this slope base Costs
    private final double mDnSlopeLimit;
    private final double mDelta;
    private final double mfd;


    private CostCalculatorTwoPieceFunc mProfileCalculator;
    public CostCalculatorTreckingBike(WayAttributs wayTagEval, CostCalculatorTwoPieceFunc profile) {
        mProfileCalculator = profile;

        double deltaSlope = 0.0;
        double  distFactor = 2.0;

        if (!wayTagEval.accessable || "private".equals(wayTagEval.access)){
             distFactor = 10;
        } else {
            int surfaceCat = 0;
            if (wayTagEval.surface != null) {
                switch (wayTagEval.surface) {
                    case "asphalt":
                    case "paved":
                        surfaceCat = 1;
                        break;
                    case "fine_gravel":
                    case "compacted":
                    case "paving_stones":
                        surfaceCat = 2;
                        break;
                    default:
                        surfaceCat = 3;
                }
            }
            if ("path".equals(wayTagEval.highway)) {
                if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) || surfaceCat == 1 )
                    distFactor = 1.0;
                else if ("bic_designated".equals(wayTagEval.bicycle)){ // often bike lanes along big streets
                    distFactor = 1.5;
                    deltaSlope = 1.0;
                } else if ("bic_yes".equals(wayTagEval.bicycle)) {
                    distFactor = 1.5;
                    deltaSlope = 1.5;
                } else {
                    distFactor = 10;
                    deltaSlope = 2;
                }
            } else if ("track".equals(wayTagEval.highway) || "unclassified".equals(wayTagEval.highway)) {
                int type = 0;
                if (wayTagEval.tracktype != null) {
                    switch (wayTagEval.tracktype) {
                        case "grade1":
                            type = 1;
                            break;
                        case "grade2":
                            type = 2;
                            break;
                        case "grade3":
                            type = 3;
                            break;
                        default:
                            type = 4;
                    }
                }
                type = Math.max(type, surfaceCat);
                if ( type ==1  || type <= 2 && ( "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) )) {
                    distFactor = 1;
                    deltaSlope = 0;
                } else if (type == 2 || type <= 3 && ( "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) )) {
                    distFactor = 1.2;
                    deltaSlope = 0.5;
                } else if (type==3) {
                    distFactor = 1.6;
                    deltaSlope = 1.5;
                } else if (type==4) {
                    distFactor = 2;
                    deltaSlope = 2;
                } else if ("bic_designated".equals(wayTagEval.bicycle)) { // often for bike lanes along big streets
                    distFactor = 1.5;
                    deltaSlope = 0.5;
                } else if ("bic_yes".equals(wayTagEval.bicycle) ) {
                    distFactor = 1.5;
                    deltaSlope = 0.5;
                } else {
                    distFactor = 2.5;
                    deltaSlope = 2.5;
                }
            } else if ("steps".equals(wayTagEval.highway)) {
                distFactor = 20;
            } else
                distFactor = CostCalculatorMTB.getDistFactor(wayTagEval) ;
        }

        mfd =  distFactor;
        mUpSlopeLimit = profile.mUpSlopeLimit * (1-fl*deltaSlope);
        mDelta =  (1-fl*deltaSlope);
        mDnSlopeLimit = profile.mDnSlopeLimit * mDelta;
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
                cost = dist* ( mfd + relslope * mProfileCalculator.fu);
            else
                cost = dist* ( mfd + relslope * ( mProfileCalculator.fu + (relslope - 1) * mProfileCalculator.fus));
        } else {
            relslope = slope / mDnSlopeLimit;
            if (relslope <= 1)
                cost = dist* ( mfd + relslope * mDelta * mProfileCalculator.fd);
            else
                cost = dist* ( mfd + relslope * mDelta *( mProfileCalculator.fd + (relslope - 1) * mProfileCalculator.fds));
        }
        return cost + 0.0001;
    }



    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }

}
