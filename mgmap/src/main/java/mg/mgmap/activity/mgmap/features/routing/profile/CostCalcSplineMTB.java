package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;
import java.util.Locale;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineMTB implements CostCalculator {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private final float mfd;
    private final boolean oneway;
    private final CubicSpline surfaceCatSpline;
    private final short surfaceCat;
    private final CostCalcSplineProfileMTB mProfileCalculator;

    public CostCalcSplineMTB(WayAttributs wayTagEval, CostCalcSplineProfileMTB profile) {
        mProfileCalculator = profile;
        oneway = wayTagEval.onewayBic;

        float  distFactor ;
        short surfaceCat = TagEval.getSurfaceCat(wayTagEval);
        if (TagEval.getNoAccess(wayTagEval)){
            distFactor = 10;
            surfaceCat = (surfaceCat>0) ? surfaceCat :2;
        } else {
            if ("path".equals(wayTagEval.highway)) {
                surfaceCat = (surfaceCat<=0) ? 4: surfaceCat;
                if (surfaceCat == 1){
                    distFactor = 1.2f;
                    surfaceCat = 2;
                } else
                    distFactor = 1f;
            } else if ("track".equals(wayTagEval.highway) || "unclassified".equals(wayTagEval.highway)) {
                surfaceCat = (surfaceCat>0) ? surfaceCat :4;
                distFactor = 1.25f;
            } else if ("steps".equals(wayTagEval.highway)) {
                surfaceCat = 6;
                if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network))
                    distFactor = 1f;
                else
                    distFactor = 2f;
            } else {
                TagEval.Factors factors = TagEval.getFactors(wayTagEval, surfaceCat);
                surfaceCat = factors.surfaceCat;
                distFactor = (float) ((surfaceCat <= 2) ? factors.distFactor*1.3d : factors.distFactor );
            }
        }
        if (surfaceCat>6) {
            mgLog.e("Wrong surface Cat:"+ surfaceCat + " ,Tag.highway:" + wayTagEval.highway + " ,Tag.trackType:" + wayTagEval.trackType);
        }
        this.surfaceCat = surfaceCat;
        this.surfaceCatSpline = mProfileCalculator.getSpline(surfaceCat);
        mfd =  distFactor;
    }


    public double calcCosts(double dist, float vertDist, boolean primaryDirection){
        if ( oneway && !primaryDirection)
            return mfd*dist*surfaceCatSpline.calc(vertDist / (float) dist) + dist * 5;
        else
            return mfd*dist*surfaceCatSpline.calc(vertDist / (float) dist);
    }

    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }


    @Override
    public long getDuration(double dist, float vertDist) {
        if (dist >= 0.00001) {
            float slope = vertDist / (float) dist;
            double spm = surfaceCatSpline.calc(slope);
            double v = 3.6/spm;
            mgLog.d(()-> String.format(Locale.ENGLISH, "DurationCalc: Slope=%.2f v=%.2f time=%.2f dist=%.2f surfaceCat=%s",100f*slope,v,spm*dist,dist,surfaceCat));
        }
        return ( dist >= 0.00001) ? (long) ( 1000 * dist * surfaceCatSpline.calc(vertDist/(float) dist)) : 0;
    }
}
