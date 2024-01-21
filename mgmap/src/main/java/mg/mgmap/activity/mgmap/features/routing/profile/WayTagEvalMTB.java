package mg.mgmap.activity.mgmap.features.routing.profile;

import android.util.Log;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.datastore.Way;

public class WayTagEvalMTB extends WayTagEval {


    public WayTagEvalMTB(Way way) {
        super(way);
    }

    public void calcCostFactors(GenRoutingProfile profile){
            double multCostFactor = 1.0;
            double upSlopeFactor = profile.mUpSlopeFactor;
            double dnSlopeFactor = profile.mDnSlopeFactor;
        if (accessable) {
            if ("path".equals(highway)) {
                if (mtbscale != null) {
                    switch (mtbscale) {
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
                if (trail_visibility != null) {
                    switch (trail_visibility) {
                        case "bad":
                            multCostFactor = 1.5;
                            break;
                        case "horrible":
                        case "no":
                            multCostFactor = 2;
                            break;
                    }
                }
            } else if ("track".equals(highway)) {
                mGenCostFactor = 1;
            } else if ("primary".equals(highway)) {
                if ("bic_no".equals(bicycle))
                    accessable = false;
                else if (cycleway != null)
                    mGenCostFactor = 2;
                else
                    mGenCostFactor = 3;
            } else if ("secondary".equals(highway)) {
                if (cycleway != null)
                    mGenCostFactor = 1.5;
                else
                    mGenCostFactor = 2;
            } else if ("tertiary".equals(highway)) {
                mGenCostFactor = 1.5;
            } else if ("steps".equals(highway)) {
                mGenCostFactor = 4;
//                setFixUpDistParameter(8);
//                setFixDownDistParameter(8);
            } else if ("footway".equals(highway) ) {
                if ( "bic_yes".equals(bicycle) )
                    mGenCostFactor = 1;
                else
                    mGenCostFactor = 4;
            }
            else if ("bic_no".equals(bicycle))
                mGenCostFactor = 4;
            else
                mGenCostFactor = 1;
        } else
            mGenCostFactor = 10;

        mGenCostFactor = Math.max( mGenCostFactor * multCostFactor, 1);
        profile.checkSetCostFactors(this,upSlopeFactor, dnSlopeFactor);
//        if (mGenCostFactor > 1) Log.e("Genrouting","genCostFactor" + mGenCostFactor + " " + this );

    }

}
