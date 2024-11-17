package mg.mgmap.generic.graph;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;

/**
 * This class provides a bidirectional AStar, which is: fast - not optimal
 * This means, it might find faster a solution at the price that this solution is not the optimal one.
 */
@SuppressWarnings("unused") // usage is via reflection
public class BidirectionalAStarFNO extends BidirectionalAStar{


    public BidirectionalAStarFNO(GGraphMulti graph, RoutingProfile routingProfile) {
        super(graph, routingProfile);
    }

    double heuristic(boolean reverse, GNode node){
        double h;
        if (reverse){
            h = routingProfile.heuristic(source, node);
        } else {
            h = routingProfile.heuristic(node, target);
        }
        return h;
    }

}
