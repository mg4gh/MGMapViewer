package mg.mgmap.application.util;

import org.mapsforge.map.datastore.Way;

public class CostProviderCycTour extends CostProviderTagEvalBasic {
    private short surfaceCat;

    public CostProviderCycTour(){
        setUpSlopeParameters(0.12);
        setDownSlopeParameters(0.17);
        initOffsets();
//        setCostFactorRed(1.0);
//        setFixUpDistParameter(1);
//        setFixDownDistParameter(1);
        setGenCostFactor(1);
        surfaceCat = 5;
    }

    @Override
    public void initializeSegment(Way way) {
        super.initializeSegment(way);

        if (accessable) {
//            setCostFactorRed(1.0);
//            setFixUpDistParameter(1);
//            setFixDownDistParameter(1);
            setGenCostFactor(1);
            surfaceCat = 5;


            if (surface != null){
                switch(surface) {
                    case "asphalt":
                    case "paved":
                        surfaceCat = 1;
                        break;
                    case "fine_gravel":
                    case "compacted":
                    case "paving_stones":
                        surfaceCat = 2;
                        break;
                }
            }
            if ("path".equals(highway)) {
                if (surfaceCat <= 2 || "bic_designated".equals(bicycle)|| "designated".equals(bicycle)|| "lcn".equals(network)||"rcn".equals(network))
                    setGenCostFactor(1);
                else if (("excellent".equals(trail_visibility) && surfaceCat <= 2 ) )
                    setGenCostFactor(1.5);
                else if ("good".equals(trail_visibility) && surfaceCat <= 3)
                    setGenCostFactor(3);
                else
                    accessable = false;
            }
            else if ("track".equals(highway)) {
                if ("grade1".equals(tracktype) || surfaceCat <= 1 || "bic_yes".equals(bicycle)|| "bic_designated".equals(bicycle))
                    setGenCostFactor(1);
                else if ( "grade2".equals(tracktype) || surfaceCat <= 2 )
                    setGenCostFactor(1.3);
                else if ("grade3".equals(tracktype) || surfaceCat <= 3)
                    setGenCostFactor(2);
                else
                    setGenCostFactor(4);
            }
            else if ("primary".equals(highway)||"primary_link".equals(highway)) {
                if ("bic_no".equals(bicycle))
                    accessable = false;
                else if (cycleway != null)
                    setGenCostFactor(1.5);
                else
                    setGenCostFactor(3.5);
            }
            else if ("secondary".equals(highway)) {
                if (cycleway != null)
                    setGenCostFactor(1.5);
                else
                    setGenCostFactor(2.5);
            }
            else if ("tertiary".equals(highway)) {
                setGenCostFactor(1.5);
            }
            else if ("steps".equals(highway)) {
                  setGenCostFactor(20);
//                setFixUpDistParameter(8);
//                setFixDownDistParameter(8);
            }
            else if ("footway".equals(highway)&&!"bic_yes".equals(bicycle))
                setGenCostFactor(6);
            else if ("bic_no".equals(bicycle))
                setGenCostFactor(6);

            if ("lcn".equals(network)||"rcn".equals(network)) {
              setCostFactorMulti(0.7);
          }
        }

    }

    @Override
    public void clearSegment() {
        super.clearSegment();
//        setUpSlopeParameters(0.12);
//        setDownSlopeParameters(0.17);
//        initOffsets();
//        setCostFactorRed(1.0);
//        setFixUpDistParameter(1);
//        setFixDownDistParameter(1);
        setGenCostFactor(1);
        surfaceCat = 5;
    }
}
