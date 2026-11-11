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
package mg.mgmap.generic.graph.impl5;

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
    protected DApproach sourceApproachModel = null;
    protected DNode source = null;
    protected DApproach targetApproachModel = null;
    protected DNode target = null;

    double bestHeuristicValue = Double.MAX_VALUE;
    DNode bestHeuristicNode = new DNode();
    double bestReverseHeuristicValue = Double.MAX_VALUE;
    DNode bestReverseHeuristicNode = new DNode();

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
        short neighbourRefIdx = DGraphTile.findFreeERefIdx();
        DNodeRef ref = DGraphTile.eRefs[neighbourRefIdx].init(am.tileIdx, directedNeighbourIdx, reverse, cost, heuristic);
        dGraphTile.setNdRef(nodeIdx, reverse, (short)(neighbourRefIdx | DGraphTile.ND_DNODEREF_MARKER));
        prioQueue.add(ref);
        checkMatchCost(dGraphTile, nodeIdx);
    }



    public MultiPointModel performAlgo(DApproach sourceApproachModel, DApproach targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList){
        prioQueue = new TreeSet<>();
        if (relaxedList != null) relaxedList.clear();
        for (DNodeRef nodeRef : DGraphTile.eRefs){
            nodeRef.setInvalid();
        }

        matchNode = new DNode();

        long tStart = System.currentTimeMillis();
        this.sourceApproachModel = sourceApproachModel;
        source = sourceApproachModel.approachNode;
        this.targetApproachModel = targetApproachModel;
        target = targetApproachModel.approachNode;

        createStartEntries(sourceApproachModel, false);
        createStartEntries(targetApproachModel, true);

        DNodeRef ref = prioQueue.first();
        short reverseRefIdx;

        DNode node = new DNode();
        DNode neighbourNode = new DNode();
        boolean prioQueueRefNotFirstReported = false;

        while (true){
            if (ref.getHeuristicCost() > costLimit/2){ // if costLimit reached
                mgLog.i("exit performAlgo rc=5 cost limit reached: ref.getHeuristicCost()="+ref.getHeuristicCost()+" costLimit/2="+(costLimit/2));
                rc = 5;
                break;
            }

            DGraphTile graphTile = graph.dGraphTileFactory.getDGraphTile(ref.tileIdx);
            short refNeighbourIdx = ref.isReverse()?graphTile.getNbReverseIdx(ref.neighbourIdx):ref.neighbourIdx;
            short nodeIdx = graphTile.getNbNeighbourNodeIdx( refNeighbourIdx );
            graphTile.getNodeData(node, nodeIdx); // node to be relaxed

            reverseRefIdx = graphTile.getNdRef(nodeIdx, !ref.isReverse());
            if (reverseRefIdx >= 0) { // reverseRef is settled -> match found
                mgLog.w("exit performAlgo rc=0 refNode="+node);
                rc = 0;
                break;
            }

            if (refreshRequired.get() != 0) {
                mgLog.i("exit performAlgo rc=3 refresh required");
                rc = 3;
                break;
            }

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
                if (neighbourGraphTile.getNdRef(neighbourNodeIdx, ref.isReverse()) >= 0) continue; // neighbourNode in own direction is already settled (this check seems not relevant for performance)
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
                short oldNeighbourRefIdx = neighbourGraphTile.getNdRef(neighbourNodeIdx, ref.isReverse());
                if ((oldNeighbourRefIdx == -1) || ((oldNeighbourRefIdx < -1) && (DGraphTile.eRefs[DGraphTile.eRefIdx(oldNeighbourRefIdx)].getCost() > currentCost))) { // OK, is better  - create new prioQueue entry
                    if (oldNeighbourRefIdx < -1) { // there is an old prioQueue entry
                        DNodeRef oldDNodeRef = DGraphTile.eRefs[DGraphTile.eRefIdx(oldNeighbourRefIdx)];
                        statistics.removedNodeRefs++;
                        prioQueue.remove(oldDNodeRef);
                        oldDNodeRef.setInvalid();
                    }
                    short neighbourRefIdx = DGraphTile.findFreeERefIdx();
                    if (neighbourRefIdx == -1) {
                        mgLog.e("exit performAlgo rc=6 no free DNodeRef object found");
                        rc = 6;
                        break;
                    }
                    DNodeRef dNodeRef = DGraphTile.eRefs[neighbourRefIdx];
                    //  in case of border transit replace directedNeighbourIdx with the corresponding idx in neighbour tile (needed the find path back)
                    if (border != 0){
                        short neighbourIdxInNeighbourTile = neighbourGraphTile.findNeighbourIdx(neighbourNodeIdx, Border.oppositeBorder(border), node);
                        directedNeighbourIdx = ref.isReverse()?neighbourIdxInNeighbourTile:neighbourGraphTile.getNbReverseIdx(neighbourIdxInNeighbourTile);
                        assert (directedNeighbourIdx != -1);
                    }
                    dNodeRef.init(neighbourGraphTile.tileIdx, directedNeighbourIdx, ref.isReverse(), currentCost, heuristic(ref.isReverse(), neighbourNode));
                    neighbourGraphTile.setNdRef(neighbourNodeIdx, ref.isReverse(), (short)(neighbourRefIdx | DGraphTile.ND_DNODEREF_MARKER));

                    if (ref.getHeuristicCost() >= dNodeRef.getHeuristicCost()){
                        mgLog.e("inconsistency in heuristicCost monotony: current="+ref+" new="+dNodeRef);
                    }
                    if (!prioQueue.add(dNodeRef)){
                        DNodeRef other = prioQueue.ceiling(dNodeRef);
                        mgLog.e("error nodeRef not added: "+dNodeRef+" due to existing entry:: "+other);
                    }

                    if (dNodeRef.getHeuristicCost() <= ref.getHeuristicCost()) {
                        mgLog.e("node="+node+ " neighbourNode="+neighbourNode);
                        mgLog.e("Inconsistency detected -- dNodeRef.getHeuristicCost="+dNodeRef.getHeuristicCost()+"  ref.getHeuristicCost="+ref.getHeuristicCost());
                    }
                    checkMatchCost(neighbourGraphTile, neighbourNodeIdx);
                } // create new prioQueue entry
            } // relax neighbours loop

            if (rc != 0){
                break;
            }

            if (ref.isReverse()){
                if (ref.getHeuristic() < bestReverseHeuristicValue){
                    bestReverseHeuristicNode.init(node);
                    bestReverseHeuristicValue = ref.getHeuristic();
                }
            } else {
                if (ref.getHeuristic() < bestHeuristicValue){
                    bestHeuristicNode.init(node);
                    bestHeuristicValue = ref.getHeuristic();
                }
            }

            DNodeRef lastRef = ref;
            ref = prioQueue.higher(ref);
            prioQueue.remove(lastRef);
            lastRef.setInvalid();

            graphTile.setNdRef(nodeIdx, lastRef.isReverse(), lastRef.neighbourIdx);

            if (prioQueue.first() != ref){
                if (!prioQueueRefNotFirstReported) mgLog.e("prioQueue.first() != ref");
            }

            if (ref == null) {
                mgLog.w("exit performAlgo rc=2 ref=null, no more nodes to handle, no path found");
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
            resultPath = getPath(matchNode, false, sourceApproachModel);
            for (PointModel pm : getPath(matchNode, true, targetApproachModel)){
                resultPath.addPoint(pm);
            }
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

    @SuppressWarnings({"ConstantValue"})
    public MultiPointModelImpl getPath(DNode baseNode, boolean reverse, DApproach approachModel){
        MultiPointModelImpl path = new MultiPointModelImpl();

        if ((baseNode != null) && (baseNode.nodeIdx != -1)){
            DGraphTile baseGraphTile = graph.dGraphTileFactory.getDGraphTile(baseNode.tileIdx);
            DNode node = baseGraphTile.getNode(baseNode.nodeIdx);

            DGraphTile graphTile;
            short neighbourIdx;
            if (reverse){
                while (true){
                    graphTile = graph.dGraphTileFactory.getDGraphTile(node.tileIdx);
                    short refIdx = graphTile.getNdRef(node.nodeIdx, reverse);
                    neighbourIdx = (refIdx >= 0)?refIdx:DGraphTile.eRefs[DGraphTile.eRefIdx(refIdx)].neighbourIdx;

                    if ((approachModel.tileIdx == node.tileIdx) && ((approachModel.node1Idx == node.nodeIdx) || (approachModel.node2Idx == node.nodeIdx))) break;

                    short neighbourNodeIdx = graphTile.getNbNeighbourNodeIdx(neighbourIdx);
                    short border = graphTile.getNbBorder(neighbourIdx);
                    DGraphTile neighbourNodeGraphTile = graphTile.neighbourTiles[border];
                    DNode neighbourNode = neighbourNodeGraphTile.getNode(neighbourNodeIdx);
                    setDuration(graphTile, neighbourNode, neighbourIdx);
                    path.addPoint( neighbourNode);

                    node = neighbourNode;
                }

                setDuration(graphTile, target, graphTile.getNbWayAttributesIdx(neighbourIdx), (float)PointModelUtil.distance(node, target), PointModelUtil.verticalDistance(node, target));
                path.addPoint( target );

            } else { // none reverse
                while (true){
                    graphTile = graph.dGraphTileFactory.getDGraphTile(node.tileIdx);
                    short refIdx = graphTile.getNdRef(node.nodeIdx, reverse);
                    neighbourIdx = (refIdx >= 0)?refIdx:DGraphTile.eRefs[DGraphTile.eRefIdx(refIdx)].neighbourIdx;

                    if ((approachModel.tileIdx == node.tileIdx) && ((approachModel.node1Idx == node.nodeIdx) || (approachModel.node2Idx == node.nodeIdx))) break;

                    setDuration(graphTile, node, neighbourIdx);
                    path.addPoint( 0, node );

                    short reverseNeighbourIdx = graphTile.getNbReverseIdx(neighbourIdx);
                    short neighbourNodeIdx = graphTile.getNbNeighbourNodeIdx(reverseNeighbourIdx);
                    byte border = graphTile.getNbBorder(reverseNeighbourIdx);
                    DGraphTile neighbourNodeGraphTile = graphTile.neighbourTiles[border];
                    node = neighbourNodeGraphTile.getNode(neighbourNodeIdx);
                }

                setDuration(graphTile, node, graphTile.getNbWayAttributesIdx(neighbourIdx), (float)PointModelUtil.distance(source, node), PointModelUtil.verticalDistance(source, node));
                path.addPoint( 0, node );
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
        paths.add(getPath(bestHeuristicNode, false, sourceApproachModel));
        paths.add(getPath(bestReverseHeuristicNode, true, targetApproachModel));
        mgLog.w(String.format(Locale.ENGLISH, "best heuristic remaining distance: %.1f", PointModelUtil.distance(bestHeuristicNode, bestReverseHeuristicNode)));
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
        if (nodeIdx == -1) return;
        short fwRef = dGraphTile.getNdRef(nodeIdx, false);
        short rvRef = dGraphTile.getNdRef(nodeIdx, true);
        if ((fwRef < -1) && (rvRef < -1)){
            double nodeMatchCost = DGraphTile.eRefs[DGraphTile.eRefIdx(fwRef)].getCost() + DGraphTile.eRefs[DGraphTile.eRefIdx(rvRef)].getCost();
            if (nodeMatchCost < matchCost){
                matchNode.tileIdx = dGraphTile.tileIdx;
                matchNode.nodeIdx = nodeIdx;
                matchCost = nodeMatchCost;
                mgLog.d("matchNode="+matchNode.tileIdx+","+matchNode.nodeIdx+" matchCost="+nodeMatchCost);
            }
        }
    }


}
