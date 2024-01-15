package mg.mgmap.activity.mgmap.features.routing.profile;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.generic.graph.GNode;

public class MTB_TEST3 extends GenRoutingProfile {

    public MTB_TEST3( ) {
        super(6.0, 0.2, 2, 0, -0.25, 2, new WayTagEvalMTB());
    }

}
