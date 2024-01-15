package mg.mgmap.activity.mgmap.features.routing.profile;

import android.util.Log;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.generic.graph.GNode;
import mg.mgmap.generic.model.PointModelUtil;

public class GenRoutingProfile extends HightDataRoutingProfile{

    private final WayTagEval mWayTagEval;
    private Way mWay;
    protected GenRoutingProfile(double upCosts, double upSlopeLimit, double upSlopeFactor,
                                double dnCosts, double dnSlopeLimit, double dnSlopeFactor, WayTagEval wayTagEval) {
        super(upCosts, upSlopeLimit, upSlopeFactor,
              dnCosts, dnSlopeLimit, dnSlopeFactor);
        mWayTagEval = wayTagEval;
        mWayTagEval.setmGenRoutingProfile(this);
    }

    public WayTagEval getWayTagEval(){
        return mWayTagEval;
    }
    public double calcCosts(Way way, double dist, double vertDist){
        mWayTagEval.calcFactors(way);

        double costs = super.calcCosts(dist, vertDist,
                mWayTagEval.mGenRoutingProfile.mUpCosts,mWayTagEval.mGenRoutingProfile.mUpSlopeLimit,mWayTagEval.mGenRoutingProfile.mUpAddCosts,
                mWayTagEval.mGenRoutingProfile.mDnCosts,mWayTagEval.mGenRoutingProfile.mDnSlopeLimit,mWayTagEval.mGenRoutingProfile.mDnAddCosts)
                * mWayTagEval.mGenCostFactor;
        Log.d("Genrouting","costFactor:" + mWayTagEval.mGenCostFactor + "  dist:" +dist + "  vertDist" + vertDist + "  cost:" + costs + "***");
        return costs;
    }
}
