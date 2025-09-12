/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.generic.graph.impl2;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.MGLog;

/**
 * Implementation of the AStar algorithm.
 */
@SuppressWarnings("unused")
public class AStar extends GGraphAlgorithm {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private TreeSet<GNodeRef> prioQueue = null;
    protected GNode source = null;
    protected GNode target = null;
    protected GNodeRef bestHeuristicRef = null;

    private long duration = 0;
    private int cntTotal = 0;
    private int cntRelaxed = 0;
    private int cntSettled = 0;
    private int resultPathLength = 0;

    public AStar(GGraph graph, RoutingProfile routingProfile) {
        super(graph, routingProfile);
    }

    public MultiPointModel performAlgo(ApproachModel sourceApproachModel, ApproachModel targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList){
        prioQueue = new TreeSet<>();
        if (relaxedList != null) relaxedList.clear();

        this.source = (sourceApproachModel instanceof ApproachModelImpl ami)?ami.getApproachNode():null;
        this.target = (targetApproachModel instanceof ApproachModelImpl ami)?ami.getApproachNode():null;
        if ((source == null) || (target == null)) return null; // should never happen
        long tStart = System.currentTimeMillis();
        GNodeRef refSource = new GNodeRef(source,0,null,null, routingProfile.heuristic(source,target));
        source.resetNodeRefs();
        source.setNodeRef(refSource);
        prioQueue.add(refSource);
        target.resetNodeRefs();
        mgLog.d(()->String.format(Locale.ENGLISH, "Source: lat=%.6f lon=%.6f ele=%.2f cost=%.2f heuristic=%.2f hcost=%.2f",source.getLat(),source.getLon(),source.getEle(),
                refSource.getCost(),refSource.getHeuristic(),refSource.getHeuristicCost()));
        mgLog.d(()->String.format(Locale.ENGLISH, "Target: lat=%.6f lon=%.6f ele=%.2f",target.getLat(),target.getLon(),target.getEle()));

        GNodeRef ref = prioQueue.first();
        boolean lowMemory = false;
        while ((ref != null) && (ref.getNode() != target) && (ref.getHeuristicCost() <= costLimit) && (!lowMemory)){ // abort  if target reached or if there are no more nodes to settle or costLimit reached or lowMemory
            GNode node = ref.getNode();
            if (node.getNodeRef() == ref){ // if there was already a better path to node found, then node.getNodeRef points to this -> then we ca skip this entry of the prioQueue
                if (refreshRequired.get() != 0) break;
                lowMemory = preNodeRelax(node); // add lazy expansion of GGraphMulti
                GNeighbour neighbour = null; // start relax all neighbours
                while ((neighbour = (neighbour==null)?node.getNeighbour():neighbour.getNextNeighbour()) != null){
                    GNode neighbourNode = neighbour.getNeighbourNode();
                    float costToNeighbour = neighbour.getCost();
                    if (costToNeighbour < 0){
                        costToNeighbour = costToNeighbour(node, neighbour, neighbourNode);
                        neighbour.setCost(costToNeighbour);
                    }
                    double currentCost = ref.getCost() + costToNeighbour; // calc cost on current relaxed path
                    // create new prioQueue entry, if there is currently none or if the current relaxted path has better cost
                    GNodeRef neighbourRef = neighbourNode.getNodeRef();
                    if ((neighbourRef == null) || (currentCost < neighbourRef.getCost() )){
                        neighbourRef = new GNodeRef(neighbourNode,currentCost,node,neighbour, routingProfile.heuristic(neighbourNode,target));
                        neighbourNode.setNodeRef(neighbourRef);
                        prioQueue.add(neighbourRef);
                        if (neighbourRef.getHeuristicCost() < ref.getHeuristicCost()){
                            mgLog.e("Inconsistency detected");
                        }
                    }
                }
                ref.setSettled(true);
                if (( bestHeuristicRef == null) || (ref.getHeuristic() < bestHeuristicRef.getHeuristic())){
                    bestHeuristicRef = ref;
                }
            }
            ref = prioQueue.higher(ref);
        }
        duration = System.currentTimeMillis() - tStart;

        // generate statistics
        cntTotal = 0;
        cntRelaxed = 0;
        cntSettled = 0;
        resultPathLength = 0;
        for (GNode node : graph.getGNodes()){
            if (node.getNeighbour() != null){
                cntTotal++;
                if (node.getNodeRef() != null){
                    cntRelaxed++;
                    if (relaxedList != null) relaxedList.add(node);
                    if (node.getNodeRef().isSetteled()){
                        cntSettled++;
                    }
                }
            }
        }
        MultiPointModel resultPath = getPath(target.getNodeRef());
        resultPathLength = resultPath.size();
        return resultPath;
    }

    public ArrayList<MultiPointModel> getBestPath(){
        ArrayList<MultiPointModel> paths = new ArrayList<>();
        paths.add(getPath(bestHeuristicRef));
        return paths;
    }

    public String getResult(){
        String res = "\n";
        res += String.format(Locale.GERMAN, "%s  tiles: %d, graphNodes: %d, relaxed: %d, settled: %d, duration %dms\n",this.getClass().getSimpleName(),graph.getTileCount(), cntTotal,cntRelaxed, cntSettled, duration);
        if (target == null){
            res += "No search for target path.";
        } else if (target.getNodeRef() == null) {
            res += "no path found";
        } else {
            res += String.format(Locale.GERMAN,"target path found - hop count=%d cost=%.2f",resultPathLength,target.getNodeRef().getCost());
        }
        return res;
    }

}
