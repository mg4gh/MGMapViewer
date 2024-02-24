package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;

public class CostCalculatorSimple implements CostCalculator {
    private final double mVertDistCostFactor;

    protected CostCalculatorSimple(double vertDistCostFactor){
        mVertDistCostFactor = vertDistCostFactor;
    }

    public double heuristic(double dist, float vertDist) {
        if (vertDist > 0){
            return (dist + vertDist*mVertDistCostFactor) * 0.999;
        }
        return dist*0.999;
    }

    @Override
    public double calcCosts(double dist, float vertDist, boolean primaryDirection) {
        if (vertDist > 0){
            return dist + vertDist*mVertDistCostFactor + 0.0001;
        }
        return dist + 0.0001;
    }
}
