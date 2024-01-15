package mg.mgmap.activity.mgmap.features.routing.profile;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.datastore.Way;

public abstract class WayTagEval {
    protected boolean accessable;
    protected String highway;
    protected String bicycle;
    protected String access;
    protected String cycleway;
    protected String surface;
    protected String mtbscale;
    protected String trail_visibility;
    protected String tracktype;
    protected String network;
    protected double mGenCostFactor;
    protected double mMultCostFactor;
     protected GenRoutingProfile mGenRoutingProfile;
    private Way mWay;

    protected void setmGenRoutingProfile(GenRoutingProfile genRoutingProfile){
        mGenRoutingProfile = genRoutingProfile;
    }

    abstract void calcFactors(Way way);
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
    }
}
