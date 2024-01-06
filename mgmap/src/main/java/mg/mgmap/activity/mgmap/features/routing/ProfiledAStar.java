package mg.mgmap.activity.mgmap.features.routing;

import mg.mgmap.generic.graph.AStar;
import mg.mgmap.generic.graph.GGraph;
import mg.mgmap.generic.graph.GNode;

public class ProfiledAStar extends AStar {

    private final RoutingProfile routingProfile;
    public ProfiledAStar(GGraph graph, RoutingProfile routingProfile) {
        super(graph);
        this.routingProfile = routingProfile;
    }

    @Override
    protected double heuristic(GNode node) {
        return routingProfile.heuristic(node,target);
    }

}
