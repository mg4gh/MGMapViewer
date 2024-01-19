package mg.mgmap.activity.mgmap.features.routing.profile;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.R;
import mg.mgmap.generic.graph.WayAttributs;

public class MTB_TEST3 extends GenRoutingProfile {

    public MTB_TEST3( ) {
        super(8.0, 0.15, 2, 0, -0.27, 2);
    }
    public WayAttributs getWayAttributes(Way way){
        return new WayTagEvalMTB(way,this);
   }
    @Override
    protected int getIconIdActive() {
        return R.drawable.rp_mtb1;
    }

    @Override
    protected int getIconIdInactive() {
        return R.drawable.rp_mtb2;
    }
}
