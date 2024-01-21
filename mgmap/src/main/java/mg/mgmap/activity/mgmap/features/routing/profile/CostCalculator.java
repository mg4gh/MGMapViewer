package mg.mgmap.activity.mgmap.features.routing.profile;

public abstract class CostCalculator {
    protected abstract double calcCosts(double dist, double verDist);
}
