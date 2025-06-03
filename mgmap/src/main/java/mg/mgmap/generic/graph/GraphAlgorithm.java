package mg.mgmap.generic.graph;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.Observable;

public interface GraphAlgorithm {

    void setRouteIntermediatesObservable(Observable routeIntermediatesObservable);

    MultiPointModel perform(ApproachModel sourceApproachModel, ApproachModel targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList);

    String getResult();
}
