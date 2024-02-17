package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalculatorMTB implements CostCalculator {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static final double ful = 1 - 0.833; // reduction of uphill slope limit with delta mtbscaleUp
    private static final double fdl = 1 - 0.833; // reduction of downhill slope limit with delta mtbscale
    private final double mUpSlopeLimit; //  up to this slope base Costs
    private final double mDnSlopeLimit;
    private final double mDelta;
    private final double mfud;
    private final double mfdd;
    private final CostCalculatorTwoPieceFunc mProfileCalculator;

    public CostCalculatorMTB(WayAttributs wayTagEval, CostCalculatorTwoPieceFunc profile) {
        mProfileCalculator = profile;
        double mtbUp = -2;
        double mtbDn = -2;
        double mfud = 1;
        double mfdd = 1;
        double deltaUp = 0;
        double deltaDn = 0;
        if (!wayTagEval.accessible || "private".equals(wayTagEval.access)) {
            mfud = 10;
            mfdd = 10;
        } else {
            if (wayTagEval.mtbScaleUp != null) {
                switch (wayTagEval.mtbScaleUp) {
                    case "mtbu_0":
                        mtbUp = 0;
                        break;
                    case "mtbu_1":
                        mtbUp = 1;
                        break;
                    case "mtbu_2":
                        mtbUp = 2;
                        break;
                    case "mtbu_3":
                        mtbUp = 3;
                        break;
                    case "mtbu_4":
                        mtbUp = 4;
                        break;
                    default:
                        mtbUp = 5;
                }
            }
            if (wayTagEval.mtbScale != null) {
                switch (wayTagEval.mtbScale) {
                    case "mtbs_0":
                        mtbDn = 0;
                        break;
                    case "mtbs_1":
                        mtbDn = 1;
                        break;
                    case "mtbs_2":
                        mtbDn = 2;
                        break;
                    case "mtbs_3":
                        mtbDn = 3;
                        break;
                    default:
                        mtbDn = 4;
                }
            }
            if (mtbUp <= 0) mtbUp = mtbDn + 1;
            if (mtbUp - mProfileCalculator.mKlevel >= 1 )
                deltaUp = Math.sqrt( mtbUp - mProfileCalculator.mKlevel);
            if (mtbDn - mProfileCalculator.mSlevel >= 1 )
                deltaDn = Math.sqrt( mtbDn - mProfileCalculator.mSlevel);
            if ("path".equals(wayTagEval.highway)) {
                if (mtbUp < 0) deltaUp = 1;
                if (mtbDn < 0) deltaDn = 1;
            } else if ("track".equals(wayTagEval.highway) || "unclassified".equals(wayTagEval.highway)) {
                short surfaceCat = 4;
                if (wayTagEval.surface != null) {
                    switch (wayTagEval.surface) {
                        case "asphalt":
                        case "paved":
                        case "smooth_paved":
                            surfaceCat = 1;
                            break;
                        case "fine_gravel":
                        case "compacted":
                        case "paving_stones":
                            surfaceCat = 2;
                            break;
/*   might be used in future ...
                      case "ground":
                        case "dirt":
                        case "earth":
                        case "raw":
                            surfaceCat = 3;
                            break;*/
                    }
                }
                mfud = 1.25;
                mfdd = 1.25;
                //noinspection StatementWithEmptyBody
                if ("grade2".equals(wayTagEval.trackType) || "grade1".equals(wayTagEval.trackType) || surfaceCat <= 2) {
                } else if ("grade3".equals(wayTagEval.trackType) ) {
                    if (mtbUp < 0) deltaUp = 0.5;
                    if (mtbDn < 0) deltaDn = 0.5;
                } else {
                    if (mtbUp < 0) deltaUp = 1;
                    if (mtbDn < 0) deltaDn = 1;
                }
            } else if ("steps".equals(wayTagEval.highway)) {
                mfud = 20;
                deltaUp = 2;
                if ( mProfileCalculator.mSlevel == 3 )
                    mfdd = 2.5;
                else if (mProfileCalculator.mSlevel == 2)
                    mfdd = 5;
                else
                    mfdd = 20;
                deltaDn = 2;
            } else {
                double distFactor = getDistFactor(wayTagEval) ;
                distFactor = ( distFactor == 1) ? 1 : distFactor * 1.2;
                mfud = distFactor;
                mfdd = distFactor;
            }
        }

        this.mfud = mfud;
        this.mfdd = mfdd;

        mUpSlopeLimit = profile.mUpSlopeLimit * (1-ful*deltaUp);
        mDelta =  (1-fdl*deltaDn);
        mDnSlopeLimit = profile.mDnSlopeLimit * mDelta;
    }

    protected static double getDistFactor(WayAttributs wayTagEval ){
        double distFactor ;
        if ("cycleway".equals(wayTagEval.highway)) {
            if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network))
                distFactor = 1;
            else
                distFactor = 1.3;
        } else if ("primary".equals(wayTagEval.highway) || "primary_link".equals(wayTagEval.highway)) {
            if ("bic_no".equals(wayTagEval.bicycle))
                distFactor = 10;
            else if ( "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) || ( wayTagEval.cycleway != null && "lcn".equals(wayTagEval.network)))
                distFactor = 1.5;
            else if (wayTagEval.cycleway != null || "lcn".equals(wayTagEval.network)  )
                distFactor = 1.8;
            else
                distFactor = 2.5;
        } else if ("secondary".equals(wayTagEval.highway)) {
            if ( "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) || ( wayTagEval.cycleway != null && "lcn".equals(wayTagEval.network)))
                distFactor = 1.4;
            else if (wayTagEval.cycleway != null || "lcn".equals(wayTagEval.network) )
                distFactor = 1.6;
            else
                distFactor = 2.0;
        } else if ("tertiary".equals(wayTagEval.highway)) {
            if ( "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) || ( wayTagEval.cycleway != null && "lcn".equals(wayTagEval.network)))
                distFactor = 1.2;
            else if (wayTagEval.cycleway != null || "lcn".equals(wayTagEval.network))
                distFactor = 1.3;
            else
                distFactor = 1.5;
        } else if ("residential".equals(wayTagEval.highway)||"living_street".equals(wayTagEval.highway)) {
            if ("bic_destination".equals(wayTagEval.bicycle) || "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) )
                distFactor = 1.0;
            else
                distFactor = 1.3;
        } else if ("footway".equals(wayTagEval.highway) || "pedestrian".equals(wayTagEval.highway)) {
            if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) || "bic_yes".equals(wayTagEval.bicycle))
                distFactor = 1;
            else if ("bic_no".equals(wayTagEval.bicycle))
                distFactor = 4.0;
            else
                distFactor = 3.0;
        } else if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network)) {
            distFactor = 1;
        } else if ("service".equals(wayTagEval.highway)) {
            if ("bic_destination".equals(wayTagEval.bicycle))
                distFactor = 1.0;
            else if ( "no".equals(wayTagEval.access) || "bic_no".equals(wayTagEval.bicycle))
                distFactor = 15;
            else
                distFactor = 1.5;
        } else
            distFactor = 1.3;
        return distFactor;
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
                cost = dist* ( mfud + relslope * CostCalculatorTwoPieceFunc.fu);
            else
                cost = dist* ( mfud + relslope * ( CostCalculatorTwoPieceFunc.fu + (relslope - 1) * CostCalculatorTwoPieceFunc.fus));
        } else {
            relslope = slope / mDnSlopeLimit;
            if (relslope <= 0.2)
                cost = dist* ( mfud + relslope * mDelta * CostCalculatorTwoPieceFunc.fd);
            else if (relslope <= 1)
                cost = dist* ( mfdd + relslope * mDelta * CostCalculatorTwoPieceFunc.fd);
            else
                cost = dist* ( mfdd + relslope * mDelta *( CostCalculatorTwoPieceFunc.fd + (relslope - 1) * CostCalculatorTwoPieceFunc.fds));
        }
        return cost + 0.0001;
    }


    @Override
    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }



}
