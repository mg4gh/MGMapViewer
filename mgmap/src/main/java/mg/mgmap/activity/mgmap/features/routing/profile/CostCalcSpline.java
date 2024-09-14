package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSpline implements CostCalculator {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static double lowstart = -0.1;
    private static double highstart = 0.02;


    private final CubicSpline cubicSpline;
    private final CubicSplineHeuristic cubicSplineHeuristic;
    private final CubicSpline[] SurfaceCatSpline = new CubicSpline[7];


    protected CostCalcSpline() {
        cubicSpline = getDurationSpline((short) 0);
        cubicSplineHeuristic = new CubicSplineHeuristic(cubicSpline,lowstart,highstart);
    }


    public double calcCosts(double dist, float vertDist, boolean primaryDirection) {
        if (dist <= 0.0000001) {
            return 0.0001;
        }
        return dist * cubicSpline.calc(vertDist / dist) + 0.0001;
    }


    public double heuristic(double dist, float vertDist) {
        if (dist <= 0.0000001) {
            return 0.0;
        }
        return dist * cubicSplineHeuristic.heuristic(vertDist / dist) * 0.999;
    }

    @Override
    public long getDuration(double dist, float vertDist) {
        return (dist >= 0.00001) ? (long) (1000 * dist * cubicSpline.calc(vertDist / dist)) : 0;
    }

    protected CubicSpline getDurationSpline( short surfaceLevel){
        CubicSpline cubicSpline = SurfaceCatSpline[surfaceLevel];
        if (cubicSpline == null) {
            double watt0 = 90.0 ;
            double watt = 130.0;
            double ACw = 0.45;
            double fdown = 8.5;
            double m = 90;
            double [] cr = new double[] {0.0035,0.005,0.0065,0.02,0.04,0.075,0.15};
            double [] highdowndoffset = new double[] {0.15,0.142,0.13,0.12,0.1,0.08,-0.03};
            double[] relSlope;
            double[] slopes;
            double[] durations;
            if (surfaceLevel <= 3) {
                slopes = new double[]{ -0.6,-0.4,-0.2, -0.02, 0.0, 0.08, 0.2, 0.4};
                relSlope = new double[]{2.2,2.3,1.05,0.9};
                durations = new double[slopes.length];
                durations[4] = 1 / getFrictionBasedVelocity(0.0, watt0, cr[surfaceLevel], ACw, m);
                durations[3] = durations[4] + slopes[3]*relSlope[surfaceLevel];
            } else {
                slopes = new double[]{-0.6,-0.4,-0.2, 0.0, 0.08,0.2, 0.4};
                durations = new double[slopes.length];
                durations[3] = 1 / getFrictionBasedVelocity(0.0, watt0, cr[surfaceLevel], ACw, m);
            }
            durations[0] = -(slopes[0]+highdowndoffset[surfaceLevel])*fdown*1.5;
            durations[1] = -(slopes[1]+highdowndoffset[surfaceLevel])*fdown;
            durations[2] = -(slopes[2]+highdowndoffset[surfaceLevel])*fdown;
            durations[slopes.length-3] = 1.0 /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, cr[surfaceLevel], ACw, m)  ;
            durations[slopes.length-2] = 1.5 /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, cr[surfaceLevel], ACw, m)  ;
            durations[slopes.length-1] = 3.0 /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, cr[surfaceLevel], ACw, m)  ;
            try {
                cubicSpline = new CubicSpline(slopes, durations);
                SurfaceCatSpline[surfaceLevel] = cubicSpline;
                if (this.cubicSplineHeuristic !=  null){
                    checkHeuristic(cubicSpline, surfaceLevel);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return cubicSpline;
    }


    private boolean checkHeuristic( CubicSpline cubicSpline, short surfaceLevel) throws Exception {
        double xs = lowstart - 0.1;
        do {
            xs = xs + 0.001;
            if (cubicSplineHeuristic.heuristic(xs) > cubicSpline.calc(xs)) {
                throw new Exception("CubicSpline of surfaceLevel "+  surfaceLevel + " smaller than heuristic at slope: " + xs);
            }
        } while (xs < highstart + 0.2);
        return true;
    }


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