package mg.mgmap.activity.mgmap.features.routing.profile;

import android.util.Log;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.datastore.Way;

public class WayTagEvalMTB extends WayTagEval {

    public WayTagEvalMTB(Way way, GenRoutingProfile profile){
        double multCostFactor = 1.0;
        double upSlopeFactor = profile.mUpSlopeFactor;
        double dnSlopeFactor = profile.mDnSlopeFactor;
        boolean accessable = false;
        String highway = null;
        String bicycle = null;
        String access = null;
        String cycleway = null;
        String surface = null;
        String mtbscale = null;
        String trail_visibility = null;
        String tracktype = null;
        String network = null;

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
            }
        }
        if (accessable && ("private".equals(bicycle) || "private".equals(access) ||
                "motorway".equals(highway) || "trunk".equals(highway))){
            accessable = false;
        }


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
