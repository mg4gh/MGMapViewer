package mg.mgmap.generic.graph;

import org.mapsforge.map.datastore.Way;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;

public class GEnv {

    private final Way way;
    private final RoutingProfile routingProfile;

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
