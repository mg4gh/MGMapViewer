package mg.mgmap.activity.mgmap.features.routing.profile;

public class CostCalculatorMTB extends CostCalculatorTwoPieceFunc{
    double mDistCostFactor = 1.0;
    public CostCalculatorMTB(WayTagEval wayTagEval, CostCalculatorHeuristicTwoPieceFunc profile) {
        super();

        double slopeShift = 1.0;
        double multCostFactor = 1.0;
        double upSlopeFactor = profile.mUpSlopeFactor;
        double dnSlopeFactor = profile.mDnSlopeFactor;
        if (wayTagEval.accessable) {
            if ("path".equals(wayTagEval.highway)) {
                 if (wayTagEval.trail_visibility != null) {
                    switch (wayTagEval.trail_visibility) {
                        case "bad":
                            multCostFactor = 1.5;
                            break;
                        case "horrible":
                        case "no":
                            multCostFactor = 2;
                            break;
                    }
                }
            } else if ("track".equals(wayTagEval.highway)) {
                mDistCostFactor = 1;
                slopeShift = 1.5;
                mDistCostFactor = slopeShift;
            } else if ("primary".equals(wayTagEval.highway)) {
                if ("bic_no".equals(wayTagEval.bicycle))
                    wayTagEval.accessable = false;
                else if (wayTagEval.cycleway != null)
                    mDistCostFactor = 1.5;
                else
                    mDistCostFactor = 2;
            } else if ("secondary".equals(wayTagEval.highway)) {
                if (wayTagEval.cycleway != null)
                    mDistCostFactor = 1.5;
                else
                    mDistCostFactor = 2;
            } else if ("tertiary".equals(wayTagEval.highway)) {
                mDistCostFactor = 1.5;
            } else if ("steps".equals(wayTagEval.highway)) {
                mDistCostFactor = 8;
//                setFixUpDistParameter(8);
//                setFixDownDistParameter(8);
            } else if ("footway".equals(wayTagEval.highway) ) {
                if ( "bic_yes".equals(wayTagEval.bicycle) )
                    mDistCostFactor = 1;
                else
                    mDistCostFactor = 1;
            }
            else if ("bic_no".equals(wayTagEval.bicycle))
                mDistCostFactor = 1;
            else
                mDistCostFactor = 1;
        }
        if (wayTagEval.accessable) mDistCostFactor = 10;

        mDistCostFactor = Math.max( mDistCostFactor * multCostFactor, 1);
        mUpCosts = profile.mUpCosts * ( ( 1- slopeShift)/(profile.mUpCosts*profile.mUpSlopeLimit) + 1);
        mDnCosts = profile.mDnCosts ;// base costs in m per hm uphill;
        mUpSlopeLimit = profile.mUpSlopeLimit; //  up to this slope base Costs
        mDnSlopeLimit = profile.mDnSlopeLimit;
        mUpAddCosts = upSlopeFactor/( mUpSlopeLimit * mUpSlopeLimit);
        mDnAddCosts = dnSlopeFactor/( mDnSlopeLimit * mDnSlopeLimit) ;


//        if (genCostFactor > 1) Log.e("Genrouting","genCostFactor" + genCostFactor + " " + this );
    }

    public double calcCosts(double dist, float vertDist){
        return super.calcCosts( dist*mDistCostFactor, vertDist);
    }

}
