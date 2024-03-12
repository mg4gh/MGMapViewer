package mg.mgmap.generic.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.model.PointModel;

public abstract class GGraphSearch {

    protected final GGraphMulti graph;
    protected final RoutingProfile routingProfile;

    public GGraphSearch(GGraphMulti graph, RoutingProfile routingProfile) {
        this.graph = graph;
        this.routingProfile = routingProfile;
    }

    public abstract List<GNodeRef> perform(GNode source, GNode target, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList);

    public abstract ArrayList<GNodeRef> getBestPath();

    public abstract String getResult();

}
