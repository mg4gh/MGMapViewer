package mg.mgmap.generic.graph.impl2;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.GraphAlgorithm;
import mg.mgmap.generic.model.ExtendedPointModelImpl;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Observable;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.basic.MemoryUtil;

public abstract class GGraphAlgorithm implements GraphAlgorithm {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    protected final GGraph graph;
    protected final RoutingProfile routingProfile;
    protected Observable routeIntermediatesObservable;
    volatile private boolean running = false;


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
            if (ref.isReverse()){
                while (ref.getPredecessor() != null){

                    int[] intermediates = ref.isReverse()?ref.getNeighbour().getIntermediatesPoints():ref.getNeighbour().getReverse().getIntermediatesPoints();
                    if (intermediates != null){
                        for (int i=0; i<intermediates.length/3;i++){
                            path.addPoint(ExtendedPointModelImpl.createFromLaLo(intermediates[3*i],intermediates[3*i+1],intermediates[3*i+2],ref.getNeighbour().getWayAttributs()));
                        }
                    }
                    path.addPoint( new ExtendedPointModelImpl<>(ref.getPredecessor(), ref.getNeighbour().getWayAttributs()) );
                    ref = ref.getPredecessor().getNodeRef(ref.isReverse());
                }


            } else { // none reverse
                while (ref.getPredecessor() != null){
                    path.addPoint(0, new ExtendedPointModelImpl<>(ref.getNode(), ref.getNeighbour().getWayAttributs()));
                    int[] intermediates = ref.getNeighbour().getIntermediatesPoints(); // neighbour.getReverse contains
                    if (intermediates != null){
                        for (int i=intermediates.length/3-1; i>=0;i--){ // traverse through intermediate points also in backward direction (same as through GNodeRef(s))
                            path.addPoint(0, ExtendedPointModelImpl.createFromLaLo(intermediates[3*i],intermediates[3*i+1],intermediates[3*i+2],ref.getNeighbour().getWayAttributs()));
                        }
                    }
                    ref = ref.getPredecessor().getNodeRef(ref.isReverse());
                }
                path.addPoint(0,new ExtendedPointModelImpl<>(ref.getNode(), null));
            }
        }
        return path;
    }

    float costToNeighbour(GNode node, GNeighbour neighbour, GNode neighbourNode){
        float costToNeighbour;
        int cntIntermediates = neighbour.cntIntermediates();
        if (cntIntermediates == 0){
            costToNeighbour = routingProfile.getCost(neighbour.getWayAttributs(), node, neighbourNode, neighbour.isPrimaryDirection());
        } else {
            int[] intermediatePoints = neighbour.getIntermediatesPoints();
            costToNeighbour = routingProfile.getCost(neighbour.getWayAttributs(),
                    (float) PointModelUtil.distance(node.getLat(), node.getLon(), LaLo.md2d(intermediatePoints[0]), LaLo.md2d(intermediatePoints[1])),
                    intermediatePoints[2]/PointModelUtil.ELE_FACTOR - node.getEle(), neighbour.isPrimaryDirection());
            for (int i=0; i<cntIntermediates-1; i++){
                costToNeighbour += routingProfile.getCost(neighbour.getWayAttributs(),
                        (float)PointModelUtil.distance(LaLo.md2d(intermediatePoints[3*i]), LaLo.md2d(intermediatePoints[3*i+1]), LaLo.md2d(intermediatePoints[3*i+3]), LaLo.md2d(intermediatePoints[3*i+4])),
                        (intermediatePoints[3*i+5] - intermediatePoints[3*i+2])/PointModelUtil.ELE_FACTOR, neighbour.isPrimaryDirection());
            }
            costToNeighbour += routingProfile.getCost(neighbour.getWayAttributs(),
                    (float)PointModelUtil.distance(LaLo.md2d(intermediatePoints[3*cntIntermediates-3]), LaLo.md2d(intermediatePoints[3*cntIntermediates-2]), neighbourNode.getLat(), neighbourNode.getLon()),
                    neighbourNode.getEle() - intermediatePoints[3*cntIntermediates-1]/PointModelUtil.ELE_FACTOR, neighbour.isPrimaryDirection());
        }
        return costToNeighbour;
    }


}
