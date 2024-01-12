package mg.mgmap.generic.graph;

import org.mapsforge.core.model.Tag;
import org.mapsforge.map.datastore.Way;

import java.util.ArrayList;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;

public class GEnv {

    private final Way defaultWay = new Way((byte)1,new ArrayList<>(),null, null);
    private final Way way;
    private final RoutingProfile routingProfile;

    public GEnv(RoutingProfile routingProfile){
        this.way = defaultWay;
        defaultWay.tags.add(new Tag("highway=true"));
        this.routingProfile = routingProfile;
    }

    public GEnv(Way way, RoutingProfile routingProfile) {
        this.way = way;
        this.routingProfile = routingProfile;
    }

    public Way getWay() {
        return way;
    }

    public RoutingProfile getRoutingProfile() {
        return routingProfile;
    }

    public double getCost(GNode node, GNeighbour neighbour){
        return getRoutingProfile().getCost(way, node, neighbour.getNeighbourNode());
    }
}
