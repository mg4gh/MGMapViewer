package mg.mgmap.generic.graph.impl2;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;

@SuppressWarnings("unused")
public class BidirectionalAstarExt extends BidirectionalAStar{

    public BidirectionalAstarExt(GGraph graph, RoutingProfile routingProfile) {
        super(graph, routingProfile);
    }

    @Override
    public MultiPointModel performAlgo(ApproachModel sourceApproachModel, ApproachModel targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList) {
        MultiPointModel mpm = super.performAlgo(sourceApproachModel, targetApproachModel, costLimit, refreshRequired, relaxedList);
        RoutingSummary.routingSummaries.add(new RoutingSummary(sourceApproachModel, targetApproachModel, mpm, getMatchCost(matchNode), rc));
        return mpm;
    }
}
