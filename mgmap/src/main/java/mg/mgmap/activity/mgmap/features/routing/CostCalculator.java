package mg.mgmap.activity.mgmap.features.routing;

public interface CostCalculator {

    double calcCosts(double dist, double verDist);

    double heuristic( double dist, float vertDist);
}
