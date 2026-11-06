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
package mg.mgmap.generic.graph.impl4;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.ExtendedPointModelImpl;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
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

    AlgorithmStatistics statistics = null; // is initialized from factory
    DNode matchNode = null;
    double matchCost = Double.MAX_VALUE;

    int rc = 0;

    public BidirectionalAStar(Graph graph, RoutingProfile routingProfile) {
        super(graph, routingProfile);
    }

    void createStartEntries(DApproach am, boolean reverse){
        DGraphTile dGraphTile = graph.dGraphTileFactory.getDGraphTile(am.tileIdx);
        short wayAttributesIdx = dGraphTile.getNbWayAttributesIdx(am.neighbour1to2);
        WayAttributs wayAttributes = dGraphTile.wayAttributesList.get(wayAttributesIdx);
        boolean is12PrimaryDirection = dGraphTile.isNbPrimaryDirection(am.neighbour1to2);
        short neighbour2to1 = dGraphTile.getNbReverseIdx(am.neighbour1to2);

        createStartEntry(dGraphTile, am, am.node1Idx, neighbour2to1, wayAttributes, !is12PrimaryDirection, reverse);
        createStartEntry(dGraphTile, am, am.node2Idx, am.neighbour1to2, wayAttributes, is12PrimaryDirection, reverse);
    }

    void createStartEntry(DGraphTile dGraphTile, DApproach am, short nodeIdx, short neighbourIdx, WayAttributs wayAttributes, boolean is12PrimaryDirection, boolean reverse){ // neighbourIdx points to nodeIdx
        DNode node = new DNode();
        dGraphTile.getNodeData(node, nodeIdx);
        double distance = PointModelUtil.distance(am.approachNode, node);
        double verDistance = PointModelUtil.verticalDistance(am.approachNode, node);
        verDistance = reverse?-verDistance:verDistance;
        double cost = routingProfile.getCost(wayAttributes, (float)distance, (float)verDistance, is12PrimaryDirection);
        double heuristic = heuristic(reverse, node);
        short directedNeighbourIdx = reverse?dGraphTile.getNbReverseIdx(neighbourIdx):neighbourIdx;
        DNodeRef ref = new DNodeRef(am.tileIdx, directedNeighbourIdx, cost, heuristic);
        dGraphTile.nodeRefs[reverse?1:0][nodeIdx] = ref;
        ref.setStart(true);
        ref.setReverse(reverse);
        prioQueue.add(ref);
        checkMatchCost(dGraphTile, nodeIdx);
    }



    public MultiPointModel performAlgo(DApproach sourceApproachModel, DApproach targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList){
        prioQueue = new TreeSet<>();
        if (relaxedList != null) relaxedList.clear();
        matchNode = new DNode();

        long tStart = System.currentTimeMillis();
        source = sourceApproachModel.approachNode;
        target = targetApproachModel.approachNode;

        createStartEntries(sourceApproachModel, false);
        createStartEntries(targetApproachModel, true);

        DNodeRef ref = prioQueue.first();
        MiDNodeRef reverseRef;

        DNode node = new DNode();
        DNode neighbourNode = new DNode();

        while (true){
            if (ref.getHeuristicCost() > costLimit/2){ // if costLimit reached
                mgLog.i("exit performAlgo rc=5 cost limit reached: ref.getHeuristicCost()="+ref.getHeuristicCost()+" costLimit/2="+(costLimit/2));
                rc = 5;
                break;
            }

            DGraphTile graphTile = graph.dGraphTileFactory.getDGraphTile(ref.tileIdx);
            short refNeighbourIdx = ref.isReverse()?graphTile.getNbReverseIdx(ref.neighbourIdx):ref.neighbourIdx;
            short nodeIdx = graphTile.getNbNeighbourNodeIdx( refNeighbourIdx );
            {
                byte border = graphTile.getNbBorder(refNeighbourIdx);
                graphTile = (border==0)?graphTile:graphTile.neighbourTiles[border];
            }
            graphTile.getNodeData(node, nodeIdx); // node to be relaxed

            reverseRef = graphTile.nodeRefs[ref.isReverse()?0:1][nodeIdx];
            if ((reverseRef != null) && (reverseRef.isSettled())) { // match found
                mgLog.w("exit performAlgo rc=0 refNode="+node+" ref="+getRefDetails(ref)+" revRef="+getRefDetails(reverseRef));
                rc = 0;
                break;
            }

            if (graphTile.nodeRefs[ref.isReverse()?1:0][nodeIdx] == ref){ // if there was already a better path to node found, then actual nodeRef entry for node points to this -> then we can skip this entry of the prioQueue
                if (refreshRequired.get() != 0) {
                    mgLog.i("exit performAlgo rc=3 refresh required");
                    rc = 3;
                    break;
                }
//                GNode node = ref.getNode();
                byte nodeBorder = graphTile.getNdBorder(nodeIdx);
                if (preNodeRelax(node, nodeBorder)) { // add lazy expansion of GGraphMulti
                    mgLog.i("exit performAlgo rc=4 low memory");
                    rc = 4;
                    break;
                }
                short neighbourIdx = -1; // start relax all neighbours
                while ((neighbourIdx = (neighbourIdx==-1)?graphTile.getNdNeighbourIdx(nodeIdx):graphTile.getNbNextNeighbourIdx(neighbourIdx)) != -1){
                    short neighbourNodeIdx = graphTile.getNbNeighbourNodeIdx(neighbourIdx);
                    byte border = graphTile.getNbBorder(neighbourIdx);
                    DGraphTile neighbourGraphTile = (border==0)?graphTile:graphTile.neighbourTiles[border];
                    assert (neighbourGraphTile != null):"neighbourGraphTile is null - "+graphTile+" border="+border;
                    neighbourGraphTile.getNodeData(neighbourNode, neighbourNodeIdx);

                    short directedNeighbourIdx = ref.isReverse()?graphTile.getNbReverseIdx(neighbourIdx):neighbourIdx;

                    float costToNeighbour = graphTile.getNbCost(directedNeighbourIdx);
                    if (costToNeighbour < 0){
                        short wayAttributesIdx = graphTile.getNbWayAttributesIdx(directedNeighbourIdx);
                        WayAttributs wayAttributs = (wayAttributesIdx==-1)?null:graphTile.wayAttributesList.get(wayAttributesIdx);
                        float distance = graphTile.getNbDistance(directedNeighbourIdx);
                        float vertDistance = graphTile.getNbVertDistance(directedNeighbourIdx);
                        costToNeighbour = routingProfile.getCost(wayAttributs, distance, vertDistance, graphTile.isNbPrimaryDirection(directedNeighbourIdx));
                        graphTile.setNbCost(directedNeighbourIdx, costToNeighbour);
                    }
                    double currentCost = ref.getCost() + costToNeighbour; // calc cost on current relaxed path
                    // create new prioQueue entry, if there is currently none or if the current relaxed path has better cost
                    MiDNodeRef miNeighbourRef = neighbourGraphTile.nodeRefs[ref.isReverse()?1:0][neighbourNodeIdx];
                    if ((miNeighbourRef == null) || ((miNeighbourRef instanceof DNodeRef neighbourRef) && (currentCost < neighbourRef.getCost() ))) { // OK, is better  - create new prioQueue entry
                        if (miNeighbourRef != null) {
                            statistics.removedNodeRefs++;
                            prioQueue.remove(miNeighbourRef);
                        }
                        DNodeRef neighbourRef = new DNodeRef(graphTile.tileIdx, directedNeighbourIdx, currentCost, heuristic(ref.isReverse(), neighbourNode));
                        neighbourRef.setReverse(ref.isReverse());
                        neighbourGraphTile.setNodeRef(neighbourNodeIdx, ref.isReverse(), neighbourRef);
                        prioQueue.add(neighbourRef);
                        if (neighbourRef.getHeuristicCost() <= ref.getHeuristicCost()) {
                            mgLog.e("node="+node+ " neighbourNode="+neighbourNode);
                            mgLog.e("Inconsistency detected -- neighbourRef.getHeuristicCost="+neighbourRef.getHeuristicCost()+"  ref.getHeuristicCost="+ref.getHeuristicCost());
                        }
                        checkMatchCost(neighbourGraphTile, neighbourNodeIdx);
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
            DNodeRef lastRef = ref;
            ref = prioQueue.higher(ref);
            prioQueue.remove(lastRef);
            if ((lastRef.tileIdx != matchNode.tileIdx) || (nodeIdx != matchNode.nodeIdx)){
                graphTile.nodeRefs[lastRef.isReverse()?1:0][nodeIdx] = new MiDNodeRef(lastRef); // replace dNodeRef with MiDNodeRef, which consumes less memory
            }
            if (ref == null) {
                mgLog.i("exit performAlgo rc=2 ref=null, no more nodes to handle, no path found");
                rc = 2;
                break; // no more nodes to handle - no path found
            }
        }

        // generate statistics
        statistics.duration = System.currentTimeMillis() - tStart;
        statistics.matchCost = matchCost;
        for (DGraphTile dGraphTile : graph.dGraphTileFactory.getUsed()){
            dGraphTile.setupStatistic(statistics);
        }
        statistics.prioQueueSize = prioQueue.size();

        MultiPointModelImpl resultPath = new MultiPointModelImpl();
        if (matchNode.tileIdx != 0){
            DGraphTile matchGraphTile = graph.dGraphTileFactory.getDGraphTile(matchNode.tileIdx);
            resultPath = getPath(matchGraphTile.getNodeRef(matchNode.nodeIdx, false), matchNode);
            for (PointModel pm : getPath(matchGraphTile.getNodeRef(matchNode.nodeIdx, true), matchNode) ){
                resultPath.addPoint(pm);
            }
            resultPath.addPoint(target);
            resultPath.addPoint(0, source);
        }

        // checkDirectConnect
        if ((sourceApproachModel.tileIdx == targetApproachModel.tileIdx) && (sourceApproachModel.neighbour1to2 == targetApproachModel.neighbour1to2)){
            DGraphTile graphTile = graph.dGraphTileFactory.getDGraphTile(sourceApproachModel.tileIdx); // same tile for source and target
            DNode node1 = new DNode();
            graphTile.getNodeData(node1, sourceApproachModel.node1Idx);

            WayAttributs wayAttributes = graphTile.wayAttributesList.get(graphTile.getNbWayAttributesIdx(sourceApproachModel.neighbour1to2));
            double distance = PointModelUtil.distance(source, target);
            double verDistance = PointModelUtil.verticalDistance(source, target);
            boolean primaryDirection = graphTile.isNbPrimaryDirection(sourceApproachModel.neighbour1to2);
            primaryDirection = (PointModelUtil.distance(source, node1) < PointModelUtil.distance(target, node1)) == primaryDirection;
            double directCost = routingProfile.getCost(wayAttributes, (float)distance, (float)verDistance, primaryDirection);
            if (directCost < matchCost){
                resultPath = new MultiPointModelImpl(); // drop calculated route, since direct connection is better
                resultPath.addPoint( new ExtendedPointModelImpl<>(source, null) );
                resultPath.addPoint( new ExtendedPointModelImpl<>(target, wayAttributes) );
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

    public MultiPointModelImpl getPath(MiDNodeRef ref, DNode baseNode){
        MultiPointModelImpl path = new MultiPointModelImpl();
        if (ref != null){
            if (ref.isReverse()){
                DNode neighbourNode = null;
                while (!ref.isStart()){
                    DGraphTile neighbourGraphTile = graph.dGraphTileFactory.getDGraphTile(ref.tileIdx);
                    short neighbourNodeIdx = neighbourGraphTile.getNbNeighbourNodeIdx(ref.neighbourIdx);
                    neighbourNode = neighbourGraphTile.getNode(neighbourNodeIdx);
                    setDuration(neighbourGraphTile, neighbourNode, ref.neighbourIdx);
                    path.addPoint( neighbourNode);

//                    short wayAttributesIdx = neighbourGraphTile.getNbWayAttributesIdx(ref.neighbourIdx);
//                    WayAttributs wayAttributs = (wayAttributesIdx==-1)?null:neighbourGraphTile.wayAttributesList.get(wayAttributesIdx);
//                    path.addPoint( new ExtendedPointModelImpl<>(neighbourNode, wayAttributs) );
                    ref = neighbourGraphTile.getNodeRef(neighbourNodeIdx, ref.isReverse());
                    if (ref == null) {
                        mgLog.e("Inconsistency detected");
                        break;
                    }
                }

                if (ref != null) { // ref == null should never happen, just for defensive programming
                    if (neighbourNode != null){
                        DGraphTile neighbourGraphTile = graph.dGraphTileFactory.getDGraphTile(ref.tileIdx);
                        setDuration(neighbourGraphTile, target, neighbourGraphTile.getNbWayAttributesIdx(ref.neighbourIdx), (float)PointModelUtil.distance(neighbourNode, target), PointModelUtil.verticalDistance(neighbourNode, target));
                    } else if (baseNode != null){
                        assert (ref.tileIdx == baseNode.tileIdx);
                        DGraphTile baseGraphTile = graph.dGraphTileFactory.getDGraphTile(baseNode.tileIdx);
                        baseGraphTile.getNodeData(baseNode, baseNode.nodeIdx);
                        setDuration(baseGraphTile, target, baseGraphTile.getNbWayAttributesIdx(ref.neighbourIdx), (float)PointModelUtil.distance(baseNode, target), PointModelUtil.verticalDistance(baseNode, target));
                    }
                } else {
                    mgLog.e("Inconsistency detected");
                }
                path.addPoint( target );


            } else { // none reverse
                ArrayList<MiDNodeRef> refList = new ArrayList<>();
                while (!ref.isStart()){
                    if (!refList.contains(ref)){
                        refList.add(ref);
                    } else {
                        mgLog.e("Inconsistency detected");
                        mgLog.w(refList);
                        mgLog.w("ref="+ref);
                        break;
                    }
                    DGraphTile neighbourGraphTile = graph.dGraphTileFactory.getDGraphTile(ref.tileIdx);
                    short neighbourNodeIdx = neighbourGraphTile.getNbNeighbourNodeIdx(ref.neighbourIdx);
                    short border = neighbourGraphTile.getNbBorder(ref.neighbourIdx);
                    DGraphTile neighbourNodeGraphTile = neighbourGraphTile.neighbourTiles[border];
                    DNode neighbourNode = neighbourNodeGraphTile.getNode(neighbourNodeIdx);
                    setDuration(neighbourGraphTile, neighbourNode, ref.neighbourIdx);
                    path.addPoint( 0, neighbourNode );

                    short reverseNeighbourIdx = neighbourGraphTile.getNbReverseIdx(ref.neighbourIdx);
                    short reverseNeighbourNodeIdx = neighbourGraphTile.getNbNeighbourNodeIdx(reverseNeighbourIdx); // idx of neighbourNode of reverseNeighbour
                    ref = neighbourGraphTile.getNodeRef(reverseNeighbourNodeIdx, ref.isReverse());
                    if (ref == null) {
                        mgLog.e("Inconsistency detected");
                        break;
                    }
                }

                if (ref != null){ // ref == null should never happen, just for defensive programming
                    DGraphTile neighbourGraphTile = graph.dGraphTileFactory.getDGraphTile(ref.tileIdx);
                    short neighbourNodeIdx = neighbourGraphTile.getNbNeighbourNodeIdx(ref.neighbourIdx);
                    DNode neighbourNode = neighbourGraphTile.getNode(neighbourNodeIdx);
                    setDuration(neighbourGraphTile, neighbourNode, neighbourGraphTile.getNbWayAttributesIdx(ref.neighbourIdx), (float)PointModelUtil.distance(source, neighbourNode), PointModelUtil.verticalDistance(source, neighbourNode));
                    path.addPoint( 0, neighbourNode );
//                    path.addPoint( 0, new ExtendedPointModelImpl<>(neighbourNode, null) );
                }

                source.setTimestamp(0);
                path.addPoint( 0, source );
            }
        }
        return path;
    }

    void setDuration(DGraphTile graphTile, DNode node, short neighbourIdx){
        short wayAttributesIdx = graphTile.getNbWayAttributesIdx(neighbourIdx);
        setDuration(graphTile, node, wayAttributesIdx, graphTile.getNbDistance(neighbourIdx), graphTile.getNbVertDistance(neighbourIdx));
    }
    void setDuration(DGraphTile graphTile, DNode node, short wayAttributesIdx, float distance, float vertDistance){
        WayAttributs wayAttributs = (wayAttributesIdx==-1)?null:graphTile.wayAttributesList.get(wayAttributesIdx);
        long duration = routingProfile.getDuration(wayAttributs, distance, vertDistance);
        node.setTimestamp(duration);
    }


    public ArrayList<MultiPointModel> getBestPath(){
        ArrayList<MultiPointModel> paths = new ArrayList<>();
        paths.add(getPath(bestHeuristicRef, null));
        paths.add(getPath(bestReverseHeuristicRef, null));
        return paths;
    }

    public String getResult(){
        String res = "\n";
        res += String.format(Locale.GERMAN, "%s  tiles: %d, graphNodes: %d, relaxed: %d, settled: %d, duration %dms, removedNodeRefs=%d\n",this.getClass().getSimpleName(),
                graph.getTileCount(), statistics.cntTotal, statistics.cntRelaxed, statistics.cntSettled, statistics.duration, statistics.removedNodeRefs);
        if (matchNode.tileIdx == 0){
            res += "no path found";
        } else {
            res += String.format(Locale.GERMAN,"target path found - hop count=%d cost=%.2f", statistics.resultPathLength, statistics.matchCost);
        }
        return res;
    }

    void checkMatchCost(DGraphTile dGraphTile, short nodeIdx){
        if ((nodeIdx != -1) && (dGraphTile.nodeRefs[0][nodeIdx] instanceof DNodeRef nodeRef) && (dGraphTile.nodeRefs[1][nodeIdx] instanceof DNodeRef reverseNodeRef) ){
            double nodeMatchCost = nodeRef.getCost() + reverseNodeRef.getCost();
            if (nodeMatchCost < matchCost){
                matchNode.tileIdx = dGraphTile.tileIdx;
                matchNode.nodeIdx = nodeIdx;
                matchCost = nodeMatchCost;
                mgLog.d("matchNode="+matchNode.tileIdx+","+matchNode.nodeIdx+" matchCost="+nodeMatchCost);
            }
        }
    }

    private String getRefDetails(MiDNodeRef ref){
        if (ref instanceof DNodeRef dNodeRef){
            return String.format(Locale.ENGLISH, " %s settled=%b cost=%.2f heuristic=%.2f hcost=%.2f",ref.isReverse()?"rv":"fw",ref.isSettled(),dNodeRef.getCost(),dNodeRef.getHeuristic(),dNodeRef.getHeuristicCost());
        }
        if (ref != null){
            return String.format(Locale.ENGLISH, " %s settled=%b ",ref.isReverse()?"rv":"fw",ref.isSettled());
        }
        return "";
    }

}
