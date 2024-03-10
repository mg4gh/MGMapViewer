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
    private final short surfaceCat;
    private final CostCalculatorTwoPieceFunc mProfileCalculator;
    private final boolean oneway;

    public CostCalculatorMTB(WayAttributs wayTagEval, CostCalculatorTwoPieceFunc profile) {
        mProfileCalculator = profile;
        oneway = wayTagEval.onewayBic;
        double mtbUp = -2;
        double mtbDn = -2;
        double mfud = 1;
        double mfdd = 1;
        double deltaUp = 0;
        double deltaDn = 0;
        short surfaceCat = TagEval.getSurfaceCat(wayTagEval);
//        surfaceCat = ( surfaceCat == 0) ? 4: surfaceCat;
        if (!TagEval.getAccessible(wayTagEval)) {
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
                if ( surfaceCat == 0){
                    surfaceCat = (short) (3 + Math.max(deltaDn,deltaUp));
                }
                if (mtbUp < 0) deltaUp = 1;
                if (mtbDn < 0) deltaDn = 1;

            } else if ("track".equals(wayTagEval.highway) || "unclassified".equals(wayTagEval.highway)) {
                short type = 0;
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
                surfaceCat = (type > 0) ? type : (surfaceCat>0) ? surfaceCat:4;
                mfud = 1.25;
                mfdd = 1.25;
                //noinspection StatementWithEmptyBody
                if (surfaceCat == 3 ) {
                    if (mtbUp < 0) deltaUp = 0.5;
                    if (mtbDn < 0) deltaDn = 0.5;
                } else if (surfaceCat >= 4){
                    if (mtbUp < 0) deltaUp = 1;
                    if (mtbDn < 0) deltaDn = 1;
                }
            } else if ("steps".equals(wayTagEval.highway)) {
                surfaceCat = 10;
                mfud = 15;
                deltaUp = 2;
                if ( mProfileCalculator.mSlevel == 3 )
                    mfdd = 2.5;
                else if (mProfileCalculator.mSlevel == 2)
                    mfdd = 5;
                else
                    mfdd = 15;
                deltaDn = 2;
            } else {
                double distFactor = TagEval.getDistFactor(wayTagEval) ;
                distFactor = ( distFactor <= 1.2 ) ? distFactor : distFactor * 1.2;
                mfud = distFactor;
                mfdd = distFactor;
            }
        }
        this.surfaceCat = surfaceCat;
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
                distFactor = 1.21;
            else if (wayTagEval.cycleway != null || "lcn".equals(wayTagEval.network))
                distFactor = 1.3;
            else
                distFactor = 1.5;
        } else if ("residential".equals(wayTagEval.highway)||"living_street".equals(wayTagEval.highway)) {
            if ("bic_destination".equals(wayTagEval.bicycle) || "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) )
                distFactor = 1.0;
            else
                distFactor = 1.15;
        } else if ("footway".equals(wayTagEval.highway) || "pedestrian".equals(wayTagEval.highway)) {
            if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) )
                distFactor = 1;
            else if ("bic_yes".equals(wayTagEval.bicycle))
                distFactor = 1.5;
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


    public double calcCosts(double dist, float vertDist, boolean primaryDirection){
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
            if (relslope <= 1)
                cost = dist* ( mfdd + relslope * mDelta * CostCalculatorTwoPieceFunc.fd);
            else
                cost = dist* ( mfdd + relslope * mDelta *( CostCalculatorTwoPieceFunc.fd + (relslope - 1) * CostCalculatorTwoPieceFunc.fds));
        }
        if ( oneway && !primaryDirection)
            cost = cost + dist * 5;
        return cost + 0.0001;
    }


    @Override
    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }

    @Override
    public long getDuration(double dist, float vertDist) {
        return ( dist >= 0.00001) ? (long) ( 1000 * dist * DurationSplineFunctionFactory.getInst().getDurationSplineFunction(mProfileCalculator.mKlevel,mProfileCalculator.mSlevel,surfaceCat,(short) 3).calc(vertDist/dist)) : 0;
    }


}
