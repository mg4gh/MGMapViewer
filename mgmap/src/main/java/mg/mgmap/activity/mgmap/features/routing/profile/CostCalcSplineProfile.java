package mg.mgmap.activity.mgmap.features.routing.profile;

import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.generic.util.basic.MGLog;

public abstract class CostCalcSplineProfile implements CostCalculator {
    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static final float lowstart = -0.15f;
    private static final float highstart = 0.02f;

    private final CubicSpline cubicSpline;
    private final CubicSpline cubicSplineHeuristic;

    protected CostCalcSplineProfile() {
        cubicSpline = getRefSpline();
        cubicSplineHeuristic = getCubicSplineHeuristic(lowstart,highstart);
    }

    protected abstract  CubicSpline getRefSpline();

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
        return dist * cubicSplineHeuristic.calc(vertDist / (float) dist) * 0.9999;
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

    protected boolean checkHeuristic(CubicSpline cubicSpline, short surfaceLevel) throws Exception {
        if (cubicSplineHeuristic!= null) {
            float xs = lowstart - 0.1f;
            float minmfd = (surfaceLevel == 0) ? (float) TagEval.minDistfSc0 : 1f;
            do {
                xs = xs + 0.001f;
                if (cubicSplineHeuristic.calc(xs)*0.9999f > minmfd * cubicSpline.calc(xs) + 0.0001f) {
                    float h = cubicSplineHeuristic.calc(xs);
                    float s = cubicSpline.calc(xs);
                    float d = h - s;
                    mgLog.d("SurfaceCat: " + surfaceLevel + " slope: " + xs + " delta:" + d );
                    throw new Exception("CubicSplineDouble of surfaceLevel " + surfaceLevel + " smaller than heuristic at slope: " + xs);
                }
            } while (xs < highstart + 0.2f);
        }
        return true;
    }


    protected float getFrictionBasedVelocity(double slope, double watt, double Cr, double ACw, double m ){
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

    /* How the heuristic is determined:
       Given two points with a distance d and vertical distance v and a continuously differentiable
       cost function of the elevation f(e) = f(v/d) (given here as a cubicSpline function) and the costs from source to target point is
       given by: c(d,v) = d*f(v/d). If the vertical distance is given and constant, one can vary the path to the
       target and thereby increase the distance. A criteria of this minimum cost is that the first
       derivative varying d is 0, c'(d) = f(v/d) - d * f'(d) v/d^2 = f(e) - f'(e)*e = 0 or
       f(e) = f'(e)*e. This is a tangent on f(e) crossing the origin.
       If f(e) has a single minimum and curvature is positive (f''(e)>=0), there will be two such
       tangents. So a heuristic is given by the two tangents above and below the touch points of those tangents
       and by f(e) between those touch points.
       Equations solved via Newton Iteration.
     */

    protected CubicSpline getCubicSplineHeuristic(float minStartNewton, float maxStartNewton){
        // cut out a new cubic spline out of the existing one using the to touch points of the tangents
        return cubicSpline.getCutCubicSpline(solve(minStartNewton,cubicSpline),solve(maxStartNewton,cubicSpline));
    }


    // Newton iteration to find tangent
    private float solve( float xs, CubicSpline cubicSpline){
        float a;
        float as;
        float xsp;
        float minval = 0.0001f;
        do {
            xsp = xs;
            a = cubicSpline.calc(xsp) - cubicSpline.calcSlope(xsp)*xsp - minval;
            as = -cubicSpline.calcCurve(xsp) * xsp;
            xs = xsp - a / as;
        } while ( a > minval || a < -minval);
        return xs;
    }
}
