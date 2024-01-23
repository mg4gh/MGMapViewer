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
package mg.mgmap.generic.graph;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.MGLog;

/**
 * Implementation of the Dijkstra algorithm.
 */
public class Dijkstra {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    protected final GGraphMulti graph;
    protected final RoutingProfile routingProfile;

    private TreeSet<GNodeRef> prioQueue = null;
    protected GNode target = null;

    private long duration = 0;
    private int cntTotal = 0;
    private int cntRelaxed = 0;
    private int cntSettled = 0;
    private int resultPathLength = 0;

    public Dijkstra(GGraphMulti graph, RoutingProfile routingProfile) {
        this.graph = graph;
        this.routingProfile = routingProfile;
    }


    public List<GNodeRef> perform(GNode source, GNode target, double costLimit, ArrayList<PointModel> relaxedList){
        List<GNode> sources = new ArrayList<>();
        sources.add(source);
        return perform(sources,target,costLimit,relaxedList);
    }

    public List<GNodeRef> perform(List<GNode> sources, GNode target, double costLimit, ArrayList<PointModel> relaxedList){
        prioQueue = new TreeSet<>();
        resetNodeRefs();
        if (relaxedList != null) relaxedList.clear();

        this.target = target;
        long tStart = System.currentTimeMillis();
        for (GNode source : sources){
            GNodeRef ref = new GNodeRef(source,0,null,null, heuristic(source));
            source.setNodeRef(ref);
            prioQueue.add(ref);
            mgLog.d(()->String.format(Locale.ENGLISH, "Source: lat=%.6f lon=%.6f ele=%.2f cost=%.2f heuristic=%.2f hcost=%.2f",source.getLat(),source.getLon(),source.getEleA(),
                    ref.getCost(),ref.getHeuristic(),ref.getHeuristicCost()));
            mgLog.d(()->String.format(Locale.ENGLISH, "Target: lat=%.6f lon=%.6f ele=%.2f",target.getLat(),target.getLon(),target.getEleA()));
        }

        GNodeRef ref = prioQueue.first();
        while ((ref != null) && (ref.getNode() != target) && (ref.getHeuristicCost() <= costLimit)){ // abort  if target reached of if there are no more nodes to settle
            if (ref.getNode().getNodeRef() == ref){ // if there was already a better path to node found, then node.getNodeRef points to this -> then we ca skip this entry of the prioQueue

                GNode node = ref.getNode();
                graph.preNodeRelax(node); // add lazy expansion of GGraphMulti
                GNeighbour neighbour = ref.getNode().getNeighbour(); // start relax all neighbours
                while ((neighbour = graph.getNextNeighbour(node, neighbour)) != null){
                    GNode neighbourNode = neighbour.getNeighbourNode();
                    double costToNeighbour = neighbour.getCost();
                    if (costToNeighbour < 0){
                        costToNeighbour = routingProfile.getCost(neighbour.getWayAttributs(), node, neighbourNode);
                        neighbour.setCost(costToNeighbour);
                    }
                    double currentCost = ref.getCost() + costToNeighbour; // calc cost on current relaxed path
                    // create new prioQueue entry, if there is currently none or if the current relaxted path has better cost
                    GNodeRef neighbourRef = neighbourNode.getNodeRef();
                    if ((neighbourRef == null) || (currentCost < neighbourRef.getCost() )){
                        neighbourRef = new GNodeRef(neighbourNode,currentCost,ref.getNode(),neighbour, heuristic(neighbourNode));
                        neighbourNode.setNodeRef(neighbourRef);
                        prioQueue.add(neighbourRef);
                        if (neighbourRef.getHeuristicCost() < ref.getHeuristicCost()){
                            mgLog.e("Inconsistency detected");
                        }
                    }
                }
                ref.setSettled(true);
            }
            ref = prioQueue.higher(ref);
        }
        duration = System.currentTimeMillis() - tStart;

        // generate statistics
        cntTotal = 0;
        cntRelaxed = 0;
        cntSettled = 0;
        resultPathLength = 0;
        for (GNode node : graph.getNodes()){
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

        ArrayList<GNodeRef> resultPath = new ArrayList<>();
        if ((target != null) && (target.getNodeRef() != null)){
            ref = target.getNodeRef();
            while (ref.getPredecessor() != null){
                resultPath.add(0, ref);
                ref = ref.getPredecessor().getNodeRef();
            }
            resultPath.add(0,ref);
        }

        resultPathLength = resultPath.size();
        return resultPath;
    }


    protected double heuristic(GNode node){
        return 0;
    }

    private void resetNodeRefs(){
        for (GNode node : graph.getNodes()){
            node.setNodeRef(null);
        }
    }

    public String getResult(){
        String res = "\n";
        res += String.format(Locale.GERMAN, "%s  tiles: %d, graphNodes: %d, relaxed: %d, settled: %d, duration %dms\n",this.getClass().getSimpleName(),graph.getTileCount(), cntTotal,cntRelaxed, cntSettled, duration);
        if (target == null){
            res += "No search for target path.";
        } else if (target.getNodeRef() == null) {
            res += "no path found";
        } else {
            res += String.format(Locale.GERMAN,"traget path found - hop count=%d cost=%.2f",resultPathLength,target.getNodeRef().getCost());
        }
        return res;
    }

}
