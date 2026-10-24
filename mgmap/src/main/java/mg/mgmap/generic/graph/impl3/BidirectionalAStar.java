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
package mg.mgmap.generic.graph.impl3;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;

import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.MGLog;

/**
 * Implementation of the Dijkstra algorithm.
 */
public class BidirectionalAStar extends DGraphAlgorithm {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private TreeSet<DNodeRef> prioQueue = null;
    protected DNode source = null;
    protected DNode target = null;

    protected DNodeRef bestHeuristicRef = null;
    protected DNodeRef bestReverseHeuristicRef = null;

    protected AlgorithmStatistics statistics = null;
    protected DNode matchNode = null;

    int rc = 0;

    public BidirectionalAStar(Graph graph, RoutingProfile routingProfile) {
        super(graph, routingProfile);
    }


    public MultiPointModel performAlgo(DApproach sourceApproachModel, DApproach targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList){
        prioQueue = new TreeSet<>();
        if (relaxedList != null) relaxedList.clear();
        statistics = new AlgorithmStatistics();

        long tStart = System.currentTimeMillis();

        // take any neighbour that points to source for refSource
        DGraphTile sourceGraphTile = graph.dGraphTileFactory.getDGraphTile(sourceApproachModel.tileIdx);
        source = sourceApproachModel.approachNode;
        short neighbourOfSource = sourceGraphTile.getNdNeighbourIdx(source.nodeIdx);
        short neighbourToSource = sourceGraphTile.getNbReverseIdx(neighbourOfSource);
        DNodeRef refSource = new DNodeRef(source.tileIdx, neighbourToSource,0, heuristic(false, source));
        sourceGraphTile.nodeRefs[source.nodeIdx] = refSource;
        prioQueue.add(refSource);
        mgLog.d(()->String.format(Locale.ENGLISH, "Source: lat=%.6f lon=%.6f ele=%.2f cost=%.2f heuristic=%.2f hcost=%.2f",source.getLat(),source.getLon(),source.getEle(),
                refSource.getCost(),refSource.getHeuristic(),refSource.getHeuristicCost()));

        // take any neighbour from target (reverse neighbour points to target)
        DGraphTile targetGraphTile = graph.dGraphTileFactory.getDGraphTile(targetApproachModel.tileIdx);
        target = targetApproachModel.approachNode;
        short neighbourOfTarget = targetGraphTile.getNdNeighbourIdx(target.nodeIdx);
        DNodeRef refTarget = new DNodeRef(target.tileIdx, neighbourOfTarget,0, heuristic(true, target));
        refTarget.setReverse(true);
        targetGraphTile.reverseNodeRefs[target.nodeIdx] = refTarget;
        prioQueue.add(refTarget);
        mgLog.d(()->String.format(Locale.ENGLISH, "Target: lat=%.6f lon=%.6f ele=%.2f cost=%.2f heuristic=%.2f hcost=%.2f",target.getLat(),target.getLon(),target.getEle(),
                refTarget.getCost(),refTarget.getHeuristic(),refTarget.getHeuristicCost()));

        DNodeRef ref = prioQueue.first();
        DNodeRef reverseRef;
        double bestMatchCost = Double.MAX_VALUE;

        DNode node = new DNode();
        DNode neighbourNode = new DNode();

        while (true){
            if (ref.getHeuristicCost() > costLimit/2){ // if costLimit reached
                mgLog.i("exit performAlgo rc=5 cost limit reached: ref.getHeuristicCost()="+ref.getHeuristicCost()+" costLimit/2="+(costLimit/2));
                rc = 5;
                break;
            }

            DGraphTile nodeGraphTile = graph.dGraphTileFactory.getDGraphTile(ref.tileIdx);
            short nodeIdx = nodeGraphTile.getNbNeighbourNodeIdx( ref.getNeighbourIdx(nodeGraphTile) ); // getNeighbourIdx result depends on reverse flag of ref
            nodeGraphTile.getNodeData(node, nodeIdx); // node to be relaxed

            reverseRef = ref.reverse? nodeGraphTile.nodeRefs[nodeIdx]: nodeGraphTile.reverseNodeRefs[nodeIdx];
            if ((reverseRef != null) && (reverseRef.isSetteled())) { // match found
                mgLog.i("exit performAlgo rc=0 refNode="+node+" ref="+getRefDetails(ref)+" revRef="+getRefDetails(reverseRef));
                rc = 0;
                break;
            }

            if ((ref.reverse?nodeGraphTile.reverseNodeRefs[nodeIdx]:nodeGraphTile.nodeRefs[nodeIdx]) == ref){ // if there was already a better path to node found, then actual nodeRef entry for node points to this -> then we can skip this entry of the prioQueue
                if (refreshRequired.get() != 0) {
                    mgLog.i("exit performAlgo rc=3 refresh required");
                    rc = 3;
                    break;
                }
//                GNode node = ref.getNode();
                if (preNodeRelax(node, nodeGraphTile.getNdBorder(nodeIdx))) { // add lazy expansion of GGraphMulti
                    mgLog.i("exit performAlgo rc=4 low memory");
                    rc = 4;
                    break;
                }
                short neighbourIdx = -1; // start relax all neighbours
                while ((neighbourIdx = (neighbourIdx==-1)?nodeGraphTile.getNdNeighbourIdx(nodeIdx):nodeGraphTile.getNbNextNeighbourIdx(neighbourIdx)) != -1){
                    short neighbourNodeIdx = nodeGraphTile.getNbNeighbourNodeIdx(neighbourIdx);
                    byte border = nodeGraphTile.getNbBorder(neighbourIdx);
                    DGraphTile neighbourNodeGraphTile = (border==0)?nodeGraphTile:nodeGraphTile.neighbourTiles[border];
                    neighbourNodeGraphTile.getNodeData(neighbourNode, neighbourNodeIdx);

                    short directedNeighbourIdx = ref.isReverse()?nodeGraphTile.getNbReverseIdx(neighbourIdx):neighbourIdx;
                    DGraphTile directedNodeGraphTile = ref.isReverse()?nodeGraphTile:neighbourNodeGraphTile;
                    float costToNeighbour = directedNodeGraphTile.getNbCost(directedNeighbourIdx);
                    if (costToNeighbour < 0){
                        DNode directedNode = ref.isReverse()?neighbourNode:node;
                        DNode directedNeighbourNode = ref.isReverse()?node:neighbourNode;
                        short wayAttributesIdx = nodeGraphTile.getNbWayAttributesIdx(directedNeighbourIdx);
                        costToNeighbour = costToNeighbour(directedNode, directedNeighbourNode, nodeGraphTile.wayAttributesList.get(wayAttributesIdx), nodeGraphTile.isNbPrimaryDirection(directedNeighbourIdx));
                        directedNodeGraphTile.setNbCost(directedNeighbourIdx, costToNeighbour);
                    }
                    double currentCost = ref.getCost() + costToNeighbour; // calc cost on current relaxed path
                    // create new prioQueue entry, if there is currently none or if the current relaxed path has better cost
                    DNodeRef neighbourRef = ref.isReverse()?neighbourNodeGraphTile.reverseNodeRefs[neighbourNodeIdx]:neighbourNodeGraphTile.nodeRefs[neighbourNodeIdx];
                    if ((neighbourRef == null) || (currentCost < neighbourRef.getCost() )) { // OK, is better  - create new prioQueue entry
                        neighbourRef = new DNodeRef(directedNodeGraphTile.tileIdx, directedNeighbourIdx, currentCost, heuristic(ref.isReverse(), neighbourNode));
                        neighbourRef.setReverse(ref.isReverse());
                        neighbourNodeGraphTile.setNodeRef(neighbourNodeIdx, ref.isReverse(), neighbourRef);
                        prioQueue.add(neighbourRef);
                        if (neighbourRef.getHeuristicCost() < ref.getHeuristicCost()) {
                            mgLog.e("Inconsistency detected ");
                        }
                        double matchCost = getMatchCost(neighbourNodeGraphTile, neighbourNodeIdx);
                        if (matchCost < bestMatchCost){
                            matchNode.tileIdx = neighbourNode.tileIdx;
                            matchNode.nodeIdx = neighbourNode.nodeIdx;
                            bestMatchCost = matchCost;
                            mgLog.d("matchNode="+matchNode.tileIdx+","+matchNode.nodeIdx+" matchCost="+matchCost);
                        }
                    } // add new prioQueue entry
                } // relax neighbours loop


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
        }

        // generate statistics
        statistics.duration = System.currentTimeMillis() - tStart;
        for (DGraphTile dGraphTile : graph.dGraphTileFactory.getUsed()){
            dGraphTile.setupStatistic(statistics, relaxedList);
        }

        MultiPointModelImpl resultPath = new MultiPointModelImpl();
        DGraphTile matchGraphTile = graph.dGraphTileFactory.getDGraphTile(matchNode.tileIdx);
        if (matchNode != null){
            resultPath = getPath(matchGraphTile.getNodeRef(matchNode.nodeIdx, false));
            for (PointModel pm : getPath(matchGraphTile.getNodeRef(matchNode.nodeIdx, true)) ){
                resultPath.addPoint(pm);
            }
        }
        statistics.resultPathLength = resultPath.size();
        return resultPath;
    }

    double heuristic(boolean reverse, PointModel node){
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
        res += String.format(Locale.GERMAN, "%s  tiles: %d, graphNodes: %d, relaxed: %d, settled: %d, duration %dms\n",this.getClass().getSimpleName(),
                graph.getTileCount(), statistics.cntTotal, statistics.cntRelaxed, statistics.cntSettled, statistics.duration);
        if (matchNode.tileIdx == 0){
            res += "no path found";
        } else {
            DGraphTile matchGraphTile = graph.dGraphTileFactory.getDGraphTile(matchNode.tileIdx);
            res += String.format(Locale.GERMAN,"target path found - hop count=%d cost=%.2f", statistics.resultPathLength,getMatchCost(matchGraphTile,matchNode.nodeIdx));
        }
        return res;
    }

    protected double getMatchCost(DGraphTile dGraphTile, short nodeIdx){
        if ((nodeIdx == -1) || (dGraphTile.nodeRefs[nodeIdx] == null) || (dGraphTile.reverseNodeRefs[nodeIdx] == null)) return Double.MAX_VALUE;
        return dGraphTile.nodeRefs[nodeIdx].getCost() + dGraphTile.reverseNodeRefs[nodeIdx].getCost();
    }

    private String getRefDetails(DNodeRef ref){
        if (ref == null) return "";
        return String.format(Locale.ENGLISH, " %s settled=%b cost=%.2f heuristic=%.2f hcost=%.2f",ref.isReverse()?"rv":"fw",ref.isSetteled(),ref.getCost(),ref.getHeuristic(),ref.getHeuristicCost());
    }

}
