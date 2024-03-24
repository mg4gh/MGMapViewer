package mg.mgmap.generic.graph;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;

public abstract class GGraphSearch {

    protected final GGraphMulti graph;
    protected final RoutingProfile routingProfile;

    public GGraphSearch(GGraphMulti graph, RoutingProfile routingProfile) {
        this.graph = graph;
        this.routingProfile = routingProfile;
    }

    public abstract MultiPointModel perform(GNode source, GNode target, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList);

    public abstract ArrayList<MultiPointModel> getBestPath();

    public abstract String getResult();

    public MultiPointModelImpl getPath(GNodeRef ref){
        MultiPointModelImpl path = new MultiPointModelImpl();
        if (ref != null){
            while (ref.getPredecessor() != null){
                path.addPoint(0, ref.getNode());
                ref = ref.getPredecessor().getNodeRef(ref.isReverse());
            }
            path.addPoint(0,ref.getNode());
        }
        return path;
    }

}
