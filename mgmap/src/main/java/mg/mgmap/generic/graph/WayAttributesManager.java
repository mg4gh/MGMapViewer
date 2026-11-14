package mg.mgmap.generic.graph;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.datastore.Way;

import java.util.HashMap;

@SuppressWarnings({"ConstantValue", "PointlessBitwiseExpression", "SwitchStatementWithTooFewBranches", "ConstantExpression", "unused"})
public class WayAttributesManager {

    public static int WA_HIGHWAY_BIT_SHIFT = 0;
    static final int WA_HIGHWAY_VALUE_UNSET =           0x00000 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_TRUNK =           0x00001 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_PRIMARY =         0x00010 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_PRIMARY_LINK =    0x00011 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_SECONDARY =       0x00100 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_TERTIARY =        0x00101 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_UNCLASSIFIED =    0x00110 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_RESIDENTIAL =     0x00111 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_LIVING_STREET =   0x01000 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_SERVICE =         0x01001 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_TRACK =           0x01010 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_PATH =            0x01011 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_CYCLEWAY =        0x01100 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_FOOTWAY =         0x01101 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_PEDESTRIAN =      0x01110 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_STEPS =           0x01111 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_CONSTRUCTION =    0x10000 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_VALUE_MOTORWAY =        0x10001 << WA_HIGHWAY_BIT_SHIFT;
    static final int WA_HIGHWAY_MASK =                  0b11111 << WA_HIGHWAY_BIT_SHIFT;

    public static int WA_BICYCLE_BIT_SHIFT = 5;
    static final int WA_BICYCLE_VALUE_UNSET =           0b000 << WA_BICYCLE_BIT_SHIFT;
    static final int WA_BICYCLE_VALUE_PRIVATE =         0b001 << WA_BICYCLE_BIT_SHIFT;
    static final int WA_BICYCLE_VALUE_BIC_YES =         0b010 << WA_BICYCLE_BIT_SHIFT;
    static final int WA_BICYCLE_VALUE_BIC_NO =          0b011 << WA_BICYCLE_BIT_SHIFT;
    static final int WA_BICYCLE_VALUE_BIC_DESIGNATED =  0b100 << WA_BICYCLE_BIT_SHIFT;
    static final int WA_BICYCLE_VALUE_BIC_PERMISSIVE =  0b101 << WA_BICYCLE_BIT_SHIFT;
    static final int WA_BICYCLE_MASK =                  0b111 << WA_BICYCLE_BIT_SHIFT;

    public static int WA_ACCESS_BIT_SHIFT = 8;
    static final int WA_ACCESS_VALUE_UNSET =            0b000 << WA_ACCESS_BIT_SHIFT;
    static final int WA_ACCESS_VALUE_PRIVATE =          0b001 << WA_ACCESS_BIT_SHIFT;
    static final int WA_ACCESS_VALUE_NO =               0b010 << WA_ACCESS_BIT_SHIFT;
    static final int WA_ACCESS_VALUE_ACC_NO =           0b011 << WA_ACCESS_BIT_SHIFT;
    static final int WA_ACCESS_MASK =                   0b111 << WA_ACCESS_BIT_SHIFT;

    public static int WA_CYCLEWAY_BIT_SHIFT = 11;
    static final int WA_CYCLEWAY_VALUE_UNSET =          0b00 << WA_CYCLEWAY_BIT_SHIFT;
    static final int WA_CYCLEWAY_VALUE_SET =            0b01 << WA_CYCLEWAY_BIT_SHIFT;
    static final int WA_CYCLEWAY_MASK =                 0b11 << WA_CYCLEWAY_BIT_SHIFT;

    public static int WA_SURFACE_BIT_SHIFT = 13;
    static final int WA_SURFACE_VALUE_UNSET =           0b0000 << WA_SURFACE_BIT_SHIFT;
    static final int WA_SURFACE_VALUE_ASPHALT =         0b0001 << WA_SURFACE_BIT_SHIFT;
    static final int WA_SURFACE_VALUE_SMOOTH_PAVED =    0b0010 << WA_SURFACE_BIT_SHIFT;
    static final int WA_SURFACE_VALUE_PAVED =           0b0011 << WA_SURFACE_BIT_SHIFT;
    static final int WA_SURFACE_VALUE_COMPACTED =       0b0100 << WA_SURFACE_BIT_SHIFT;
    static final int WA_SURFACE_VALUE_FINE_GRAVEL =     0b0101 << WA_SURFACE_BIT_SHIFT;
    static final int WA_SURFACE_VALUE_PAVED_STONES =    0b0110 << WA_SURFACE_BIT_SHIFT;
    static final int WA_SURFACE_VALUE_ROUGH_PAVED =     0b0111 << WA_SURFACE_BIT_SHIFT;
    static final int WA_SURFACE_VALUE_GRAVEL =          0b1000 << WA_SURFACE_BIT_SHIFT;
    static final int WA_SURFACE_VALUE_RAW =             0b1001 << WA_SURFACE_BIT_SHIFT;
    static final int WA_SURFACE_VALUE_UNPAVED =         0b1010 << WA_SURFACE_BIT_SHIFT;
    static final int WA_SURFACE_VALUE_WINTER =          0b1011 << WA_SURFACE_BIT_SHIFT;
    static final int WA_SURFACE_MASK =                  0b1111 << WA_SURFACE_BIT_SHIFT;

    public static int WA_MTB_SCALE_BIT_SHIFT = 17;
    public static int WA_MTB_SCALE_VALUE_UNSET =        0b000 << WA_MTB_SCALE_BIT_SHIFT;
    public static int WA_MTB_SCALE_VALUE_MTBS_0 =       0b001 << WA_MTB_SCALE_BIT_SHIFT;
    public static int WA_MTB_SCALE_VALUE_MTBS_1 =       0b010 << WA_MTB_SCALE_BIT_SHIFT;
    public static int WA_MTB_SCALE_VALUE_MTBS_2 =       0b011 << WA_MTB_SCALE_BIT_SHIFT;
    public static int WA_MTB_SCALE_VALUE_MTBS_3 =       0b100 << WA_MTB_SCALE_BIT_SHIFT;
    public static int WA_MTB_SCALE_MASK =               0b111 << WA_MTB_SCALE_BIT_SHIFT;

    public static int WA_MTB_SCALE_UP_BIT_SHIFT = 20;
    public static int WA_MTB_SCALE_UP_VALUE_UNSET =     0b000 << WA_MTB_SCALE_UP_BIT_SHIFT;
    public static int WA_MTB_SCALE_UP_VALUE_MTBU_0 =    0b001 << WA_MTB_SCALE_UP_BIT_SHIFT;
    public static int WA_MTB_SCALE_UP_VALUE_MTBU_1 =    0b010 << WA_MTB_SCALE_UP_BIT_SHIFT;
    public static int WA_MTB_SCALE_UP_VALUE_MTBU_2 =    0b011 << WA_MTB_SCALE_UP_BIT_SHIFT;
    public static int WA_MTB_SCALE_UP_VALUE_MTBU_3 =    0b100 << WA_MTB_SCALE_UP_BIT_SHIFT;
    public static int WA_MTB_SCALE_UP_VALUE_MTBU_4 =    0b101 << WA_MTB_SCALE_UP_BIT_SHIFT;
    public static int WA_MTB_SCALE_UP_MASK =            0b111 << WA_MTB_SCALE_UP_BIT_SHIFT;

    public static int WA_TRACK_TYPE_BIT_SHIFT = 23;
    public static int WA_TRACK_TYPE_VALUE_UNSET =       0b000 << WA_TRACK_TYPE_BIT_SHIFT;
    public static int WA_TRACK_TYPE_VALUE_GRADE1 =      0b001 << WA_TRACK_TYPE_BIT_SHIFT;
    public static int WA_TRACK_TYPE_VALUE_GRADE2 =      0b010 << WA_TRACK_TYPE_BIT_SHIFT;
    public static int WA_TRACK_TYPE_VALUE_GRADE3 =      0b011 << WA_TRACK_TYPE_BIT_SHIFT;
    public static int WA_TRACK_TYPE_VALUE_GRADE4 =      0b100 << WA_TRACK_TYPE_BIT_SHIFT;
    public static int WA_TRACK_TYPE_MASK =              0b111 << WA_TRACK_TYPE_BIT_SHIFT;

    public static int WA_NETWORK_BIT_SHIFT = 26;
    public static int WA_NETWORK_VALUE_UNSET =          0b000 << WA_NETWORK_BIT_SHIFT;
    public static int WA_NETWORK_VALUE_LCN =            0b001 << WA_NETWORK_BIT_SHIFT;
    public static int WA_NETWORK_VALUE_RCN =            0b010 << WA_NETWORK_BIT_SHIFT;
    public static int WA_NETWORK_VALUE_ICN =            0b011 << WA_NETWORK_BIT_SHIFT;
    public static int WA_NETWORK_MASK =                 0b111 << WA_NETWORK_BIT_SHIFT;

    public static int WA_ONEWAY_BIT_SHIFT = 29;
    public static int WA_ONEWAY_VALUE_UNSET =           0b0 << WA_ONEWAY_BIT_SHIFT;
    public static int WA_ONEWAY_VALUE_OW_BIC =          0b1 << WA_ONEWAY_BIT_SHIFT;


    public static HashMap<Integer, WayAttributs> waMap = new HashMap<>();

    static int waValue(Way way) {
        String oneway = null;
        String oneway_bic = null;

        int waValue = 0;

        for (Tag tag : way.tags) {
            switch (tag.key) {
                case "highway":
                    switch (tag.value) {
                        case "trunk":           waValue |= WA_HIGHWAY_VALUE_TRUNK; break;
                        case "primary":         waValue |= WA_HIGHWAY_VALUE_PRIMARY; break;
                        case "primary_link":    waValue |= WA_HIGHWAY_VALUE_PRIMARY_LINK; break;
                        case "secondary":       waValue |= WA_HIGHWAY_VALUE_SECONDARY; break;
                        case "tertiary":        waValue |= WA_HIGHWAY_VALUE_TERTIARY; break;
                        case "unclassified":    waValue |= WA_HIGHWAY_VALUE_UNCLASSIFIED; break;
                        case "residential":     waValue |= WA_HIGHWAY_VALUE_RESIDENTIAL; break;
                        case "living_street":   waValue |= WA_HIGHWAY_VALUE_LIVING_STREET; break;
                        case "service":         waValue |= WA_HIGHWAY_VALUE_SERVICE; break;
                        case "track":           waValue |= WA_HIGHWAY_VALUE_TRACK; break;
                        case "path":            waValue |= WA_HIGHWAY_VALUE_PATH; break;
                        case "cycleway":        waValue |= WA_HIGHWAY_VALUE_CYCLEWAY; break;
                        case "footway":         waValue |= WA_HIGHWAY_VALUE_FOOTWAY; break;
                        case "pedestrian":      waValue |= WA_HIGHWAY_VALUE_PEDESTRIAN; break;
                        case "steps":           waValue |= WA_HIGHWAY_VALUE_STEPS; break;
                        case "construction":    waValue |= WA_HIGHWAY_VALUE_CONSTRUCTION; break;
                        case "motorway":        waValue |= WA_HIGHWAY_VALUE_MOTORWAY; break;
                        default:                waValue |= WA_HIGHWAY_MASK; break;
                    }
                    break;

                case "surface":
                    switch (tag.value) {
                        case "asphalt":         waValue |= WA_SURFACE_VALUE_ASPHALT; break;
                        case "smooth_paved":    waValue |= WA_SURFACE_VALUE_SMOOTH_PAVED; break;
                        case "paved":           waValue |= WA_SURFACE_VALUE_PAVED; break;
                        case "compacted":       waValue |= WA_SURFACE_VALUE_COMPACTED; break;
                        case "fine_gravel":     waValue |= WA_SURFACE_VALUE_FINE_GRAVEL; break;
                        case "paved_stones":    waValue |= WA_SURFACE_VALUE_PAVED_STONES; break;
                        case "rough_paved":     waValue |= WA_SURFACE_VALUE_ROUGH_PAVED; break;
                        case "gravel":          waValue |= WA_SURFACE_VALUE_GRAVEL; break;
                        case "raw":             waValue |= WA_SURFACE_VALUE_RAW; break;
                        case "unpaved":         waValue |= WA_SURFACE_VALUE_UNPAVED; break;
                        case "winter":          waValue |= WA_SURFACE_VALUE_WINTER; break;
                        default:                waValue |= WA_SURFACE_MASK; break;
                    }
                    break;

                case "tracktype":
                    switch (tag.value) {
                        case "grade1":  waValue |= WA_TRACK_TYPE_VALUE_GRADE1; break;
                        case "grade2":  waValue |= WA_TRACK_TYPE_VALUE_GRADE2; break;
                        case "grade3":  waValue |= WA_TRACK_TYPE_VALUE_GRADE3; break;
                        case "grade4":  waValue |= WA_TRACK_TYPE_VALUE_GRADE4; break;
                        default:        waValue |= WA_TRACK_TYPE_MASK; break;
                    }
                    break;

                case "network":
                    switch (tag.value) {
                        case "lcn": waValue |= WA_NETWORK_VALUE_LCN; break;
                        case "rcn": waValue |= WA_NETWORK_VALUE_RCN; break;
                        case "icn": waValue |= WA_NETWORK_VALUE_ICN; break;
                        default:    waValue |= WA_NETWORK_MASK; break;
                    }
                    break;

                case "bicycle":
                    switch (tag.value) {
                        case "private":     waValue |= WA_BICYCLE_VALUE_PRIVATE; break;
                        case "yes":         waValue |= WA_BICYCLE_VALUE_BIC_YES; break;
                        case "no":          waValue |= WA_BICYCLE_VALUE_BIC_NO; break;
                        case "designated":  waValue |= WA_BICYCLE_VALUE_BIC_DESIGNATED; break;
                        case "permissive":  waValue |= WA_BICYCLE_VALUE_BIC_PERMISSIVE; break;
                        default:            waValue |= WA_BICYCLE_MASK; break;
                    }
                    break;

                case "cycleway":
                case "cycleway_track":
                case "cycleway_lane":
                    switch (tag.value) {
                        case "yes": waValue |= WA_CYCLEWAY_VALUE_SET; break;
                        default:    waValue |= WA_CYCLEWAY_MASK; break;
                    }
                    break;

                case "access":
                    switch (tag.value) {
                        case "private": waValue |= WA_ACCESS_VALUE_PRIVATE; break;
                        case "no":      waValue |= WA_ACCESS_VALUE_NO; break;
                        case "acc_no":  waValue |= WA_ACCESS_VALUE_ACC_NO; break;
                        default:        waValue |= WA_ACCESS_MASK; break;
                    }
                    break;

                case "mtb_scale":
                    switch (tag.value) {
                        case "0":   waValue |= WA_MTB_SCALE_VALUE_MTBS_0; break;
                        case "1":   waValue |= WA_MTB_SCALE_VALUE_MTBS_1; break;
                        case "2":   waValue |= WA_MTB_SCALE_VALUE_MTBS_2; break;
                        case "3":   waValue |= WA_MTB_SCALE_VALUE_MTBS_3; break;
                        default:    waValue |= WA_MTB_SCALE_MASK; break;
                    }
                    break;

                case "mtb_scale_uphill":
                    switch (tag.value) {
                        case "0":   waValue |= WA_MTB_SCALE_UP_VALUE_MTBU_0; break;
                        case "1":   waValue |= WA_MTB_SCALE_UP_VALUE_MTBU_1; break;
                        case "2":   waValue |= WA_MTB_SCALE_UP_VALUE_MTBU_2; break;
                        case "3":   waValue |= WA_MTB_SCALE_UP_VALUE_MTBU_3; break;
                        case "4":   waValue |= WA_MTB_SCALE_UP_VALUE_MTBU_4; break;
                        default:    waValue |= WA_MTB_SCALE_UP_MASK; break;
                    }
                    break;

                case "oneway":
                    oneway = tag.value;
                    break;
                case "oneway:bicycle":
                    oneway_bic = tag.value;
                    break;
                case "smoothness":
                    boolean smoothness = true;
                    break;
            }
        }
        if ( "yes".equals(oneway) && !"ow_bic_no".equals(oneway_bic)){
            waValue |= WA_ONEWAY_VALUE_OW_BIC;
        }
        return waValue;
    }

    public static WayAttributs getWayAttributs(Way way) {
        int waValue = waValue(way);
        if (waMap.containsKey(waValue)) {
            return waMap.get(waValue);
        }
        WayAttributs wa = new WayAttributs(way);
        waMap.put(waValue, wa);
        return wa;
    }


}
