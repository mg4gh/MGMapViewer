package mg.mgmap.activity.mgmap.features.routing.profile;

public class CostCalculatorSimple implements IfCostCalcHeuristic {
    private double mVertDistCostFactor;

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
    public double calcCosts(double dist, float vertDist) {
        if (vertDist > 0){
            return dist + vertDist*mVertDistCostFactor + 0.0001;
        }
        return dist + 0.0001;
    }
}
