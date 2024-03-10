package mg.mgmap.activity.mgmap.features.routing.profile;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.features.routing.CostCalculator;
import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB_K3S3 extends RoutingProfile {

    public MTB_K3S3( ) {
//        super(new CostCalculatorTwoPieceFunc(8.0, 0.10, 3, 0, -0.27, 2));
        super(new CostCalculatorTwoPieceFunc((short)3,  (short)3, (short)1));
    }

    @Override
    protected CostCalculator getCostCalculator(CostCalculator profileCalculator, WayAttributs wayAttributs) {
        return new CostCalculatorMTB(wayAttributs, (CostCalculatorTwoPieceFunc) profileCalculator); //0.0,0.0,0.7,0.0,0.7,0.0);
    }

     @Override
    public int getIconIdActive() {
         return R.drawable.rp_mtb_k3s3_a;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_mtb_k3s3_i;
    }
    protected int getIconIdCalculating() {
        return R.drawable.rp_mtb_k3s3_c;
    }

}
