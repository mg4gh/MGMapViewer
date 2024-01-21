package mg.mgmap.activity.mgmap.features.routing.profile;

public interface IfCostCalcHeuristic extends IfCostCalculator {
    public double heuristic( double dist, float vertDist);
}
