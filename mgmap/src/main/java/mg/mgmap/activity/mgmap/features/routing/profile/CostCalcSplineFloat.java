package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.util.basic.MGLog;

public class CostCalcSplineFloat implements CostCalculator {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static float lowstart = -0.1f;
    private static float highstart = 0.02f;


    private final CubicSplineFloat cubicSpline;
    private final CubicSplineHeuristicFloat cubicSplineHeuristic;
    private final CubicSplineFloat[] SurfaceCatSpline = new CubicSplineFloat[7];


    protected CostCalcSplineFloat() {
        cubicSpline = getDurationSpline((short) 1);
        cubicSplineHeuristic = new CubicSplineHeuristicFloat(cubicSpline,lowstart,highstart);
    }


    public double calcCosts(double dist, float vertDist, boolean primaryDirection) {
        if (dist <= 0.0000001) {
            return 0.0001;
        }
        return dist * cubicSpline.calc(vertDist / (float) dist) + 0.0001;
    }


    public double heuristic(double dist, float vertDist) {
        if (dist <= 0.0000001) {
            return 0.0;
        }
        return dist * cubicSplineHeuristic.heuristic(vertDist / (float) dist) * 0.999;
    }

    @Override
    public long getDuration(double dist, float vertDist) {
        if (dist >= 0.00001) {
            float slope = vertDist / (float) dist;
            double spm = cubicSpline.calc(slope);
            double v = 3.6/spm;
            mgLog.d("DurationCalc - Slope:" + slope + " v:" + v + " time:" + spm*dist + " dist:" + dist );
//                       mgLog.d(()-> String.format(Locale.ENGLISH, "   segment dist=%.2f vertDist=%.2f ascend=%.1f cost=%.2f revCost=%.2f wa=%s",distance,verticalDistance,verticalDistance*100/distance,last.getNeighbour(node).getCost(),node.getNeighbour(last).getCost(),last.getNeighbour(node).getWayAttributs().toDetailedString()));
        }
        return (dist >= 0.00001) ? (long) (1000 * dist * cubicSpline.calc(vertDist / (float) dist)) : 0;
    }

    protected CubicSplineFloat getDurationSpline( short surfaceLevel){
        CubicSplineFloat cubicSpline = SurfaceCatSpline[surfaceLevel];
        if (cubicSpline == null) {
            float watt0 = 90.0f ;
            float watt = 130.0f;
            float ACw = 0.45f;
            float fdown = 8.5f;
            float m = 90f;
            float [] cr = new float[] {0.0035f,0.005f,0.0076f,0.015f,0.04f,0.075f,0.15f};
            float [] highdowndoffset = new float[] {0.15f,0.143f,0.13f,0.11f,0.1f,0.08f,-0.03f};
            float[] relSlope;
            float[] slopes;
            float[] durations;
            if (surfaceLevel <= 3) {
                slopes = new float[]{ -0.6f,-0.4f,-0.2f, -0.02f, 0.0f, 0.08f, 0.2f, 0.4f};
                relSlope = new float[]{2.2f,2.3f,1.15f,1.0f};
                durations = new float[slopes.length];
                durations[4] = 1f / getFrictionBasedVelocity(0.0f, watt0, cr[surfaceLevel], ACw, m);
                durations[3] = durations[4] + slopes[3]*relSlope[surfaceLevel];
            } else {
                slopes = new float[]{-0.6f,-0.4f,-0.2f, 0.0f, 0.08f,0.2f, 0.4f};
                durations = new float[slopes.length];
                durations[3] = 1 / getFrictionBasedVelocity(0.0, watt0, cr[surfaceLevel], ACw, m);
            }
            durations[0] = -(slopes[0]+highdowndoffset[surfaceLevel])*fdown*1.5f;
            durations[1] = -(slopes[1]+highdowndoffset[surfaceLevel])*fdown;
            durations[2] = -(slopes[2]+highdowndoffset[surfaceLevel])*fdown;
            durations[slopes.length-3] = 1.0f /  getFrictionBasedVelocity(slopes[slopes.length-3], watt, cr[surfaceLevel], ACw, m)  ;
            durations[slopes.length-2] = 1.5f /  getFrictionBasedVelocity(slopes[slopes.length-2], watt, cr[surfaceLevel], ACw, m)  ;
            durations[slopes.length-1] = 3.0f /  getFrictionBasedVelocity(slopes[slopes.length-1], watt, cr[surfaceLevel], ACw, m)  ;
            try {
                cubicSpline = new CubicSplineFloat(slopes, durations);
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


    private boolean checkHeuristic( CubicSplineFloat cubicSpline, short surfaceLevel) throws Exception {
        float xs = lowstart - 0.1f;
        float minmfd = (surfaceLevel==0) ? (float) TagEval.minDistfSc0:1f;
        do {
            xs = xs + 0.001f;
            if (cubicSplineHeuristic.heuristic(xs) >= minmfd * cubicSpline.calc(xs)) {
                throw new Exception("CubicSpline of surfaceLevel "+  surfaceLevel + " smaller than heuristic at slope: " + xs);
            }
        } while (xs < highstart + 0.2f);
        return true;
    }


    private float getFrictionBasedVelocity(double slope, double watt, double Cr, double ACw, double m ){
        double rho = 1.2;
        double mg = m*9.81;
        double ACwr = 0.5 * ACw * rho;
        double eta = 0.95;
        double p =  mg*(Cr+slope)/ACwr;
        double q =  -watt/ACwr*eta;
        double D = Math.pow(q,2)/4. + Math.pow(p,3)/27.;

        return (float) ((D>=0) ? Math.cbrt(- q*0.5 + Math.sqrt(D)) + Math.cbrt(- q*0.5 - Math.sqrt(D)) :
                Math.sqrt(-4.*p/3.) * Math.cos(1./3.*Math.acos(-q/2*Math.sqrt(-27./Math.pow(p,3.)))));
    }
}

