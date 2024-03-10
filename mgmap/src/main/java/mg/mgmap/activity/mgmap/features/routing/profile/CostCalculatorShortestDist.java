package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;

public class CostCalculatorShortestDist implements CostCalculator {
    @Override
    public double heuristic(double dist, float vertDist) {
        return dist * 0.999;
    }


    @Override
    public double calcCosts(double dist, float verDist, boolean primaryDirection) {
        return dist + 0.0001;
    }

    @Override
    public long getDuration(double dist, float vertDist) {
        return (long) (dist*0.36);
    }
}
