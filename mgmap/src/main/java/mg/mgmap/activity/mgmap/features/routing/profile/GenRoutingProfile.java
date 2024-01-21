package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public abstract class GenRoutingProfile extends RoutingProfile {

    protected IfCostCalcHeuristic mCostCalculatorForProfile;


    protected GenRoutingProfile(IfCostCalcHeuristic costCalculatorForProfile){
       mCostCalculatorForProfile = costCalculatorForProfile;
    }

    public final WayAttributs getWayAttributes(Way way){
        return new WayTagEval(way);
    }

    // Intended to be overwritten. Default implementation, if Cost Calculator does not have to evaluate tags, simply use the same calculator as for profile.
    public void refreshWayAttributes(WayAttributs wayAttributs) {
        if (wayAttributs instanceof WayTagEval ) {
            WayTagEval wayTagEval = (WayTagEval) wayAttributs;
            wayTagEval.setCostCalculator(mCostCalculatorForProfile);
        }
    }

    @Override
    public final double getCost(WayAttributs wayAttributs, double dist, float vertDist) {
//        Log.d("Genrouting", "class" + wayAttributs.getClass().getName() + " " + wayAttributs );
        double costs;
        if ( !(wayAttributs instanceof WayTagEval))
            costs = mCostCalculatorForProfile.calcCosts(dist, vertDist);
        else {
            WayTagEval wayTagEval = (WayTagEval) wayAttributs;
            costs = wayTagEval.getCostCalculator().calcCosts(dist, vertDist);
//            if (wayTagEval.mGenCostFactor > 1 )
//              Log.e("traget path","costFactor:" + wayTagEval.mGenCostFactor + "  dist:" +dist + "  vertDist" + vertDist + "  cost:" + costs + "***");
        }
        return costs;
     }




    protected final double heuristic(double dist, float vertDist){
      return mCostCalculatorForProfile.heuristic(dist,vertDist);
    }

    /* Any modified cost function must be larger or equal to the base cost function, which is the foundation for the heuristic. Used to verify/correct cost factors derived from way tags */
/*    protected void checkSetCostFactors(WayTagEval wayTagEval,double upSlopeFactor, double dnSlopeFactor){
        if (wayTagEval.mUpCosts < mUpCosts) {
            wayTagEval.mUpCosts = mUpCosts;
//            Log.e("TagEval","UpCosts to small");
        }
        if (wayTagEval.mUpSlopeLimit < mUpSlopeLimit) {
            wayTagEval.mUpSlopeLimit = mUpSlopeLimit;
//            Log.e("TagEval","UpCosts to small");
        }
        if (wayTagEval.mDnCosts < mDnCosts) {
            wayTagEval.mDnCosts = mDnCosts;
//            Log.e("TagEval","UpCosts to small");
        }
        if (wayTagEval.mDnSlopeLimit > mDnSlopeLimit) {
            wayTagEval.mDnSlopeLimit = mDnSlopeLimit;
//            Log.e("TagEval","UpCosts to small");
        }
        if (upSlopeFactor < 1) upSlopeFactor =1;
        wayTagEval.mUpAddCosts = upSlopeFactor/( mUpSlopeLimit * mUpSlopeLimit);
        if (dnSlopeFactor < 1) dnSlopeFactor = 1;
        wayTagEval.mDnAddCosts = dnSlopeFactor/( mDnSlopeLimit * mDnSlopeLimit) ;
        if (wayTagEval.mGenCostFactor < 1 ) wayTagEval.mGenCostFactor = 1;
    } */

}
