package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineDoubleTreckingBike implements CostCalculator {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private final double mfd;
    private final boolean oneway;
    private final CubicSplineDouble surfaceCatSpline;
    private final short surfaceCat;
    private final CostCalcSplineDouble mProfileCalculator;

    public CostCalcSplineDoubleTreckingBike(WayAttributs wayTagEval, CostCalcSplineDouble profile) {
        mProfileCalculator = profile;
        oneway = wayTagEval.onewayBic;

        double  distFactor ;
        short surfaceCat = TagEval.getSurfaceCat(wayTagEval);
        if (TagEval.getNoAccess(wayTagEval)){
            distFactor = 10;
            surfaceCat = (surfaceCat>0) ? surfaceCat :2;
        } else {
            if ("path".equals(wayTagEval.highway)) {
                surfaceCat = (surfaceCat<=0) ? 4: (surfaceCat == 1) ? 2 : surfaceCat;
                if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network)) {
                    distFactor = 1.1;
                } else if ("bic_designated".equals(wayTagEval.bicycle)) {
                    distFactor = 1.5;
                } else if ("bic_yes".equals(wayTagEval.bicycle) ) {
                    distFactor = 1.5;
                } else {
                    distFactor = 2;
                }
            } else if ("track".equals(wayTagEval.highway) || "unclassified".equals(wayTagEval.highway)) {
                surfaceCat = (surfaceCat>0) ? surfaceCat :4;
                if ( "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) ) {
                    distFactor = 1.0;
                    surfaceCat = (surfaceCat>2) ? (short) (surfaceCat-1):surfaceCat;
                } else if ( "bic_designated".equals(wayTagEval.bicycle) ) {
                    distFactor = 1.1;
                } else {
                    distFactor = 1.5;
                }
            } else if ("steps".equals(wayTagEval.highway)) {
                surfaceCat = 6;
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
        if (surfaceCat>6) {
            mgLog.e("Wrong surface Cat:"+ surfaceCat + " ,Tag.highway:" + wayTagEval.highway + " ,Tag.trackType:" + wayTagEval.trackType);
        }
        this.surfaceCat = surfaceCat;
        this.surfaceCatSpline = mProfileCalculator.getDurationSpline(surfaceCat);
        mfd =  distFactor;
    }


    public double calcCosts(double dist, float vertDist, boolean primaryDirection){
        if ( oneway && !primaryDirection)
            return mfd*dist*surfaceCatSpline.calc(vertDist / dist) + dist * 5;
        else
            return mfd*dist*surfaceCatSpline.calc(vertDist / dist);
//        return dist*surfaceCatSpline.calc(vertDist / dist);
    }

    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }


    @Override
    public long getDuration(double dist, float vertDist) {
/*        if (dist >= 0.00001) {
            double slope = vertDist / dist;
            double spm = surfaceCatSpline.calc(slope);
            double v = 3.6/spm;
            mgLog.d("DurationCalc - Slope:" + slope + " v:" + v + " time:" + spm*dist + " dist:" + dist + " surfaceCat:" + surfaceCat + " mfd:" + mfd);
        } */
        return ( dist >= 0.00001) ? (long) ( 1000 * dist * surfaceCatSpline.calc(vertDist/dist)) : 0;
    }

}
