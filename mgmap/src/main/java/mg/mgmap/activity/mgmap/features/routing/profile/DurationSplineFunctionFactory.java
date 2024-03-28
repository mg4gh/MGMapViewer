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
            double[] slopes = {-0.4, -0.2, -0.05, 0, 0.05, 0.1, 0.7};
            double[] durations = new double[slopes.length];
            double watt;
            if (bicType == 1){
                watt = 80 + 40*klevel;
            } else {
                watt = 130;
            }
            double ACw;
            double Cr;
            double fd;
            double fdown;
            double fr;
            if (bicType == 1){
                ACw = 0.4 + surfaceLevel * 0.05 ;
//                fd = Math.exp(-(slevel-2)*Math.log(Math.sqrt(2.0)))/1.6;
                fd = 1.104 - slevel/4.53;

                if (surfaceLevel <= 3){
                    Cr = 0.005 + 0.004*surfaceLevel;
                } else {
                    Cr = 0.015 + 0.015*fd  + 0.01*(surfaceLevel - 4);
                }
                fr = 1.1 - fd * (0.5+surfaceLevel/20.0);
                fdown =  fd*(3.5+surfaceLevel*0.6);
            } else {
                if (surfaceLevel <= 2){
                    ACw = 0.45 + 0.1 * surfaceLevel;
                    Cr = 0.004 + 0.001*surfaceLevel;
                    fr    = 0.85 - 0.075*surfaceLevel;
                    fdown = 2.5 + 0.5*surfaceLevel;
                } else {
                    ACw = 0.8 + 0.3 * ( surfaceLevel - 3);
                    Cr = 0.02 + 0.015 * (surfaceLevel - 3);
                    fr = 0.6;
                    fdown = 3.5 + (surfaceLevel - 2);
                }
            }
            double m = 90;
            durations[0] = (-slopes[0]-0.075)*fdown;
            durations[1] = (-slopes[1]-0.075)*fdown;
            durations[2] = 1 / (getFrictionBasedVelocity(slopes[2], watt, Cr, ACw, m) * fr);
            for (int i = 3; i < slopes.length; i++) {
                durations[i] = 1 / getFrictionBasedVelocity(slopes[i], watt, Cr, ACw, m);
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
