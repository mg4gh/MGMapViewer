package mg.mgmap.activity.mgmap.features.routing.profile;

import java.util.Arrays;
import java.util.HashMap;

public class DurationSplineFunctionFactory {
    static DurationSplineFunctionFactory durationSplineFunctionFactory = new DurationSplineFunctionFactory();

    private static class Id {
        private final short[] ids;
        Id(short[] ids){
            this.ids = ids;
        }
        @Override
        public int hashCode() {
            return Arrays.hashCode(ids);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            Id other = (Id) obj;
            return Arrays.equals(ids, other.ids);
        }
    }
    private final HashMap<Id,CubicSpline> map = new HashMap<>();

    public static DurationSplineFunctionFactory getInst(){
        return durationSplineFunctionFactory;
    }


    public CubicSpline getDurationSplineFunction(short klevel, short slevel, short surfaceLevel, short bicType){

        Id id = new Id(new short[] {klevel,slevel,surfaceLevel,bicType});
        CubicSpline cubicSpline = map.get(id);
        if (cubicSpline == null) {
            double[] slopes ;
            double[] durations;
            if (bicType > 0) {
//                slopes = new double[]{-0.4, -0.2, -0.05, 0, 0.05, 0.10, 0.24,0.6};
//                durations = new double[slopes.length];
                double watt;
                double watt0;
                double ACw;
                double Cr;
                double fd;
                double highdowndoffset;
                double fdown;
                double fr;
                double f1u;
                double f2u;
                double m = 90;
                if (bicType == 1){
                    slopes = new double[]{-0.4, -0.2, -0.05, 0, 0.05, 0.10, 0.24,0.6};
                    durations = new double[slopes.length];
                    watt = 90 + 35*klevel;
                    ACw = 0.4 + surfaceLevel * 0.05 ;
                    highdowndoffset = 0.075;
//                fd = Math.exp(-(slevel-2)*Math.log(Math.sqrt(2.0)))/1.6;
//                    fd = 1.104 - slevel/4.53;
                    fd = 1.1 - slevel/4.55;
                    if (surfaceLevel <= 3){
                        Cr = 0.004 + 0.001*surfaceLevel;
                    } else {
                        Cr = 0.015 + 0.015*fd  + 0.01*(surfaceLevel - 4);
                    }
//                    Cr = 0.004 + 0.002*surfaceLevel;
                    fr = 1.1 - fd * (0.5+surfaceLevel/20.0);
//                    fr = 0.715+0.045*slevel-0.08375*surfaceLevel+0.02375*slevel*surfaceLevel;
                    f1u = 1.0 + 0.5*surfaceLevel*surfaceLevel/16.0;
                    f2u = 1.2 + 1.4*surfaceLevel*surfaceLevel/16.0;
                    fdown =  fd*(3.5+surfaceLevel*0.6);
                    slopes[5] = 0.07+0.015*klevel;
                    slopes[6] = 0.24+0.02*klevel;
                    durations[0] = (-slopes[0]-highdowndoffset)*fdown;
                    durations[1] = (-slopes[1]-0.075)*fdown;
                    durations[2] = 1 / (getFrictionBasedVelocity(slopes[2], watt, Cr, ACw, m) * fr);
                    for (int i = 3; i < slopes.length - 2; i++) {
                        durations[i] = 1 / getFrictionBasedVelocity(slopes[i], watt, Cr, ACw, m);
                    }
                    durations[6] = f1u /  getFrictionBasedVelocity(slopes[6], watt, Cr, ACw, m)  ;
                    durations[7] = f2u /  getFrictionBasedVelocity(slopes[7], watt, Cr, ACw, m)  ;
                } else { //if (bicType ==3) {
                    watt0 = 90.0 ;
                    watt = 130.0;
                    ACw = 0.45;
                    fr = 1.0;
                    if (surfaceLevel <= 2) {
//                      ACw = 0.45 + 0.1 * surfaceLevel;
                        Cr = 0.0035 + 0.0015 * surfaceLevel;
//                      fr = 0.875 - 0.075 * surfaceLevel;
                        highdowndoffset = 0.0;
                        fdown = 3.5 + 0.5*surfaceLevel;
//                        fdown = 2.5 + 0.5*surfaceLevel;
                    } else {
//                        ACw = 0.8 + 0.3 * (surfaceLevel - 3);
                        Cr = 0.012 + 0.023 * (surfaceLevel - 3);
 //                       fr = 1.0; // 0.6;
                        highdowndoffset = -0.1;
                        fdown = 8.0 + (surfaceLevel - 3);
//                        fdown = 6 + 2*(surfaceLevel - 3);
                    }
                    int i0;
                    if (surfaceLevel <= 3) {
                        slopes = new double[]{-10.0, -0.6, -0.2, 50.0, 0.0, 0.1, 0.6,10.0};
                        durations = new double[slopes.length];
                        double[] freeRollSlope = new double[] {-0.049,-0.033,-0.0195,-0.0215};
                        slopes[3] = freeRollSlope[surfaceLevel];
                        durations[3] = 1 / (getFrictionBasedVelocity(slopes[3], 0.0, Cr, ACw, m));
                        i0 = 4;
                    } else {
                        slopes = new double[]{-10.0, -0.6,-0.2, 0.0, 0.1, 0.6,10.0};
                        durations = new double[slopes.length];
                        i0 = 3;
                    }
                    durations[0] = (-slopes[0]-highdowndoffset+0.4)*fdown;
                    durations[1] = (-slopes[1]-highdowndoffset)*fdown;
                    durations[2] = (-slopes[2]-0.075)*fdown;
                    durations[i0] = 1 / getFrictionBasedVelocity(slopes[i0], watt0, Cr, ACw, m);
                    for (int i = i0+1; i < slopes.length - 2; i++) {
                        durations[i] = 1 / getFrictionBasedVelocity(slopes[i], watt, Cr, ACw, m);
                    }
                    durations[slopes.length-2] = 1.5 /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, Cr, ACw, m)  ;
                    durations[slopes.length-1] = 1.8 /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, Cr, ACw, m)  ;
                }
            } else { // bicType == 0 -> hiking
                double fu = 9.0;
                double fd = 10.5;
                double off = 0.1;
                double vbase = 5.25 - 0.1*surfaceLevel;
                double t_base = 3.6/vbase;
                double t_m10Pcnt = t_base/1.3;

                slopes = new double[]{-0.5, -0.3, -0.075, 0.0, 0.2, 0.5};
                durations = new double[slopes.length];
                durations[0] = (-slopes[0]-off)*fd;
                durations[1] = (-slopes[1]-off)*fd;
                durations[2] = t_m10Pcnt;
                durations[3] = t_base;
                durations[4] = slopes[4]*fu;
                durations[5] = slopes[5]*fu;
            }

            try {
                cubicSpline = new CubicSpline(slopes, durations);
                map.put(id,cubicSpline);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cubicSpline;
    }

    /**
     * see <a href="https://www.michael-konczer.com/de/training/rechner/rennrad-leistung-berechnen">...</a>
     * @param slope slope of the trail
     * @param watt power measured in watt
     * @param Cr rolling friction coefficient
     * @param ACw Surface times dimensionless air resistance coefficient
     * @param m system mass [driver + bike ]
     * @return velocity (v in [m/s]
     * watt = P air + P roll + P slope
     * P air = 1/2 * Acw * rho * v^3 ; P roll = mg * Cr * v; P slope = mg * slope
     * solved for velocity via cardanic equations
     */
    private double getFrictionBasedVelocity(double slope, double watt, double Cr, double ACw, double m ){
        double rho = 1.2;
        double mg = m*9.81;
        double ACwr = 0.5 * ACw * rho;
        double eta = 0.95;
        double p =  mg*(Cr+slope)/ACwr;
        double q =  -watt/ACwr*eta;
        double D = Math.pow(q,2)/4. + Math.pow(p,3)/27.;

        return (D>=0) ? Math.cbrt(- q*0.5 + Math.sqrt(D)) + Math.cbrt(- q*0.5 - Math.sqrt(D)) :
                Math.sqrt(-4.*p/3.) * Math.cos(1./3.*Math.acos(-q/2*Math.sqrt(-27./Math.pow(p,3.))));
    }


}
