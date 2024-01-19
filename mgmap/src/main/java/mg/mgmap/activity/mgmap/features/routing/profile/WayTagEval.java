package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.generic.graph.WayAttributs;

public class WayTagEval extends WayAttributs {

    protected double mUpCosts;
    protected double mDnCosts;// base costs in m per hm uphill;
    protected double mUpSlopeLimit; //  up to this slope base Costs
    protected double mDnSlopeLimit;
    protected double mUpAddCosts; // relative additional costs per slope increase ( 10 means, that costs double with 10% slope increase )
    protected double mDnAddCosts;
    protected double mGenCostFactor;


}
