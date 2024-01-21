package mg.mgmap.activity.mgmap.features.routing.profile;

public class CostCalculatorNoTagEval extends CostCalculator {

    private ProfileCostCalculator mProfileCostCalculator;
    public CostCalculatorNoTagEval(ProfileCostCalculator profile) {
        super();
        mProfileCostCalculator = profile;
//        if (genCostFactor > 1) Log.e("Genrouting","genCostFactor" + genCostFactor + " " + this );
    }

    protected double calcCosts( double dist, double vertDist){
        return mProfileCostCalculator.calcCosts(dist,vertDist);
    }
}
