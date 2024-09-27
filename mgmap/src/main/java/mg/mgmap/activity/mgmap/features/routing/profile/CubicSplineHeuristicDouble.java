package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

public class CubicSplineHeuristicDouble {
    private final double mUpSlopeLimit; //  up to this slope base Costs
    private final double mUpHeuSlope;
    private final double mUpHeuInterc;
    private final double mDnSlopeLimit ;
    private final double mDnHeuSlope;
    private final double mDnHeuInterc;
    private final CubicSplineDouble cubicSpline;


    protected CubicSplineHeuristicDouble(CubicSplineDouble cubicSpline, double lowstart, double highstart){
        this.cubicSpline = cubicSpline;
        mUpSlopeLimit = solve(highstart,cubicSpline);
        mUpHeuSlope  = cubicSpline.calcSlope(mUpSlopeLimit);
        mUpHeuInterc = cubicSpline.calc(mUpSlopeLimit) - mUpHeuSlope*mUpSlopeLimit;
        mDnSlopeLimit = solve(lowstart,cubicSpline);
        mDnHeuSlope  = cubicSpline.calcSlope(mDnSlopeLimit);
        mDnHeuInterc = cubicSpline.calc(mDnSlopeLimit) - mDnHeuSlope*mDnSlopeLimit;
    }

    protected double heuristic (double slope){
        if (slope <= mDnSlopeLimit)
            return mDnHeuInterc + slope * mDnHeuSlope;
        else if (slope >= mUpSlopeLimit)
            return mUpHeuInterc + slope * mUpHeuSlope;
        else
            return cubicSpline.calc(slope);
    }

    private double solve( double xs, CubicSplineDouble cubicSpline){
        double a;
        double as;
        double xsp;
        do {
            xsp = xs;
            a = cubicSpline.calc(xsp) - cubicSpline.calcSlope(xsp)*xsp - 0.001;
            as = -cubicSpline.calcCurve(xsp) * xsp;
            xs = xsp - a / as;
        } while ( abs(xs-xsp)>0.0001);
        return xs;
    }
}
