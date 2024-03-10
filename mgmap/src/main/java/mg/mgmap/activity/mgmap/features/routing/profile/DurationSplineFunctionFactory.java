package mg.mgmap.activity.mgmap.features.routing.profile;

import java.util.Arrays;
import java.util.HashMap;

public class DurationSplineFunctionFactory {
    static DurationSplineFunctionFactory durationSplineFunctionFactory = new DurationSplineFunctionFactory();

    private class Id {
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
    private HashMap<Id,CubicSpline> map = new HashMap<>();

    public static DurationSplineFunctionFactory getInst(){
        return durationSplineFunctionFactory;
    }



    public CubicSpline getDurationSplineFunction(short klevel, short slevel,short bicType){
        return getDurationSplineFunction(klevel,slevel, (short) 1,bicType);
    }

    public CubicSpline getDurationSplineFunction(short klevel, short slevel, short surfaceLevel, short bicType){

        Id id = new Id(new short[] {klevel,slevel,surfaceLevel,bicType});
        CubicSpline cubicSpline = map.get(id);
        if (cubicSpline == null) {
            double[] slopes = {-0.4, -0.2, -0.05, 0, 0.05, 0.1, 0.7};
            double[] durations = new double[slopes.length];

            double watt = 60 + 40*klevel + 20*bicType;
            double ACw = 0.7;
            double rho = 1.2;
            double Cr = 0.005+(surfaceLevel-1) * 0.03;
            double m = 90;
            double fdown = 3.0/(slevel+1)+0.5*surfaceLevel;
            durations[0] = (-slopes[0]-0.075)*fdown;
            durations[1] = (-slopes[1]-0.075)*fdown;
            durations[2] = 1 / (getFrictionBasedVelocity(slopes[2], watt, Cr, ACw, rho, m) * 0.85);
            for (int i = 3; i < slopes.length; i++) {
                durations[i] = 1 / getFrictionBasedVelocity(slopes[i], watt, Cr, ACw, rho, m);
            }
            try {
                cubicSpline = new CubicSpline(slopes, durations);
                map.put(id,cubicSpline);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            double[] t = new double[80];
            double slope = -0.21;
            for (int i = 0; i < t.length; i++) {
                t[i] = cubicSpline.calc(slope);
                slope = slope + 0.01;
            }
        }
        return cubicSpline;
    }

    /**
     * see https://www.michael-konczer.com/de/training/rechner/rennrad-leistung-berechnen
     * @param slope
     * @param watt
     * @param Cr rolling friction coefficient
     * @param ACw
     * @param rho Air density
     * @param m system mass [driver + bike ]
     * @return velocity (v in [m/s]
     * watt = P air + P roll + P slope
     * P air = 1/2 * Acw * rho * v^3 ; P roll = mg * Cr * v; P slope = mg * slope
     * solved for velocity via cardanic equations
     */
    private double getFrictionBasedVelocity(double slope, double watt, double Cr, double ACw, double rho, double m ){
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
