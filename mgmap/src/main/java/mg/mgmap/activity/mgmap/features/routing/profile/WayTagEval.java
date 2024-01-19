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
    protected WayTagEval (  ){

    }

/*    protected WayTagEval(GenRoutingProfile profile){
        setWayCostFactors( profile.mUpCosts,profile.mUpSlopeLimit,profile.mUpAddCosts,
                profile.mDnCosts,profile.mDnSlopeLimit,profile.mDnAddCosts,
                1.0);
    } */


/*    abstract void calcFactors(Way way);
    protected void setWayTags(Way way) {
        if (mWay != way) {
            accessable = false;
            cycleway = null;
            highway = null;
            bicycle = null;
            access = null;
            surface = null;
            tracktype = null;
            mtbscale = null;
            network = null;
            mGenCostFactor = 1.0;
            mMultCostFactor = 1.0;
            trail_visibility = null;
            for (Tag tag : way.tags) {
                switch (tag.key) {
                    case "highway":
                        accessable = true;
                        highway = tag.value;
                        break;
                    case "surface":
                        surface = tag.value;
                        break;
                    case "tracktype":
                        tracktype = tag.value;
                        break;
                    case "network":
                        network = tag.value;
                        break;
                    case "bicycle":
                        bicycle = tag.value;
                        break;
                    case "cycleway":
                    case "cycleway_lane":
                        cycleway = tag.value;
                        break;
                    case "access":
                        access = tag.value;
                        break;
                    case "mtb_scale":
                        mtbscale = tag.value;
                        break;
//               case "name":
//                    if (tag.value.equals("Klingenteichstr") || tag.value.equals("Hohler Kästenbaumweg") ||
//                            tag.value.contains("Klingenteichstr") || tag.value.equals("Königstuhl")) {
//                        log = true;
//                        Log.d("Init Log", tag.value);
//                    }
                }
            }
            if (accessable && ("private".equals(bicycle) || "private".equals(access) ||
                    "motorway".equals(highway) || "trunk".equals(highway)))
                accessable = false;
        }
    } */
}
