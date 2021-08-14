package mg.mgmap.application.util;

import android.util.Log;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.datastore.Way;

import mg.mgmap.generic.model.PointModel;


public abstract class  CostProviderTagEvalBasic extends CostProviderEle{
    protected String highway;
    protected String bicycle;
    protected String access;
    protected String cycleway;
    protected String surface;
    protected String mtbscale;
    protected String trail_visibility;
    protected String tracktype;
    protected String network;


    @Override
    public void initializeSegment(Way way) {
        super.initializeSegment(way);

//        setMaxUpCostFactor(100);
        accessable = false;

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
//                case "name":
//                    if (tag.value.equals("Klingenteichstr") || tag.value.equals("Hohler Kästenbaumweg") ||
//                            tag.value.contains("Klingenteichstr") || tag.value.equals("Königstuhl")) {
//                        log = true;
//                        Log.d("Init Log", tag.value);
//                    }
            }


        }
//        if (log)
//            if("steps".equals(highway))
//               log = true;
//            else
//               log = false;
        if ( accessable && ( "private".equals(bicycle) || "private".equals(access) || "motorway".equals(highway) || "trunk".equals(highway))) accessable = false;

//        if (accessable && "footway".equals(highway) && "bic_yes".equals(bicycle))
//            log = true;
    }

    public void clearSegment(){
        super.clearSegment();
        highway = null;
        bicycle = null;
        access = null;
        surface = null;
        cycleway = null;
        tracktype = null;
        mtbscale = null;
        network = null;
        trail_visibility = null;
    }
}
