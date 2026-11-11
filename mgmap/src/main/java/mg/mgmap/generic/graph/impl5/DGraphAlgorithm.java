package mg.mgmap.generic.graph.impl5;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.graph.GraphAlgorithm;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.Observable;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.basic.MemoryUtil;

public abstract class DGraphAlgorithm implements GraphAlgorithm {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    protected final DGraphMulti graph;
    protected final RoutingProfile routingProfile;
    protected Observable routeIntermediatesObservable;
    volatile private boolean running = false;


    public DGraphAlgorithm(Graph graph, RoutingProfile routingProfile) {
        if (graph instanceof DGraphMulti dGraphMulti){
            this.graph = dGraphMulti;
        } else {
            throw new IllegalArgumentException("Unexpected graph type: "+graph.getClass().getName());
        }

        this.routingProfile = routingProfile;
    }

    public MultiPointModel perform(ApproachModel sourceApproachModel, ApproachModel targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList){

        running = true;
        new Thread(() -> {
            while (running) {
                synchronized (DGraphAlgorithm.this) {
                    try {
                        DGraphAlgorithm.this.wait(1000);
                        if (running && (routeIntermediatesObservable != null)) {
                            routeIntermediatesObservable.setChanged();
                            routeIntermediatesObservable.notifyObservers(getBestPath());
                        }
                    } catch (InterruptedException e) {
                        mgLog.e(e.getMessage());
                    }
                }
            }
            if (routeIntermediatesObservable != null) {
                routeIntermediatesObservable.setChanged();
                routeIntermediatesObservable.notifyObservers(null);
            }
        }).start();
        MultiPointModel mpm =  performAlgo((DApproach) sourceApproachModel, (DApproach) targetApproachModel, costLimit, refreshRequired, relaxedList);
        if ((mpm instanceof MultiPointModelImpl mpmi) && (mpmi.size() > 2)){
            MultiPointModelImpl mpmi2 = new MultiPointModelImpl(); // copy, but remove duplicates
            Iterator<PointModel> itMpm = mpmi.iterator();
            PointModel lastPM = null;
            while (itMpm.hasNext()){
                PointModel currentPM = itMpm.next();
                if (!currentPM.equals(lastPM)){
                    mpmi2.addPoint(currentPM);
                }
                lastPM = currentPM;
            }
            mpm = mpmi2;
        }
        running = false;
        synchronized (DGraphAlgorithm.this){
            DGraphAlgorithm.this.notify();
        }
        return mpm;
    }

    public abstract MultiPointModel performAlgo(DApproach sourceApproachModel, DApproach targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList);

    public abstract ArrayList<MultiPointModel> getBestPath();

    public abstract String getResult();

    public void setRouteIntermediatesObservable(Observable routeIntermediatesObservable) {
        this.routeIntermediatesObservable = routeIntermediatesObservable;
    }

    protected boolean preNodeRelax(DNode node, byte border){
        if ( graph.preNodeRelax(node, border)){
            if (MemoryUtil.checkLowMemory(Config.LOW_MEMORY_THRESHOLD)){
                mgLog.w("abort routing due low memory");
                return true;
            }
        }
        return false;
    }

}
