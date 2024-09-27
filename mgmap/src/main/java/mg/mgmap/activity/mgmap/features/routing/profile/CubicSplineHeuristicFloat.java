package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

public class CubicSplineHeuristicFloat {
    private final float mUpSlopeLimit; //  up to this slope base Costs
    private final float mUpHeuSlope;
    private final float mUpHeuInterc;
    private final float mDnSlopeLimit ;
    private final float mDnHeuSlope;
    private final float mDnHeuInterc;
    private final CubicSplineFloat cubicSpline;
    private final CubicSplineFloat cubicSplineHeuristic;

    /* given two points with a distance d and vertical distance v and a continuously differentiable
       cost function of the elevation f(e) = f(v/d) (given here as a cubicSpline function) and the costs from source to target point is
       given by: c(d,v) = d*f(v/d). If the vertical distance is given and constant, one can vary the path to the
       target and thereby increase the distance. A criteria of this minimum cost is that the first
       derivative varying d is 0, c'(d) = f(v/d) - d * f'(d) v/d^2 = f(e) - f'(e)*e = 0 or
       f(e) = f'(e)*e. This is a tangent on f(e) crossing the origin.
       If f(e) has a single minimum and curvature is positive (f''(e)>=0), there will be two such
       tangents. So a heuristic is given by the two tangents above and below the touch points of those tangents
       and by f(e) between those touch points
     */

    protected CubicSplineHeuristicFloat(CubicSplineFloat cubicSpline, float lowstart, float highstart){
        this.cubicSpline = cubicSpline;
        mUpSlopeLimit = solve(highstart,cubicSpline);
        mUpHeuSlope  = cubicSpline.calcSlope(mUpSlopeLimit);
        mUpHeuInterc = cubicSpline.calc(mUpSlopeLimit) - mUpHeuSlope*mUpSlopeLimit;
        mDnSlopeLimit = solve(lowstart,cubicSpline);
        mDnHeuSlope  = cubicSpline.calcSlope(mDnSlopeLimit);
        mDnHeuInterc = cubicSpline.calc(mDnSlopeLimit) - mDnHeuSlope*mDnSlopeLimit;
        cubicSplineHeuristic = new CubicSplineFloat(cubicSpline,mDnSlopeLimit,mUpSlopeLimit);
    }

    protected float heuristic (float slope){
        float heuristic;
        if (slope <= mDnSlopeLimit)
            heuristic = mDnHeuInterc + slope * mDnHeuSlope;
        else if (slope >= mUpSlopeLimit)
            heuristic = mUpHeuInterc + slope * mUpHeuSlope;
        else
            heuristic = cubicSpline.calc(slope);
        float newHeuristic = cubicSplineHeuristic.calc(slope);
        return heuristic;
    }

    private float solve( float xs, CubicSplineFloat cubicSpline){
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
