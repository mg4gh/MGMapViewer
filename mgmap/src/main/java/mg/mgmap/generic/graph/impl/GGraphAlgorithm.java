package mg.mgmap.generic.graph.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.GraphAlgorithm;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.ExtendedPointModelImpl;
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

    public MultiPointModel perform(ApproachModel sourceApproachModel, ApproachModel targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList) {
        MultiPointModel mpm = checkApproachesOverlap(sourceApproachModel, targetApproachModel);
        if (mpm == null) { // no overlap, use regular routing algo
            running = true;
            new Thread(() -> {
                while (running) {
                    synchronized (GGraphAlgorithm.this) {
                        try {
                            GGraphAlgorithm.this.wait(1000);
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
            mpm = performAlgo(sourceApproachModel, targetApproachModel, costLimit, refreshRequired, relaxedList);
            if ((mpm instanceof MultiPointModelImpl mpmi) && (mpmi.size() > 2)) {
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
            synchronized (GGraphAlgorithm.this) {
                GGraphAlgorithm.this.notify();
            }
        }
        return mpm;
    }

    public abstract MultiPointModel performAlgo(ApproachModel sourceApproachModel, ApproachModel targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList);

    public abstract ArrayList<MultiPointModel> getBestPath();

    public abstract String getResult();

    public void setRouteIntermediatesObservable(Observable routeIntermediatesObservable) {
        this.routeIntermediatesObservable = routeIntermediatesObservable;
    }

    protected boolean preNodeRelax(GNode node) {
        if (graph.preNodeRelax(node)) {
            if (MemoryUtil.checkLowMemory(GGraphTileFactory.LOW_MEMORY_THRESHOLD)) {
                mgLog.w("abort routing due low memory");
                return true;
            }
        }
        return false;
    }

    public MultiPointModelImpl getPath(GNodeRef ref){
        MultiPointModelImpl path = new MultiPointModelImpl();
        if (ref != null){
            if (ref.isReverse()){
                while (ref.getPredecessor() != null){
                    path.addPoint( new ExtendedPointModelImpl<>(ref.getPredecessor(), ref.getNeighbour().getWayAttributs()) );
                    ref = ref.getPredecessor().getNodeRef(ref.isReverse());
                }
            } else { // none reverse
                while (ref.getPredecessor() != null){
                    path.addPoint(0, new ExtendedPointModelImpl<>(ref.getNode(), ref.getNeighbour().getWayAttributs()));
                    ref = ref.getPredecessor().getNodeRef(ref.isReverse());
                }
                path.addPoint(0,new ExtendedPointModelImpl<>(ref.getNode(), null));
            }
        }
        return path;
    }

    protected MultiPointModel checkApproachesOverlap(ApproachModel sourceApproachModel, ApproachModel targetApproachModel) {
        MultiPointModelImpl mpmi = null;
        if ((sourceApproachModel instanceof ApproachModelImpl sami) && (targetApproachModel instanceof ApproachModelImpl tami)) { // expected to be always true
            if ((sami.getNode1() == tami.getNode1()) && (sami.getNode2() == tami.getNode2())) { // source and target have same approach segment
                WayAttributs wayAttributs = (sami.getNeighbour1To2()==null)?null:sami.getNeighbour1To2().getWayAttributs();
                mpmi = new MultiPointModelImpl();
                mpmi.addPoint(new ExtendedPointModelImpl<>(sami.getApproachNode(), null));
                mpmi.addPoint(new ExtendedPointModelImpl<>(tami.getApproachNode(), wayAttributs));
            } // else approaches do not overlap
        }
        return mpmi;
    }

}
