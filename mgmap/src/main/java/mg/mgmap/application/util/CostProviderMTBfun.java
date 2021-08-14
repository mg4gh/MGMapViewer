package mg.mgmap.application.util;

import org.mapsforge.map.datastore.Way;

public class CostProviderMTBfun extends CostProviderTagEvalBasic{
    public CostProviderMTBfun(){
        setUpSlopeParameters(0.15);
        setDownSlopeParameters(0.2);
        setBaseCostHm(0);
        initOffsets();
//        setCostFactorRed(1.0);
//        setFixUpDistParameter(1);
//        setFixDownDistParameter(1);
        setGenCostFactor(1);
    }
    @Override

    public void initializeSegment(Way way) {
        super.initializeSegment(way);

        if (accessable) {
//            setCostFactorRed(1.0);
//            setFixUpDistParameter(1);
//            setFixDownDistParameter(1);
//            setUpSlopeParameters(0.15);
//            setGenCostFactor(1);
            if ("path".equals(highway)) {
                setUpSlopeParameters(0.12);
                if (mtbscale != null) {
                    switch (mtbscale) {
                        case "mtbs_0":
                        case "mtbs_1":
                            break;
                        case "mtbs_2":
                            setGenCostFactor(1.5);
                            break;
                        case "mtbs_3":
                        default:
                            setGenCostFactor(3);
                    }
                }
                if (trail_visibility != null) {
                    switch (trail_visibility) {
                        case "bad":
                            setCostFactorMulti(1.5);
                            break;
                        case "horrible":
                        case "no":
                            setGenCostFactor(2);
                            break;
                    }
                }
            } else if ("track".equals(highway)) {
//                setGenCostFactor(1.2);
            } else if ("primary".equals(highway)) {
                if ("bic_no".equals(bicycle))
                    accessable = false;
                else if (cycleway != null)
                    setGenCostFactor(2);
                else
                    setGenCostFactor(3);
            } else if ("secondary".equals(highway)) {
                if (cycleway != null)
                    setGenCostFactor(1.5);
                else
                    setGenCostFactor(2);
            } else if ("tertiary".equals(highway)) {
                setGenCostFactor(1.5);
            } else if ("steps".equals(highway)) {
                setGenCostFactor(4);
//                setFixUpDistParameter(8);
//                setFixDownDistParameter(8);
            } else if ("footway".equals(highway)) {
                setUpGenCostFactor(8);
                setDownGenCostFactor(2);
            }
            else if ("bic_no".equals(bicycle)) setGenCostFactor(4);
        }
        initOffsets();
    }

@Override
    public void clearSegment() {
        super.clearSegment();
        setUpSlopeParameters(0.15);
        setDownSlopeParameters(0.2);
        initOffsets();
//        setCostFactorRed(1.0);
//        setFixUpDistParameter(1);
//        setFixDownDistParameter(1);
        setGenCostFactor(1);
    }
}
