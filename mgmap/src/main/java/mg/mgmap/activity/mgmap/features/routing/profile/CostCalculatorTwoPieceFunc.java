package mg.mgmap.activity.mgmap.features.routing.profile;

import static java.lang.Math.abs;

import android.util.Log;

public abstract class CostCalculatorTwoPieceFunc  {
    protected final static double fb = 0.8; // upCosts = upLimit / fb
    protected final static double fa = 3.0; // upAddCosts = fa / ( upLimit * upLimit)

}
