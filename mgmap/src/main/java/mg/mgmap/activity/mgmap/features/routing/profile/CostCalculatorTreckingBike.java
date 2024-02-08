package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;

public class CostCalculatorTreckingBike implements CostCalculator {

    double mGenCostFactor = 2.0;
    private CostCalculatorTwoPieceFunc mProfileCalculator;
    public CostCalculatorTreckingBike(WayAttributs wayTagEval, CostCalculatorTwoPieceFunc profile) {
        mProfileCalculator = profile;
        short surfaceCat = 4;
        boolean cycleroute = false;

        if (!wayTagEval.accessable) {
            mGenCostFactor = 10;
            return;
        }

        if ("lcn".equals(wayTagEval.network)||"rcn".equals(wayTagEval.network)||"icn".equals(wayTagEval.network)||
                "bic_designated".equals(wayTagEval.bicycle) ) {
            cycleroute = true;
        }
        if ("path".equals(wayTagEval.highway)) {
            if (cycleroute)
                mGenCostFactor = 1.0;
            else if("bic_yes".equals(wayTagEval.bicycle))
                mGenCostFactor = 1.5;
            else
                mGenCostFactor = 10;
        } else if ("track".equals(wayTagEval.highway)) {
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
            if ("grade1".equals(wayTagEval.tracktype) || surfaceCat <= 1 || "bic_yes".equals(wayTagEval.bicycle) )
                mGenCostFactor = 1;
            else if ("grade2".equals(wayTagEval.tracktype) || surfaceCat <= 2)
                mGenCostFactor = 1.3;
            else if ("grade3".equals(wayTagEval.tracktype) || surfaceCat <= 3)
                mGenCostFactor = 3.0;
            else
                mGenCostFactor = 3;
        } else if("cycleway".equals(wayTagEval.highway)){
            mGenCostFactor = 1.5;
        } else if ("primary".equals(wayTagEval.highway) || "primary_link".equals(wayTagEval.highway)) {
            if ("bic_no".equals(wayTagEval.bicycle))
                mGenCostFactor = 10;
            else if (wayTagEval.cycleway != null || cycleroute)
                mGenCostFactor = 1.7;
            else
                mGenCostFactor = 2.5;
        } else if ("secondary".equals(wayTagEval.highway)) {
            if (wayTagEval.cycleway != null || cycleroute)
                mGenCostFactor = 1.5;
            else
                mGenCostFactor = 2.0;
        } else if ("tertiary".equals(wayTagEval.highway)) {
            mGenCostFactor = 1.5;
        } else if ("residential".equals(wayTagEval.highway)) {
            mGenCostFactor = 1.2;
        } else if ("steps".equals(wayTagEval.highway)) {
            mGenCostFactor = 20;
        } else if ("footway".equals(wayTagEval.highway)||"pedestrian".equals(wayTagEval.highway)){
            if (cycleroute)
                mGenCostFactor = 1;
            else
                mGenCostFactor = 4;
        } else if (cycleroute) {
            mGenCostFactor = 1;
        } else  if ("service".equals(wayTagEval.highway) ) {
            if ("parking_aisle".equals(wayTagEval.service))
                mGenCostFactor=1.5;
            else
                mGenCostFactor = 15;
        }
        if (cycleroute) {
            mGenCostFactor = Math.max(mGenCostFactor*0.7,1);
        }
    }



    public double calcCosts(double dist, float vertDist){
        return mProfileCalculator.calcCosts(dist, vertDist)*mGenCostFactor;
    }


    public double heuristic(double dist, float vertDist) {
        return mProfileCalculator.heuristic(dist, vertDist);
    }
}
