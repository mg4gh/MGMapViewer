package mg.mgmap.generic.graph;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.datastore.Way;

public class WayAttributs {

    public boolean accessible = false;
    public String highway = null;
    public String bicycle = null;
    public String access = null;
    public String cycleway = null;
    public String surface = null;
    public String mtbScale = null;
    public String mtbScaleUp = null;

    public String trackType = null;
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
                    accessible = true;
                    highway = tag.value;
                    break;
                case "surface":
                    surface = tag.value;
                    break;
                case "tracktype":
                    trackType = tag.value;
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
                    mtbScale = tag.value;
                    break;
                case "mtb_scale_uphill":
                    mtbScaleUp = tag.value;
                    break;
/*                case "name":
                    name = tag.value;
                    break;
                case "service":
                    service = tag.value; */
            }
        }
        if (accessible && ("private".equals(bicycle) || (("private".equals(access)|| "no".equals(access))&&!("bic_yes".equals(bicycle)||"bic_permissive".equals(bicycle))) ||
                "motorway".equals(highway) || "trunk".equals(highway))) {
            accessible = false;
        }
    }

    public Object getDerivedData() {
        return derivedData;
    }

    public void setDerivedData(Object derivedData) {
        this.derivedData = derivedData;
    }
}
