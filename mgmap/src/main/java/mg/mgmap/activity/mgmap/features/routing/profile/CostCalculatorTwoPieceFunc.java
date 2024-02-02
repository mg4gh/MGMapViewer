package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

public abstract class CostCalculatorTwoPieceFunc  {
    protected final static double fb = 0.8; // upCosts = upLimit / fb
    protected final static double fa = 3.0; // upAddCosts = fa / ( upLimit * upLimit)

    protected final static double base_ul = 1.3; // factor increase base limit

    protected final static double shift_ul = 1.3;
    protected final static double ref_ul = 0.1; // base up limit

}
