package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;
import java.lang.invoke.MethodHandles;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalculatorTreckingBike implements CostCalculator {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static final double fl = 1 - 1/1.3; // reduction of uphill slope limit with deltaSlope
    private final double mUpSlopeLimit;
    private final double mDnSlopeLimit;
    private final double mDelta;
    private final double mfd;


    private final CostCalculatorTwoPieceFunc mProfileCalculator;
    public CostCalculatorTreckingBike(WayAttributs wayTagEval, CostCalculatorTwoPieceFunc profile) {
        mProfileCalculator = profile;

        double deltaSlope = 0.0;
        double  distFactor ;

        if (!wayTagEval.accessible || "private".equals(wayTagEval.access)){
             distFactor = 10;
        } else {
            int surfaceCat = 0;
            if (wayTagEval.surface != null) {
                switch (wayTagEval.surface) {
                    case "asphalt":
                    case "smooth_paved":
                        surfaceCat = 1;
                        break;
                    case "compacted":
                    case "paved":
                    case "fine_gravel":
                    case "paving_stones":
                        surfaceCat = 2;
                        break;
                    default:
                        surfaceCat = 4;
                }
            }
            if ("path".equals(wayTagEval.highway)) {
                surfaceCat = ( surfaceCat == 0 ) ? 3 : surfaceCat;
                if (surfaceCat == 1  || (surfaceCat <= 2 && ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network))))
                    distFactor = 1.0;
                else if ( ("bic_designated".equals(wayTagEval.bicycle) && surfaceCat <= 2 ) ||"lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network)){
                    distFactor = 1.5;
                    deltaSlope = 1.0;
                } else if ("bic_yes".equals(wayTagEval.bicycle) && surfaceCat <= 2) {
                    distFactor = 1.5;
                    deltaSlope = 1.5;
                } else {
                    distFactor = 10;
                    deltaSlope = 2;
                }
            } else if ("track".equals(wayTagEval.highway) || "unclassified".equals(wayTagEval.highway)) {
                int type = 0;
                if (wayTagEval.trackType != null) {
                    switch (wayTagEval.trackType) {
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
                type = (type > 0) ? type : (surfaceCat>0) ? surfaceCat:4;
                if ( type ==1  || type <= 2 && ( "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) )) {
                    distFactor = 1;
                    deltaSlope = 0;
                } else if (type == 2 || type <= 3 && ( "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) )) {
                    distFactor = 1.2;
                    deltaSlope = 0.5;
                } else if (type==3 || "bic_designated".equals(wayTagEval.bicycle) || "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network)) {
                    distFactor = 1.5;
                    deltaSlope = 1.0;
                } else {
                    distFactor = 2.5;
                    deltaSlope = 2.5;
                }
            } else if ("steps".equals(wayTagEval.highway)) {
                if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network))
                    distFactor = 5.0;
                else
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
        if ( abs(slope) >=  10.0 ) mgLog.e("Suspicious Slope in calcCosts. Dist:" + dist + " VertDist:" + vertDist + " Slope:" + slope);
        double cost;
        double relslope;
        if (slope >= 0) {
            relslope = slope / mUpSlopeLimit;
            if (relslope <= 1)
                cost = dist* ( mfd + relslope * CostCalculatorTwoPieceFunc.fu);
            else
                cost = dist* ( mfd + relslope * ( CostCalculatorTwoPieceFunc.fu + (relslope - 1) * CostCalculatorTwoPieceFunc.fus));
        } else {
            relslope = slope / mDnSlopeLimit;
            if (relslope <= 1)
                cost = dist* ( mfd + relslope * mDelta * CostCalculatorTwoPieceFunc.fd);
            else
                cost = dist* ( mfd + relslope * mDelta *( CostCalculatorTwoPieceFunc.fd + (relslope - 1) * CostCalculatorTwoPieceFunc.fds));
        }
        return cost + 0.0001;
    }



    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }

}
