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
    private final boolean oneway;
    private final short surfaceCat;


    private final CostCalculatorTwoPieceFunc mProfileCalculator;
    public CostCalculatorTreckingBike(WayAttributs wayTagEval, CostCalculatorTwoPieceFunc profile) {
        mProfileCalculator = profile;
        oneway = wayTagEval.onewayBic;
        double deltaSlope = 0.0;
        double  distFactor ;
        short surfaceCat = TagEval.getSurfaceCat(wayTagEval);
        if (!TagEval.getAccessible(wayTagEval) ){
             distFactor = 10;
             surfaceCat = 2;
        } else {
            if ("path".equals(wayTagEval.highway)) {
                if (surfaceCat <= 1  || (surfaceCat <= 2 && ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network)))) {
                    distFactor = 1.1;
                    deltaSlope = 0;
                } else if ( ("bic_designated".equals(wayTagEval.bicycle) && surfaceCat <= 2 ) ||"lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network)){
                    surfaceCat = ( surfaceCat == 0 ) ? 2 : surfaceCat;
                    distFactor = 1.5;
                    deltaSlope = 1.0;
                } else if ("bic_yes".equals(wayTagEval.bicycle) && surfaceCat <= 2) {
                    distFactor = 1.5;
                    deltaSlope = 1.5;
                } else if ("bic_yes".equals(wayTagEval.bicycle) || "bic_designated".equals(wayTagEval.bicycle)) {
                    surfaceCat = ( surfaceCat == 0 ) ? 3 : surfaceCat;
                    distFactor = 2;
                    deltaSlope = 1.5;
                } else {
                    surfaceCat = ( surfaceCat == 0 ) ? 4 : surfaceCat;
                    distFactor = 10;
                    deltaSlope = 2;
                }
            } else if ("track".equals(wayTagEval.highway) || "unclassified".equals(wayTagEval.highway)) {
                surfaceCat = (surfaceCat>0) ? surfaceCat :4;
                if ( surfaceCat <=1  && ( "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) )) {
                    distFactor = 1.0;
                    deltaSlope = 0;
                } else if ( surfaceCat <=1 || surfaceCat <= 2 && ( "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) )) {
                    distFactor = 1.05;
                    deltaSlope = 0;
                } else if (surfaceCat == 2 || surfaceCat <= 3 && ( "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) )) {
                    distFactor = 1.2;
                    deltaSlope = 0.5;
                } else if (surfaceCat==3 || "bic_designated".equals(wayTagEval.bicycle) || "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network)) {
                    distFactor = 1.8;
                    deltaSlope = 1.0;
                } else {
                    distFactor = 2.5;
                    deltaSlope = 2.5;
                }
            } else if ("steps".equals(wayTagEval.highway)) {
                surfaceCat = 10;
                if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network))
                    distFactor = 5.0;
                else
                    distFactor = 20;
            } else {
                TagEval.Factors factors = TagEval.getFactors(wayTagEval, surfaceCat);
                surfaceCat = factors.surfaceCat;
                distFactor = factors.distFactor;
            }
        }
        this.surfaceCat = surfaceCat;
        mfd =  distFactor;
        mUpSlopeLimit = profile.mUpSlopeLimit * (1-fl*deltaSlope);
        mDelta =  (1-fl*deltaSlope);
        mDnSlopeLimit = profile.mDnSlopeLimit * mDelta;
     }



    public double calcCosts(double dist, float vertDist, boolean primaryDirection){
        if (dist <= 0.0000001 ) {
            return 0.0001;
        }
        double cost;
        double slope = vertDist / dist;
        if (abs(slope) >= 10.0)
           mgLog.e("Suspicious Slope in calcCosts. Dist:" + dist + " VertDist:" + vertDist + " Slope:" + slope);
        double relslope;
        if (slope >= 0) {
            relslope = slope / mUpSlopeLimit;
            if (relslope <= 1)
                cost = dist * (mfd + relslope * CostCalculatorTwoPieceFunc.fu);
            else
                cost = dist * (mfd + relslope * (CostCalculatorTwoPieceFunc.fu + (relslope - 1) * CostCalculatorTwoPieceFunc.fus));
        } else {
            relslope = slope / mDnSlopeLimit;
            if (relslope <= 1)
                cost = dist * (mfd + relslope * mDelta * CostCalculatorTwoPieceFunc.fd);
            else
                cost = dist * (mfd + relslope * mDelta * (CostCalculatorTwoPieceFunc.fd + (relslope - 1) * CostCalculatorTwoPieceFunc.fds));
        }
        if ( oneway && !primaryDirection)
            cost = cost + dist * 5;
        return cost + 0.0001;
    }



    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }

    @Override
    public long getDuration(double dist, float vertDist) {
/*       if (dist >= 0.00001) {
           double slope = vertDist / dist;
           double spm = DurationSplineFunctionFactory.getInst().getDurationSplineFunction(mProfileCalculator.mKlevel,mProfileCalculator.mSlevel,surfaceCat,mProfileCalculator.mBicType).calc(slope);
           double v = 3.6/spm;
           mgLog.d("DurationCalc - Slope:" + slope + " v:" + v + " time:" + spm*dist + " dist:" + dist + " surfaceCat:" + surfaceCat + " mfd:" + mfd);
       } */
       return ( dist >= 0.00001) ? (long) ( 1000 * dist * DurationSplineFunctionFactory.getInst().getDurationSplineFunction(mProfileCalculator.mKlevel,mProfileCalculator.mSlevel,surfaceCat,mProfileCalculator.mBicType).calc(vertDist/dist)) : 0;
    }

}
