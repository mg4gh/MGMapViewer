package mg.mgmap.activity.mgmap.features.routing.profile;

public class CostCalculatorNoTagEval extends CostCalculator {

    private CostCalculatorForProfile mCostCalculatorForProfile;
    public CostCalculatorNoTagEval(CostCalculatorForProfile profile) {
        super();
        mCostCalculatorForProfile = profile;
//        if (genCostFactor > 1) Log.e("Genrouting","genCostFactor" + genCostFactor + " " + this );
    }

    protected double calcCosts( double dist, double vertDist){
        return mCostCalculatorForProfile.calcCosts(dist,vertDist);
    }
}
