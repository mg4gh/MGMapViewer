package mg.mgmap.activity.mgmap.features.routing;

public interface CostCalculator {

    double calcCosts(double dist, float verDist, boolean primaryDirection);

    double heuristic( double dist, float vertDist);
}
