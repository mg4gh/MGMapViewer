package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;

public class CostCalculatorShortestDist implements IfCostCalcHeuristic, CostCalculator {
    @Override
    public double heuristic(double dist, float vertDist) {
        return dist * 0.999;
    }

    @Override
    public double calcCosts(double dist, double verDist) {
        return dist + 0.0001;
    }
}