package mg.mgmap.generic.graph.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.GraphAlgorithm;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.Observable;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.basic.MemoryUtil;

public abstract class GGraphAlgorithm implements GraphAlgorithm {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    protected final GGraph graph;
    protected final RoutingProfile routingProfile;
    protected Observable routeIntermediatesObservable;
    private boolean running = false;


    public GGraphAlgorithm(GGraph graph, RoutingProfile routingProfile) {
        this.graph = graph;
        this.routingProfile = routingProfile;
    }

    public MultiPointModel perform(ApproachModel sourceApproachModel, ApproachModel targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList){
        running = true;
        new Thread(() -> {
            while (running){
                synchronized (GGraphAlgorithm.this){
                    try {
                        GGraphAlgorithm.this.wait(1000);
                        if (running && (routeIntermediatesObservable != null)){
                            routeIntermediatesObservable.setChanged();
                            routeIntermediatesObservable.notifyObservers( getBestPath() );
                        }
                    } catch (InterruptedException e) {
                        mgLog.e(e.getMessage());
                    }
                }
            }
            if (routeIntermediatesObservable != null){
                routeIntermediatesObservable.setChanged();
                routeIntermediatesObservable.notifyObservers(null);
            }
        }).start();
        MultiPointModel mpm =  performAlgo(sourceApproachModel, targetApproachModel, costLimit, refreshRequired, relaxedList);
        running = false;
        synchronized (GGraphAlgorithm.this){
            GGraphAlgorithm.this.notify();
        }
        return mpm;
    }
    public abstract MultiPointModel performAlgo(ApproachModel sourceApproachModel, ApproachModel targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList);

    public abstract ArrayList<MultiPointModel> getBestPath();

    public abstract String getResult();

    public void setRouteIntermediatesObservable(Observable routeIntermediatesObservable) {
        this.routeIntermediatesObservable = routeIntermediatesObservable;
    }

    protected boolean preNodeRelax(GNode node){
        if ( graph.preNodeRelax(node)){
            if (MemoryUtil.checkLowMemory(GGraphTileFactory.LOW_MEMORY_THRESHOLD)){
                mgLog.w("abort routing due low memory");
                return true;
            }
        }
        return false;
    }

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
