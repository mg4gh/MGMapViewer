package mg.mgmap.generic.graph;

import androidx.annotation.NonNull;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.datastore.Way;

import java.util.Objects;

public class WayAttributs {

    public String highway = null;
    public String bicycle = null;
    public String access = null;
    public String cycleway = null;
    public String surface = null;
    public String mtbScale = null;
    public String mtbScaleUp = null;

    public String trackType = null;
    public String network = null;
    public boolean onewayBic;

    private Object derivedData; // profile specific, will be reset on profile change

    long waValue = 0;

    public WayAttributs() {}

    public WayAttributs(Way way) {

        String oneway = null;
        String oneway_bic = null;

        for (Tag tag : way.tags) {
            switch (tag.key) {
                case "highway":
//                    accessible = true;
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
                    cycleway = "set";
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
                case "oneway":
                    oneway = tag.value;
                    break;
                case "oneway:bicycle":
                    oneway_bic = tag.value;
                    break;
            }
        }
        this.onewayBic = ( "yes".equals(oneway) && !"ow_bic_no".equals(oneway_bic));
    }

    public Object getDerivedData() {
        return derivedData;
    }

    public void setDerivedData(Object derivedData) {
        this.derivedData = derivedData;
    }

    @NonNull
    @Override
    public String toString() {
        return Integer.toHexString(hashCode());
    }


    public String toDetailedString() {
        return "WayAttributs{" +
                "hash='" + Integer.toHexString(hashCode()) + "\n"+
                ", highway='" + highway + '\'' +
                ", bicycle='" + bicycle + '\'' +
                ", access='" + access + '\'' +
                ", cycleway='" + cycleway + '\'' +
                ", surface='" + surface + '\'' +
                ", mtbScale='" + mtbScale + '\'' +
                ", mtbScaleUp='" + mtbScaleUp + '\'' +
                ", trackType='" + trackType + '\'' +
                ", network='" + network + '\'' +
                ", onewayBic=" + onewayBic +
                ", derivedData=" + derivedData +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        WayAttributs that = (WayAttributs) o;
        return onewayBic == that.onewayBic && Objects.equals(highway, that.highway) && Objects.equals(bicycle, that.bicycle) && Objects.equals(access, that.access) && Objects.equals(cycleway, that.cycleway) && Objects.equals(surface, that.surface) && Objects.equals(mtbScale, that.mtbScale) && Objects.equals(mtbScaleUp, that.mtbScaleUp) && Objects.equals(trackType, that.trackType) && Objects.equals(network, that.network);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(highway);
        result = 31 * result + Objects.hashCode(bicycle);
        result = 31 * result + Objects.hashCode(access);
        result = 31 * result + Objects.hashCode(cycleway);
        result = 31 * result + Objects.hashCode(surface);
        result = 31 * result + Objects.hashCode(mtbScale);
        result = 31 * result + Objects.hashCode(mtbScaleUp);
        result = 31 * result + Objects.hashCode(trackType);
        result = 31 * result + Objects.hashCode(network);
        result = 31 * result + Boolean.hashCode(onewayBic);
        result = 31 * result + Objects.hashCode(derivedData);
        return result;
    }
}
