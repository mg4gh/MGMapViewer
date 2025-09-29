package mg.mgmap.generic.graph;

import java.util.ArrayList;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.PointModel;

public interface GraphFactory {

    ArrayList<? extends Graph> getGraphList(BBox bBox);

    Graph getGraph(PointModel pm1, PointModel pm2);

    ApproachModel calcApproach(PointModel pointModel, int closeThreshold);

    ApproachModel validateApproachModel(ApproachModel approachModel);

    GraphAlgorithm getAlgorithmForGraph(Graph graph, RoutingProfile routingProfile);


    void connectApproach2Graph(Graph graph, ApproachModel approachModel);

    void disconnectApproach2Graph(Graph graph, ApproachModel approachModel);


    void clearCache();

    void resetCosts();

    void serviceCache();

    void onDestroy();

}
