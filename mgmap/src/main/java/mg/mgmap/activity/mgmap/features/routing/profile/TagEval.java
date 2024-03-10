package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.generic.graph.WayAttributs;

public class TagEval {
    private TagEval(){};

    protected static boolean getAccessible(WayAttributs wayTagEval) {
        return wayTagEval.highway!= null && ! ("private".equals(wayTagEval.bicycle) || (("private".equals(wayTagEval.access) ||
                "no".equals(wayTagEval.access)) && !("bic_yes".equals(wayTagEval.bicycle) ||
                "bic_designated".equals(wayTagEval.bicycle) || "bic_permissive".equals(wayTagEval.bicycle))) ||
                "motorway".equals(wayTagEval.highway) || "trunk".equals(wayTagEval.highway));
    }

    protected static short getSurfaceCat(WayAttributs wayTagEval){
        short surfaceCat = 0;
        if (wayTagEval.surface != null) {
            switch (wayTagEval.surface) {
                case "asphalt":
                case "smooth_paved":
                    surfaceCat = 1;
                    break;
                case "compacted":
                case "paved":
                case "rough_paved":
                case "fine_gravel":
                case "paving_stones":
                    surfaceCat = 2;
                    break;
                case "raw":
                case "unpaved":
                case "winter":
                    surfaceCat = 3;
                    break;
                default:
                    surfaceCat = 4;
            }
        }
        return surfaceCat;
    }
    protected static double getDistFactor(WayAttributs wayTagEval ){
        double distFactor ;
        if ("cycleway".equals(wayTagEval.highway)) {
            if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network))
                distFactor = 1;
            else
                distFactor = 1.3;
        } else if ("primary".equals(wayTagEval.highway) || "primary_link".equals(wayTagEval.highway)) {
            if ("bic_no".equals(wayTagEval.bicycle))
                distFactor = 10;
            else if ( "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) || ( wayTagEval.cycleway != null && "lcn".equals(wayTagEval.network)))
                distFactor = 1.5;
            else if (wayTagEval.cycleway != null || "lcn".equals(wayTagEval.network)  )
                distFactor = 1.8;
            else
                distFactor = 2.5;
        } else if ("secondary".equals(wayTagEval.highway)) {
            if ( "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) || ( wayTagEval.cycleway != null && "lcn".equals(wayTagEval.network)))
                distFactor = 1.4;
            else if (wayTagEval.cycleway != null || "lcn".equals(wayTagEval.network) )
                distFactor = 1.6;
            else
                distFactor = 2.0;
        } else if ("tertiary".equals(wayTagEval.highway)) {
            if ( "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) || ( wayTagEval.cycleway != null && "lcn".equals(wayTagEval.network)))
                distFactor = 1.21;
            else if (wayTagEval.cycleway != null || "lcn".equals(wayTagEval.network))
                distFactor = 1.3;
            else
                distFactor = 1.5;
        } else if ("residential".equals(wayTagEval.highway)||"living_street".equals(wayTagEval.highway)) {
            if ("bic_destination".equals(wayTagEval.bicycle) || "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) )
                distFactor = 1.0;
            else
                distFactor = 1.15;
        } else if ("footway".equals(wayTagEval.highway) || "pedestrian".equals(wayTagEval.highway)) {
            if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) )
                distFactor = 1;
            else if ("bic_yes".equals(wayTagEval.bicycle))
                distFactor = 1.5;
            else if ("bic_no".equals(wayTagEval.bicycle))
                distFactor = 4.0;
            else
                distFactor = 3.0;
        } else if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network)) {
            distFactor = 1;
        } else if ("service".equals(wayTagEval.highway)) {
            if ("bic_destination".equals(wayTagEval.bicycle))
                distFactor = 1.0;
            else if ( "no".equals(wayTagEval.access) || "bic_no".equals(wayTagEval.bicycle))
                distFactor = 15;
            else
                distFactor = 1.5;
        } else
            distFactor = 1.3;
        return distFactor;
    }
}
