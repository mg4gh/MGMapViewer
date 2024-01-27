package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;

public class CostCalculatorTreckingBike extends CostCalculatorTwoPieceFunc implements CostCalculator {

    double mGenCostFactor = 1.0;
    private CostCalculatorHeuristicTwoPieceFunc mProfileCalculator;
    public CostCalculatorTreckingBike(WayAttributs wayTagEval, CostCalculatorHeuristicTwoPieceFunc profile) {
        mProfileCalculator = profile;
        short surfaceCat = 4;
        double mult = 1.0;
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
            if ("bic_designated".equals(wayTagEval.bicycle))
                mGenCostFactor = 1.0;
            else
                mGenCostFactor = 10;
        } else if ("track".equals(wayTagEval.highway)) {
            if ("grade1".equals(wayTagEval.tracktype) || surfaceCat <= 1 || "bic_yes".equals(wayTagEval.bicycle) || "bic_designated".equals(wayTagEval.bicycle))
                mGenCostFactor = 1;
            else if ("grade2".equals(wayTagEval.tracktype) || surfaceCat <= 2)
                mGenCostFactor = 1.25;
            else if ("grade3".equals(wayTagEval.tracktype) || surfaceCat <= 3)
                mGenCostFactor = 2;
            else
                mGenCostFactor = 3;
        } else if ("primary".equals(wayTagEval.highway) || "primary_link".equals(wayTagEval.highway)) {
            if ("bic_no".equals(wayTagEval.bicycle))
                mGenCostFactor = 10;
            else if (wayTagEval.cycleway != null)
                mGenCostFactor = 1.5;
            else
                mGenCostFactor = 2.5;
        } else if ("secondary".equals(wayTagEval.highway)) {
            if (wayTagEval.cycleway != null)
                mGenCostFactor = 1.0;
            else
                mGenCostFactor = 1.5;
        } else if ("tertiary".equals(wayTagEval.highway)) {
            mGenCostFactor = 1.0;
        } else if ("steps".equals(wayTagEval.highway)) {
            mGenCostFactor = 20;
        } else if ("footway".equals(wayTagEval.highway)){
            if ("bic_yes".equals(wayTagEval.bicycle))
                mGenCostFactor = 1;
            else if ("bic_no".equals(wayTagEval.bicycle))
                mGenCostFactor = 6;
            else
                mGenCostFactor = 3;
        } else if ("service".equals(wayTagEval.highway))
            mGenCostFactor = 1.0;
        if ("lcn".equals(wayTagEval.network)||"rcn".equals(wayTagEval.network)) {
            mult = 0.7;
        }
        mGenCostFactor = Math.max(mGenCostFactor*mult,1);
    }



    public double calcCosts(double dist, float vertDist){
        return mProfileCalculator.calcCosts(dist, vertDist)*mGenCostFactor;
    }


    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }
}
