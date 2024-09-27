package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class TrekkingBike extends RoutingProfile {
/*    public TrekkingBike( ) {
        super(new CostCalculatorTwoPieceFunc( (short)1,(short)0, (short)3));
    }

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalculatorTreckingBike(wayAttributs, (CostCalculatorTwoPieceFunc) profileCalculator);
    } */
    public TrekkingBike( ) {
    super(new CostCalcSplineFloat( ));
}

    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalcSplineFloatTreckingBike(wayAttributs, (CostCalcSplineFloat) profileCalculator);
    }
    @Override
    public int getIconIdActive() {
        return R.drawable.rp_trekking_a;
    }
    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_trekking_i;
    }
    protected int getIconIdCalculating() {
        return R.drawable.rp_trekking_c;
    }

}
