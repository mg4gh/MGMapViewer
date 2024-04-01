package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;

public class CostCalculatorHiking implements CostCalculator {

    private static final double vDistFactor = 10.0;
    private final short surfaceLevel;
    private final double distFactor;

    public CostCalculatorHiking(){
       surfaceLevel = 4;
       distFactor = 1.0;
    }
    public CostCalculatorHiking(WayAttributs wayTagEval){
        double distFactor ;
        surfaceLevel = TagEval.getSurfaceCat(wayTagEval);
        if (wayTagEval.highway == null || "acc_no".equals(wayTagEval.access) || "private".equals(wayTagEval.access) || "motorway".equals(wayTagEval.highway)){
            distFactor = 10;
        } else if ("primary".equals(wayTagEval.highway)){
            distFactor = 3;
        } else if ("secondary".equals(wayTagEval.highway)){
            distFactor = 2;
        } else if ("tertiary".equals(wayTagEval.highway)){
            distFactor = 1.5;
        } else
            distFactor = 1.0;
        this.distFactor = distFactor;
    }

                              @Override
    public double heuristic(double dist, float vertDist) {
        if (vertDist < 0)
            return dist  * 0.999;
        else
          return ( dist + vDistFactor*vertDist ) * 0.999;
    }

    @Override
    public double calcCosts(double dist, float vertDist, boolean primaryDirection) {
        if (vertDist < 0)
           return dist*distFactor + 0.0001;
        else
           return dist*distFactor + vDistFactor*vertDist+ 0.0001;
    }

    @Override

    public long getDuration(double dist, float vertDist) {
/*        if (dist >= 0.00001) {
           double slope = vertDist / dist;
           double spm = DurationSplineFunctionFactory.getInst().getDurationSplineFunction(mProfileCalculator.mKlevel,mProfileCalculator.mSlevel,surfaceCat,mProfileCalculator.mBicType).calc(slope);
           double v = 3.6/spm;
           mgLog.d("DurationCalc - Slope:" + slope + " v:" + v + " spm:" + spm + " dist:" + dist + " surfaceCat:" + surfaceCat + " mfd:" + mfdd);
       } */
        return ( dist >= 0.00001) ? (long) ( 1000 * dist * DurationSplineFunctionFactory.getInst().getDurationSplineFunction((short) 0,(short) 0,surfaceLevel,(short) 0).calc(vertDist/dist)) : 0;
    }
}