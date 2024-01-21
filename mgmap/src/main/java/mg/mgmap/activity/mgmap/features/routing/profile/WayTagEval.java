package mg.mgmap.activity.mgmap.features.routing.profile;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.datastore.Way;

import mg.mgmap.generic.graph.WayAttributs;

public class WayTagEval extends WayAttributs {

    boolean accessable = false;
    protected String highway = null;
    protected String bicycle = null;
    protected String access = null;
    protected String cycleway = null;
    protected String surface = null;
    protected String mtbscale = null;
    protected String trail_visibility = null;
    protected String tracktype = null;
    protected String network = null;



    private IfCostCalculator mCostCalculator;


    public WayTagEval(Way way) {

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
                "motorway".equals(highway) || "trunk".equals(highway))) {
            accessable = false;
        }
    }
    public IfCostCalculator getCostCalculator() {
        return mCostCalculator;
    }

    public void setCostCalculator(IfCostCalculator mCostCalculator) {
        this.mCostCalculator = mCostCalculator;
    }
}
