package mg.mgmap.generic.graph;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.datastore.Way;

public class WayAttributs {

    public boolean accessable = false;
    public String highway = null;
    public String bicycle = null;
    public String access = null;
    public String cycleway = null;
    public String surface = null;
    public String mtbscale = null;
    public String mtbscaleUp = null;

    public String tracktype = null;
    public String network = null;
//    additional tags that might be used in future
//    public String service = null;
//    public String name = null;
//    public String trail_visibility = null;



    private Object derivedData; // profile specific, will be reset on profile change

    public WayAttributs(Way way) {

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
                case "cycleway_track":
                case "cycleway_lane":
                    cycleway = tag.value;
                    break;
                case "access":
                    access = tag.value;
                    break;
                case "mtb_scale":
                    mtbscale = tag.value;
                    break;
                case "mtb_scale_uphill":
                    mtbscaleUp  = tag.value;
                    break;
/*                case "name":
                    name = tag.value;
                    break;
                case "service":
                    service = tag.value; */
            }
        }
        if (accessable && ("private".equals(bicycle) || "private".equals(access) ||
                "motorway".equals(highway) || "trunk".equals(highway))) {
            accessable = false;
        }
    }

    public Object getDerivedData() {
        return derivedData;
    }

    public void setDerivedData(Object derivedData) {
        this.derivedData = derivedData;
    }
}
