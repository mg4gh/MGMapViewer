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
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.MGLog;

/**
 * Implementation of the Dijkstra algorithm.
 */
public class BidirectionalAStar extends GGraphAlgorithm {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private TreeSet<GNodeRef> prioQueue = null;
    protected GNode source = null;
    protected GNode target = null;
    protected GNodeRef bestHeuristicRef = null;
    protected GNodeRef bestReverseHeuristicRef = null;

    private long duration = 0;
    private int cntTotal = 0;
    private int cntRelaxed = 0;
    private int cntSettled = 0;
    private int resultPathLength = 0;
    protected GNode matchNode = null;

    int rc = 0;

    public BidirectionalAStar(GGraph graph, RoutingProfile routingProfile) {
        super(graph, routingProfile);
    }


    public MultiPointModel performAlgo(ApproachModel sourceApproachModel, ApproachModel targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList){
        prioQueue = new TreeSet<>();
        if (relaxedList != null) relaxedList.clear();

        this.source = (sourceApproachModel instanceof ApproachModelImpl ami)?ami.getApproachNode():null;
        this.target = (targetApproachModel instanceof ApproachModelImpl ami)?ami.getApproachNode():null;
        if ((source == null) || (target == null)) return null; // should never happen

        long tStart = System.currentTimeMillis();
        GNodeRef refSource = new GNodeRef(source,0,null,null, heuristic(false, source));
        source.resetNodeRefs();
        source.setNodeRef(refSource);
        prioQueue.add(refSource);
        mgLog.d(()->String.format(Locale.ENGLISH, "Source: lat=%.6f lon=%.6f ele=%.2f cost=%.2f heuristic=%.2f hcost=%.2f",source.getLat(),source.getLon(),source.getEle(),
                refSource.getCost(),refSource.getHeuristic(),refSource.getHeuristicCost()));
        GNodeRef refTarget = new GNodeRef(target,0,null,null, heuristic(true, target));
        refTarget.setReverse(true);
        target.resetNodeRefs();
        target.setNodeRef(refTarget);
        prioQueue.add(refTarget);
        mgLog.d(()->String.format(Locale.ENGLISH, "Target: lat=%.6f lon=%.6f ele=%.2f cost=%.2f heuristic=%.2f hcost=%.2f",target.getLat(),target.getLon(),target.getEle(),
                refTarget.getCost(),refTarget.getHeuristic(),refTarget.getHeuristicCost()));

        GNodeRef ref = prioQueue.first();
        GNodeRef reverseRef;
        double bestMatchCost = Double.MAX_VALUE;
        while (true){
            if (ref.getHeuristicCost() > costLimit/2){ // if costLimit reached
                mgLog.i("exit performAlgo rc=5 cost limit reached: ref.getHeuristicCost()="+ref.getHeuristicCost()+" costLimit/2="+(costLimit/2));
                rc = 5;
                break;
            }
            if (ref.getNode().getNodeRef(ref.isReverse()) == ref){ // if there was already a better path to node found, then node.getNodeRef points to this -> then we ca skip this entry of the prioQueue
                if (refreshRequired.get() != 0) {
                    mgLog.i("exit performAlgo rc=3 refresh required");
                    rc = 3;
                    break;
                }
                GNode node = ref.getNode();
                if (preNodeRelax(node)) { // add lazy expansion of GGraphMulti
                    mgLog.i("exit performAlgo rc=4 low memory");
                    rc = 4;
                    break;
                }
                GNeighbour neighbour = null; // start relax all neighbours
                while ((neighbour = (neighbour==null)?node.getNeighbour():neighbour.getNextNeighbour()) != null){
                    GNeighbour directedNeighbour = ref.isReverse()?neighbour.getReverse():neighbour;
                    GNode neighbourNode = neighbour.getNeighbourNode();
                    GNode directedNode = ref.isReverse()?neighbourNode:node;
                    GNode directedNeighbourNode = ref.isReverse()?node:neighbourNode;
                    float costToNeighbour = directedNeighbour.getCost();
                    if (costToNeighbour < 0){
                        costToNeighbour = costToNeighbour(directedNode, directedNeighbour, directedNeighbourNode);
                        directedNeighbour.setCost(costToNeighbour);
                    }
                    double currentCost = ref.getCost() + costToNeighbour; // calc cost on current relaxed path
                    // create new prioQueue entry, if there is currently none or if the current relaxted path has better cost
                    GNodeRef neighbourRef = neighbourNode.getNodeRef(ref.isReverse());
                    if ((neighbourRef == null) || (currentCost < neighbourRef.getCost() )){
                        neighbourRef = new GNodeRef(neighbourNode,currentCost, ref.getNode(),directedNeighbour, heuristic(ref.isReverse(), neighbourNode));
                        neighbourRef.setReverse(ref.isReverse());
                        neighbourNode.setNodeRef(neighbourRef);
                        prioQueue.add(neighbourRef);
                        if (neighbourRef.getHeuristicCost() < ref.getHeuristicCost()) {
                            mgLog.e("Inconsistency detected ");
                        }
                        double matchCost = getMatchCost(neighbourNode);
                        if (matchCost < bestMatchCost){
                            matchNode = neighbourNode;
                            bestMatchCost = matchCost;
                            mgLog.d("matchNode="+matchNode+" matchCost="+matchCost);
                        }
                    }
                }
                ref.setSettled(true);
                if (ref.isReverse()){
                    if (( bestReverseHeuristicRef == null) || (ref.getHeuristic() < bestReverseHeuristicRef.getHeuristic())){
                        bestReverseHeuristicRef = ref;
                    }
                } else {
                    if (( bestHeuristicRef == null) || (ref.getHeuristic() < bestHeuristicRef.getHeuristic())){
                        bestHeuristicRef = ref;
                    }
                }
            }
            ref = prioQueue.higher(ref);
            if (ref == null) {
                mgLog.i("exit performAlgo rc=2 ref=null, no more nodes to handle, no path found");
                rc = 2;
                break; // no more nodes to handle - no path found
            }
            reverseRef = ref.getNode().getNodeRef(!ref.isReverse());
            if ((reverseRef != null) && (reverseRef.isSetteled())) { // match found
                mgLog.i("exit performAlgo rc=0 refNode="+ref.getNode()+" ref="+getRefDetails(ref)+" revRef="+getRefDetails(reverseRef));
                rc = 0;
                break;
            }
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
                if ((node.getNodeRef() != null) || (node.getNodeRef(true) != null)){
                    cntRelaxed++;
                    if (relaxedList != null) relaxedList.add(node);
                    if (((node.getNodeRef() != null) && (node.getNodeRef().isSetteled())) ||
                            ((node.getNodeRef(true) != null) && (node.getNodeRef(true).isSetteled()))){
                        cntSettled++;
                    }
                }
            }
        }
        MultiPointModelImpl resultPath = new MultiPointModelImpl();
        if (matchNode != null){
            resultPath = getPath(matchNode.getNodeRef());
            for (PointModel pm : getPath(matchNode.getNodeRef(true))){
                resultPath.addPoint(pm);
            }
        }
        resultPathLength = resultPath.size();
        return resultPath;
    }

    double heuristic(boolean reverse, GNode node){
        double h;
        double hf = routingProfile.heuristic(node, target);
        double hr = routingProfile.heuristic(source, node);
        if (reverse){
            h = (hr - hf) / 2;
        } else {
            h = (hf - hr) / 2;
        }
        return h;
    }

    public ArrayList<MultiPointModel> getBestPath(){
        ArrayList<MultiPointModel> paths = new ArrayList<>();
        paths.add(getPath(bestHeuristicRef));
        paths.add(getPath(bestReverseHeuristicRef));
        return paths;
    }

    public String getResult(){
        String res = "\n";
        res += String.format(Locale.GERMAN, "%s  tiles: %d, graphNodes: %d, relaxed: %d, settled: %d, duration %dms\n",this.getClass().getSimpleName(),graph.getTileCount(), cntTotal,cntRelaxed, cntSettled, duration);
        if (matchNode == null){
            res += "no path found";
        } else {
            res += String.format(Locale.GERMAN,"target path found - hop count=%d cost=%.2f",resultPathLength,getMatchCost(matchNode));
        }
        return res;
    }

    protected double getMatchCost(GNode aMatchNode){
        if ((aMatchNode == null) || (aMatchNode.getNodeRef()==null) || (aMatchNode.getNodeRef(true)== null)) return Double.MAX_VALUE;
        return aMatchNode.getNodeRef().getCost()+aMatchNode.getNodeRef(true).getCost();
    }

    private String getRefDetails(GNodeRef ref){
        if (ref == null) return "";
        return String.format(Locale.ENGLISH, " %s settled=%b cost=%.2f heuristic=%.2f hcost=%.2f",ref.isReverse()?"rv":"fw",ref.isSetteled(),ref.getCost(),ref.getHeuristic(),ref.getHeuristicCost());
    }

}
