package mg.mgmap.activity.mgmap.features.routing.profile;

public class CostCalculatorShortestDist implements IfCostCalcHeuristic{
    @Override
    public double heuristic(double dist, float vertDist) {
        return dist * 0.999;
    }

    @Override
    public double calcCosts(double dist, double verDist) {
        return dist + 0.0001;
    }
}
