package mg.mgmap.generic.graph;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.datastore.Way;

import java.util.HashMap;

@SuppressWarnings({"ConstantExpression", "unused"})
public class WayAttributesManager {

    static int WA_HIGHWAY_BIT_SHIFT = 0;
    static int WA_HIGHWAY_BIT_LENGTH = 5;
    static final int WA_HIGHWAY_MASK =  ((1<<WA_HIGHWAY_BIT_LENGTH)-1);
    enum WA_Highway {UNSET, TRUNK, TRUNK_LINK, PRIMARY, PRIMARY_LINK, SECONDARY, SECONDARY_LINK, TERTIARY, TERTIARY_LINK, UNCLASSIFIED, RESIDENTIAL, LIVING_STREET, SERVICE, SERVICES, TRACK, PATH, CYCLEWAY, FOOTWAY, PEDESTRIAN, STEPS, CONSTRUCTION, MOTORWAY, MOTORWAY_LINK, ELEVATOR, BUSWAY, ROAD, BRIDLEWAY, VIA_FERRATA, RACEWAY, OTHER}
    static final WA_Highway[] WA_Highway_values = WA_Highway.values();

    public static int WA_BICYCLE_BIT_SHIFT = WA_HIGHWAY_BIT_SHIFT + WA_HIGHWAY_BIT_LENGTH;
    static int WA_BICYCLE_BIT_LENGTH = 4;
    static final int WA_BICYCLE_MASK =  ((1<<WA_BICYCLE_BIT_LENGTH)-1);
    enum WA_Bicycle {UNSET, PRIVATE, BIC_YES, BIC_NO, BIC_DESIGNATED, BIC_PERMISSIVE, BIC_PRIVATE, BIC_DESTINATION, BIC_FORESTRY, BIC_PERMIT, OTHER}
    static final WA_Bicycle[] WA_Bicycle_values = WA_Bicycle.values();

    public static int WA_ACCESS_BIT_SHIFT = WA_BICYCLE_BIT_SHIFT + WA_BICYCLE_BIT_LENGTH;
    public static int WA_ACCESS_BIT_LENGTH = 4;
    static final int WA_ACCESS_MASK =  ((1<<WA_ACCESS_BIT_LENGTH)-1);
    public enum WA_Access {UNSET, PRIVATE, NO, ACC_NO, DESTINATION, PERMISSIVE, PERMIT, FORESTRY, OTHER}
    static final WA_Access[] WA_Access_values = WA_Access.values();

    public static int WA_CYCLEWAY_BIT_SHIFT = WA_ACCESS_BIT_SHIFT + WA_ACCESS_BIT_LENGTH;
    public static int WA_CYCLEWAY_BIT_LENGTH = 2;
    public static final int WA_CYCLEWAY_MASK =  ((1<<WA_CYCLEWAY_BIT_LENGTH)-1);
    enum WA_Cycleway {UNSET, SET, OTHER}
    static final WA_Cycleway[] WA_Cycleway_values = WA_Cycleway.values();

    public static int WA_SURFACE_BIT_SHIFT = WA_CYCLEWAY_BIT_SHIFT + WA_CYCLEWAY_BIT_LENGTH;
    public static int WA_SURFACE_BIT_LENGTH = 4;
    static final int WA_SURFACE_MASK =  ((1<<WA_SURFACE_BIT_LENGTH)-1);
    enum WA_Surface {UNSET, ASPHALT, SMOOTH_PAVED, PAVED, COMPACTED, FINE_GRAVEL, PAVED_STONES, ROUGH_PAVED, GRAVEL, RAW, UNPAVED, WINTER, OTHER}
    static final WA_Surface[] WA_Surface_values = WA_Surface.values();

    public static int WA_MTB_SCALE_BIT_SHIFT = WA_SURFACE_BIT_SHIFT + WA_SURFACE_BIT_LENGTH;
    public static int WA_MTB_SCALE_BIT_LENGTH = 4;
    static final int WA_MTB_SCALE_MASK =  ((1<<WA_MTB_SCALE_BIT_LENGTH)-1);
    enum WA_MtbScale {UNSET, MTBS_0, MTBS_1, MTBS_2, MTBS_3, MTBS_4, MTBS_5, MTBS_6, OTHER}
    static final WA_MtbScale[] WA_MtbScale_values = WA_MtbScale.values();

    public static int WA_MTB_SCALE_UP_BIT_SHIFT = WA_MTB_SCALE_BIT_SHIFT + WA_MTB_SCALE_BIT_LENGTH;
    public static int WA_MTB_SCALE_UP_BIT_LENGTH = 3;
    static final int WA_MTB_SCALE_UP_MASK =  ((1<<WA_MTB_SCALE_UP_BIT_LENGTH)-1);
    enum WA_MtbScaleUp {UNSET, MTBU_0, MTBU_1, MTBU_2, MTBU_3, MTBU_4, MTBU_5, OTHER}
    static final WA_MtbScaleUp[] WA_MtbScaleUp_values = WA_MtbScaleUp.values();

    public static int WA_TRACK_TYPE_BIT_SHIFT = WA_MTB_SCALE_UP_BIT_SHIFT + WA_MTB_SCALE_UP_BIT_LENGTH;
    public static int WA_TRACK_TYPE_BIT_LENGTH = 3;
    static final int WA_TRACK_TYPE_MASK =  ((1<<WA_TRACK_TYPE_BIT_LENGTH)-1);
    enum WA_TrackType {UNSET, GRADE1, GRADE2, GRADE3, GRADE4, GRADE5, OTHER}
    static final WA_TrackType[] WA_TrackType_values = WA_TrackType.values();

    public static int WA_NETWORK_BIT_SHIFT = WA_TRACK_TYPE_BIT_SHIFT + WA_TRACK_TYPE_BIT_LENGTH;
    public static int WA_NETWORK_BIT_LENGTH = 3;
    static final int WA_NETWORK_MASK =  ((1<<WA_NETWORK_BIT_LENGTH)-1);
    enum WA_Network {UNSET, LCN, RCN, ICN, HCW, NCN, OTHER}
    static final WA_Network[] WA_Network_values = WA_Network.values();

    public static int WA_ONEWAY_BIT_SHIFT = WA_NETWORK_BIT_SHIFT + WA_NETWORK_BIT_LENGTH;
    public static int WA_ONEWAY_BIT_LENGTH = 2;
    static final int WA_ONEWAY_MASK =  ((1<<WA_ONEWAY_BIT_LENGTH)-1);
    enum WA_Oneway {UNSET, OW_BIC, OTHER}
    static final WA_Oneway[] WA_Oneway_values = WA_Oneway.values();


    public static HashMap<Long, WayAttributs> waMap = new HashMap<>();

    public static long waValue(Way way) {
        String oneway = null;
        String oneway_bic = null;

        long waValue = 0;

        for (Tag tag : way.tags) {
            switch (tag.key) {
                case "highway":
                    switch (tag.value) {
                        case "trunk":           waValue |= (long) WA_Highway.TRUNK.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "trunk_link":           waValue |= (long) WA_Highway.TRUNK_LINK.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "primary":         waValue |= (long) WA_Highway.PRIMARY.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "primary_link":    waValue |= (long) WA_Highway.PRIMARY_LINK.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "secondary":       waValue |= (long) WA_Highway.SECONDARY.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "secondary_link":       waValue |= (long) WA_Highway.SECONDARY_LINK.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "tertiary":        waValue |= (long) WA_Highway.TERTIARY.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "tertiary_link":        waValue |= (long) WA_Highway.TERTIARY_LINK.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "unclassified":    waValue |= (long) WA_Highway.UNCLASSIFIED.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "residential":     waValue |= (long) WA_Highway.RESIDENTIAL.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "living_street":   waValue |= (long) WA_Highway.LIVING_STREET.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "service":         waValue |= (long) WA_Highway.SERVICE.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "services":         waValue |= (long) WA_Highway.SERVICES.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "track":           waValue |= (long) WA_Highway.TRACK.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "path":            waValue |= (long) WA_Highway.PATH.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "cycleway":        waValue |= (long) WA_Highway.CYCLEWAY.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "footway":         waValue |= (long) WA_Highway.FOOTWAY.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "pedestrian":      waValue |= (long) WA_Highway.PEDESTRIAN.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "steps":           waValue |= (long) WA_Highway.STEPS.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "construction":    waValue |= (long) WA_Highway.CONSTRUCTION.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "motorway":        waValue |= (long) WA_Highway.MOTORWAY.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "motorway_link":        waValue |= (long) WA_Highway.MOTORWAY_LINK.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "elevator":        waValue |= (long) WA_Highway.ELEVATOR.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "busway":        waValue |= (long) WA_Highway.BUSWAY.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "road":            waValue |= (long) WA_Highway.ROAD.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "bridleway":            waValue |= (long) WA_Highway.BRIDLEWAY.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "via_ferrata":            waValue |= (long) WA_Highway.VIA_FERRATA.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        case "raceway":            waValue |= (long) WA_Highway.RACEWAY.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                        default:                waValue |= (long) WA_Highway.OTHER.ordinal() << WA_HIGHWAY_BIT_SHIFT; break;
                    }
                    break;

                case "bicycle":
                    switch (tag.value) {
                        case "private":     waValue |= (long) WA_Bicycle.PRIVATE.ordinal() << WA_BICYCLE_BIT_SHIFT; break;
                        case "bic_yes":         waValue |= (long) WA_Bicycle.BIC_YES.ordinal() << WA_BICYCLE_BIT_SHIFT; break;
                        case "bic_no":          waValue |= (long) WA_Bicycle.BIC_NO.ordinal() << WA_BICYCLE_BIT_SHIFT; break;
                        case "bic_designated":  waValue |= (long) WA_Bicycle.BIC_DESIGNATED.ordinal() << WA_BICYCLE_BIT_SHIFT; break;
                        case "bic_permissive":  waValue |= (long) WA_Bicycle.BIC_PERMISSIVE.ordinal() << WA_BICYCLE_BIT_SHIFT; break;
                        case "bic_private":  waValue |= (long) WA_Bicycle.BIC_PRIVATE.ordinal() << WA_BICYCLE_BIT_SHIFT; break;
                        case "bic_destination":  waValue |= (long) WA_Bicycle.BIC_DESTINATION.ordinal() << WA_BICYCLE_BIT_SHIFT; break;
                        case "bic_forestry":  waValue |= (long) WA_Bicycle.BIC_FORESTRY.ordinal() << WA_BICYCLE_BIT_SHIFT; break;
                        case "bic_permit":  waValue |= (long) WA_Bicycle.BIC_PERMIT.ordinal() << WA_BICYCLE_BIT_SHIFT; break;
                        default:            waValue |= (long) WA_Bicycle.OTHER.ordinal() << WA_BICYCLE_BIT_SHIFT; break;
                    }
                    break;

                case "access":
                    switch (tag.value) {
                        case "private": waValue |= (long) WA_Access.PRIVATE.ordinal() << WA_ACCESS_BIT_SHIFT; break;
                        case "no":      waValue |= (long) WA_Access.NO.ordinal() << WA_ACCESS_BIT_SHIFT; break;
                        case "acc_no":  waValue |= (long) WA_Access.ACC_NO.ordinal() << WA_ACCESS_BIT_SHIFT; break;
                        case "destination":  waValue |= (long) WA_Access.DESTINATION.ordinal() << WA_ACCESS_BIT_SHIFT; break;
                        case "permissive":  waValue |= (long) WA_Access.PERMISSIVE.ordinal() << WA_ACCESS_BIT_SHIFT; break;
                        case "permit":  waValue |= (long) WA_Access.PERMIT.ordinal() << WA_ACCESS_BIT_SHIFT; break;
                        case "forestry":  waValue |= (long) WA_Access.FORESTRY.ordinal() << WA_ACCESS_BIT_SHIFT; break;
                        default:        waValue |= (long) WA_Access.OTHER.ordinal() << WA_ACCESS_BIT_SHIFT; break;
                    }
                    break;

                case "cycleway":
                case "cycleway_track":
                case "cycleway_lane":
                    waValue |= (long) WA_Cycleway.SET.ordinal() << WA_CYCLEWAY_BIT_SHIFT;
                    break;

                case "surface":
                    switch (tag.value) {
                        case "asphalt":         waValue |= (long) WA_Surface.ASPHALT.ordinal() << WA_SURFACE_BIT_SHIFT; break;
                        case "smooth_paved":    waValue |= (long) WA_Surface.SMOOTH_PAVED.ordinal() << WA_SURFACE_BIT_SHIFT; break;
                        case "paved":           waValue |= (long) WA_Surface.PAVED.ordinal() << WA_SURFACE_BIT_SHIFT; break;
                        case "compacted":       waValue |= (long) WA_Surface.COMPACTED.ordinal() << WA_SURFACE_BIT_SHIFT; break;
                        case "fine_gravel":     waValue |= (long) WA_Surface.FINE_GRAVEL.ordinal() << WA_SURFACE_BIT_SHIFT; break;
                        case "paved_stones":    waValue |= (long) WA_Surface.PAVED_STONES.ordinal() << WA_SURFACE_BIT_SHIFT; break;
                        case "rough_paved":     waValue |= (long) WA_Surface.ROUGH_PAVED.ordinal() << WA_SURFACE_BIT_SHIFT; break;
                        case "gravel":          waValue |= (long) WA_Surface.GRAVEL.ordinal() << WA_SURFACE_BIT_SHIFT; break;
                        case "raw":             waValue |= (long) WA_Surface.RAW.ordinal() << WA_SURFACE_BIT_SHIFT; break;
                        case "unpaved":         waValue |= (long) WA_Surface.UNPAVED.ordinal() << WA_SURFACE_BIT_SHIFT; break;
                        case "winter":          waValue |= (long) WA_Surface.WINTER.ordinal() << WA_SURFACE_BIT_SHIFT; break;
                        default:                waValue |= (long) WA_Surface.OTHER.ordinal() << WA_SURFACE_BIT_SHIFT; break;
                    }
                    break;

                case "mtb_scale":
                    switch (tag.value) {
                        case "mtbs_0":   waValue |= (long) WA_MtbScale.MTBS_0.ordinal() << WA_MTB_SCALE_BIT_SHIFT; break;
                        case "mtbs_1":   waValue |= (long) WA_MtbScale.MTBS_1.ordinal() << WA_MTB_SCALE_BIT_SHIFT; break;
                        case "mtbs_2":   waValue |= (long) WA_MtbScale.MTBS_2.ordinal() << WA_MTB_SCALE_BIT_SHIFT; break;
                        case "mtbs_3":   waValue |= (long) WA_MtbScale.MTBS_3.ordinal() << WA_MTB_SCALE_BIT_SHIFT; break;
                        case "mtbs_4":   waValue |= (long) WA_MtbScale.MTBS_4.ordinal() << WA_MTB_SCALE_BIT_SHIFT; break;
                        case "mtbs_5":   waValue |= (long) WA_MtbScale.MTBS_5.ordinal() << WA_MTB_SCALE_BIT_SHIFT; break;
                        case "mtbs_6":   waValue |= (long) WA_MtbScale.MTBS_6.ordinal() << WA_MTB_SCALE_BIT_SHIFT; break;
                        default:    waValue |= (long) WA_MtbScale.OTHER.ordinal() << WA_MTB_SCALE_BIT_SHIFT; break;
                    }
                    break;

                case "mtb_scale_uphill":
                    switch (tag.value) {
                        case "mtbu_0":   waValue |= (long) WA_MtbScaleUp.MTBU_0.ordinal() << WA_MTB_SCALE_UP_BIT_SHIFT; break;
                        case "mtbu_1":   waValue |= (long) WA_MtbScaleUp.MTBU_1.ordinal() << WA_MTB_SCALE_UP_BIT_SHIFT; break;
                        case "mtbu_2":   waValue |= (long) WA_MtbScaleUp.MTBU_2.ordinal() << WA_MTB_SCALE_UP_BIT_SHIFT; break;
                        case "mtbu_3":   waValue |= (long) WA_MtbScaleUp.MTBU_3.ordinal() << WA_MTB_SCALE_UP_BIT_SHIFT; break;
                        case "mtbu_4":   waValue |= (long) WA_MtbScaleUp.MTBU_4.ordinal() << WA_MTB_SCALE_UP_BIT_SHIFT; break;
                        case "mtbu_5":   waValue |= (long) WA_MtbScaleUp.MTBU_5.ordinal() << WA_MTB_SCALE_UP_BIT_SHIFT; break;
                        default:    waValue |= (long) WA_MtbScaleUp.OTHER.ordinal() << WA_MTB_SCALE_UP_BIT_SHIFT; break;
                    }
                    break;

                case "tracktype":
                    switch (tag.value) {
                        case "grade1":  waValue |= (long) WA_TrackType.GRADE1.ordinal() << WA_TRACK_TYPE_BIT_SHIFT; break;
                        case "grade2":  waValue |= (long) WA_TrackType.GRADE2.ordinal() << WA_TRACK_TYPE_BIT_SHIFT; break;
                        case "grade3":  waValue |= (long) WA_TrackType.GRADE3.ordinal() << WA_TRACK_TYPE_BIT_SHIFT; break;
                        case "grade4":  waValue |= (long) WA_TrackType.GRADE4.ordinal() << WA_TRACK_TYPE_BIT_SHIFT; break;
                        case "grade5":  waValue |= (long) WA_TrackType.GRADE5.ordinal() << WA_TRACK_TYPE_BIT_SHIFT; break;
                        default:        waValue |= (long) WA_TrackType.OTHER.ordinal() << WA_TRACK_TYPE_BIT_SHIFT; break;
                    }
                    break;

                case "network":
                    switch (tag.value) {
                        case "lcn": waValue |= (long) WA_Network.LCN.ordinal() << WA_NETWORK_BIT_SHIFT; break;
                        case "rcn": waValue |= (long) WA_Network.RCN.ordinal() << WA_NETWORK_BIT_SHIFT; break;
                        case "icn": waValue |= (long) WA_Network.ICN.ordinal() << WA_NETWORK_BIT_SHIFT; break;
                        case "hcw": waValue |= (long) WA_Network.HCW.ordinal() << WA_NETWORK_BIT_SHIFT; break;
                        case "ncn": waValue |= (long) WA_Network.NCN.ordinal() << WA_NETWORK_BIT_SHIFT; break;
                        default:    waValue |= (long) WA_Network.OTHER.ordinal() << WA_NETWORK_BIT_SHIFT; break;
                    }
                    break;

                case "oneway":
                    oneway = tag.value;
                    break;
                case "oneway:bicycle":
                    oneway_bic = tag.value;
                    break;
            }
        }
        if ( "yes".equals(oneway) && !"ow_bic_no".equals(oneway_bic)){
            waValue |= (long) WA_Oneway.OW_BIC.ordinal() << WA_ONEWAY_BIT_SHIFT;
        }
        return waValue;
    }

    public static WayAttributs getWayAttributs(Way way) {
        long waValue = waValue(way);
        if (waMap.containsKey(waValue)) {
            return waMap.get(waValue);
        }
        WayAttributs wa = new WayAttributs(way);
        wa.waValue = waValue;
        waMap.put(waValue, wa);
        return wa;
    }

    public static WayAttributs getWayAttributs(long waValue) {
        WayAttributs wa = new WayAttributs();
        wa.highway = WA_Highway_values[(int)((waValue>>WA_HIGHWAY_BIT_SHIFT) & WA_HIGHWAY_MASK)].name().toLowerCase();
        if (wa.highway.equals("unset")) wa.highway = null;
        wa.bicycle = WA_Bicycle_values[(int)((waValue>>WA_BICYCLE_BIT_SHIFT) & WA_BICYCLE_MASK)].name().toLowerCase();
        if (wa.bicycle.equals("unset")) wa.bicycle = null;
        wa.access = WA_Access_values[(int)((waValue>>WA_ACCESS_BIT_SHIFT) & WA_ACCESS_MASK)].name().toLowerCase();
        if (wa.access.equals("unset")) wa.access = null;
        wa.cycleway = WA_Cycleway_values[(int)((waValue>>WA_CYCLEWAY_BIT_SHIFT) & WA_CYCLEWAY_MASK)].name().toLowerCase();
        if (wa.cycleway.equals("unset")) wa.cycleway = null;
        wa.surface = WA_Surface_values[(int)((waValue>>WA_SURFACE_BIT_SHIFT) & WA_SURFACE_MASK)].name().toLowerCase();
        if (wa.surface.equals("unset")) wa.surface = null;
        wa.mtbScale = WA_MtbScale_values[(int)((waValue>>WA_MTB_SCALE_BIT_SHIFT) & WA_MTB_SCALE_MASK)].name().toLowerCase();
        if (wa.mtbScale.equals("unset")) wa.mtbScale = null;
        wa.mtbScaleUp = WA_MtbScaleUp_values[(int)((waValue>>WA_MTB_SCALE_UP_BIT_SHIFT) & WA_MTB_SCALE_UP_MASK)].name().toLowerCase();
        if (wa.mtbScaleUp.equals("unset")) wa.mtbScaleUp = null;
        wa.trackType = WA_TrackType_values[(int)((waValue>>WA_TRACK_TYPE_BIT_SHIFT) & WA_TRACK_TYPE_MASK)].name().toLowerCase();
        if (wa.trackType.equals("unset")) wa.trackType = null;
        wa.network = WA_Network_values[(int)((waValue>>WA_NETWORK_BIT_SHIFT) & WA_NETWORK_MASK)].name().toLowerCase();
        if (wa.network.equals("unset")) wa.network = null;
        wa.onewayBic = ((int)(waValue>>WA_ONEWAY_BIT_SHIFT) & WA_ONEWAY_MASK) == WA_Oneway.OW_BIC.ordinal();
        wa.waValue = waValue;
        return wa;
    }


}
