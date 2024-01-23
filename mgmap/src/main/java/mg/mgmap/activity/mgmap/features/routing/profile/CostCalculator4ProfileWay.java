package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.graph.WayAttributs;

public class CostCalculator4ProfileWay implements CostCalculator {

    double mGenCostFactor = 1.0;
    CostCalculator profileCalculator;

    public CostCalculator4ProfileWay(CostCalculator profileCalculator, WayAttributs wayTagEval){
        this.profileCalculator = profileCalculator;
        double multCostFactor = 1.0;

        if (wayTagEval.accessable) {
            if ("path".equals(wayTagEval.highway)) {
                if (wayTagEval.mtbscale != null) {
                    switch (wayTagEval.mtbscale) {
                        case "mtbs_0":
                        case "mtbs_1":
                            break;
                        case "mtbs_2":
                            mGenCostFactor = 1.5;
                            break;
                        case "mtbs_3":
                        default:
                            mGenCostFactor = 3;
                    }
                } else
                    mGenCostFactor = 3;
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
                mGenCostFactor = 1;
            } else if ("primary".equals(wayTagEval.highway)) {
                if ("bic_no".equals(wayTagEval.bicycle))
                    wayTagEval.accessable = false;
                else if (wayTagEval.cycleway != null)
                    mGenCostFactor = 2;
                else
                    mGenCostFactor = 3;
            } else if ("secondary".equals(wayTagEval.highway)) {
                if (wayTagEval.cycleway != null)
                    mGenCostFactor = 1.5;
                else
                    mGenCostFactor = 2;
            } else if ("tertiary".equals(wayTagEval.highway)) {
                mGenCostFactor = 1.5;
            } else if ("steps".equals(wayTagEval.highway)) {
                mGenCostFactor = 4;
//                setFixUpDistParameter(8);
//                setFixDownDistParameter(8);
            } else if ("footway".equals(wayTagEval.highway) ) {
                if ( "bic_yes".equals(wayTagEval.bicycle) )
                    mGenCostFactor = 1;
                else
                    mGenCostFactor = 4;
            }
            else if ("bic_no".equals(wayTagEval.bicycle))
                mGenCostFactor = 4;
            else
                mGenCostFactor = 1;
        } else
            mGenCostFactor = 10;

        mGenCostFactor = Math.max( mGenCostFactor * multCostFactor, 1);

//        if (genCostFactor > 1) Log.e("Genrouting","genCostFactor" + genCostFactor + " " + this );

    }

    @Override
    public double calcCosts(double dist, double vertDist) {
        return profileCalculator.calcCosts(dist, vertDist) * mGenCostFactor;
    }

    @Override
    public double heuristic(double dist, float vertDist) {
        return profileCalculator.heuristic(dist, vertDist);
    }
}
