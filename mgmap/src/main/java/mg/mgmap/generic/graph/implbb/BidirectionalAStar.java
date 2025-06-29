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
package mg.mgmap.generic.graph.implbb;

import static mg.mgmap.generic.graph.implbb.BNodes.FLAG_INVALID;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.RoutingProfile;
import mg.mgmap.generic.graph.ApproachModel;

import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.MultiPointModelImpl;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.basic.MemoryUtil;


public class BidirectionalAStar {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    BGraphMulti bGraphMulti;
    RoutingProfile routingProfile;
    BNodeRefs nodeRefs;

    protected PointModel source = null;
    protected PointModel target = null;
    protected int bestHeuristicRef = -1;
    protected int bestReverseHeuristicRef = -1;

    private long duration = 0;
    private int cntTotal = 0;
    private int cntRelaxed = 0;
    private int cntSettled = 0;
    private int resultPathLength = 0;
    private BGraphTile matchTile = null;
    private short matchNode = -1;

    public BidirectionalAStar(BGraphMulti bGraphMulti, RoutingProfile routingProfile) {
        this.bGraphMulti = bGraphMulti;
        this.routingProfile = routingProfile;

    }


    public MultiPointModel performAlgo(ApproachModel sourceApproachModel, ApproachModel targetApproachModel, double costLimit, AtomicInteger refreshRequired, ArrayList<PointModel> relaxedList){
        nodeRefs = new BNodeRefs(bGraphMulti);
        if (relaxedList != null) relaxedList.clear();

        ApproachModelImpl sourceApproachModelImpl = (sourceApproachModel instanceof ApproachModelImpl ami)?ami:null;
        ApproachModelImpl targetApproachModelImpl = (targetApproachModel instanceof ApproachModelImpl ami)?ami:null;
        if ((sourceApproachModelImpl == null) || (targetApproachModelImpl == null)) return null; // should never happen

        long tStart = System.currentTimeMillis();
        source = sourceApproachModel.getApproachNode();
        target = targetApproachModel.getApproachNode();

        createInitialNodeRef(sourceApproachModelImpl.node1, sourceApproachModelImpl.node2, source, false);
        createInitialNodeRef(sourceApproachModelImpl.node2, sourceApproachModelImpl.node1, source, false);
        createInitialNodeRef(targetApproachModelImpl.node1, targetApproachModelImpl.node2, target, true);
        createInitialNodeRef(sourceApproachModelImpl.node2, sourceApproachModelImpl.node1, target, true);

        int ref = nodeRefs.getFirstNode();
        int reverseRef;


        double bestMatchCost = Double.MAX_VALUE;
        while (true){
            if (nodeRefs.getHeuristicCost(ref) > costLimit/2){ // if costLimit reached
                mgLog.i("exit performAlgo 5 - cost limit reached: ref.getHeuristicCost()="+nodeRefs.getHeuristicCost(ref)+" costLimit/2="+(costLimit/2));
                break;
            }

            boolean refIsReverse = nodeRefs.isFlag(ref, BNodeRefs.FLAGS_REVERSED);
            BGraphTile bTile = bGraphMulti.getTile( nodeRefs.getNodeTileIdx( ref ));
            short node = nodeRefs.getNodeIdx(ref);

            reverseRef = bTile.getNodeRef(node, !refIsReverse);
            if ((reverseRef >= 0) && (nodeRefs.isFlag(reverseRef, BNodeRefs.FLAGS_SETTLED))){ // match found
                mgLog.i("exit performAlgo 1 - match found: refNode="+new BNode(bTile, node)+" ref="+getRefDetails(ref)+" revRef="+getRefDetails(reverseRef));
                break;
            }

            if (bTile.getNodeRef(node, refIsReverse) == ref){ // if there was already a better path to node found, then node.getNodeRef points to this -> then we ca skip this entry of the prioQueue

                if (refreshRequired.get() != 0) {
                    mgLog.i("exit performAlgo 3 - refresh required");
                    break;
                }
                if (preNodeRelax(bTile, node)) { // add lazy expansion of GGraphMulti
                    mgLog.i("exit performAlgo 4 - low memory");
                    break;
                }
                short neighbour = bTile.nodes.getNeighbour(node); // start relax all neighbours
                while ((neighbour = bTile.neighbours.getNextNeighbour(neighbour)) != 0) {
                    BGraphTile bNeighbourTile = bTile.neighbourTiles[bTile.neighbours.getTileSelector(neighbour)];
                    short neighbourNode = bTile.neighbours.getNeighbourPoint(neighbour);
                    short directedNeighbour = refIsReverse ? bTile.neighbours.getReverse(neighbour) : neighbour;

                    // do this block independent on the next if - data are used here - and later for heuristic
                    bNeighbourTile.nodes.bbNodes.position(neighbourNode * BNodes.NODE_SIZE + BNodes.POINT_LATITUDE_OFFSET);
                    double lat2 = LaLo.md2d(bNeighbourTile.nodes.bbNodes.getInt());
                    double lon2 = LaLo.md2d(bNeighbourTile.nodes.bbNodes.getInt());
                    float ele2 = bNeighbourTile.nodes.bbNodes.getFloat();

                    float costToNeighbour = bTile.neighbours.getCost(directedNeighbour);
                    if (costToNeighbour < 0) { // not yet calculated -> do it now
                        bTile.nodes.bbNodes.position(node * BNodes.NODE_SIZE + BNodes.POINT_LATITUDE_OFFSET);
                        double lat1 = LaLo.md2d(bTile.nodes.bbNodes.getInt());
                        double lon1 = LaLo.md2d(bTile.nodes.bbNodes.getInt());
                        float ele1 = bTile.nodes.bbNodes.getFloat();
                        short waIdx = bTile.neighbours.getWayAttributes(neighbour);
                        WayAttributs wayAttributs = (waIdx < 0) ? null : bTile.wayAttributes[waIdx];
                        double distance = PointModelUtil.distance(lat1, lon1, lat2, lon2);
                        float vertDistance = (ele2 - ele1) * (refIsReverse ? -1 : 1);
                        boolean primaryDirection = !bTile.neighbours.isReverse(neighbour) || (waIdx < 0);
                        costToNeighbour = (float) routingProfile.getCost(wayAttributs, distance, vertDistance, primaryDirection);
                        bTile.neighbours.setCost(directedNeighbour, costToNeighbour);
                    }
                    float currentCost = nodeRefs.getCost(ref) + costToNeighbour;
                    int neighbourRef = bNeighbourTile.getNodeRef(neighbourNode, refIsReverse);
                    // create new prioQueue entry, if there is currently none or if the current relaxted path has better cost
                    if ((neighbourRef < 0) || (currentCost < nodeRefs.getCost(neighbourRef))) {
                        float heuristic = heuristic(refIsReverse, lat2, lon2, ele2);
                        neighbourRef = nodeRefs.createNodeRef(bNeighbourTile.idxInMulti, neighbourNode, currentCost, heuristic, bTile.idxInMulti, node, neighbour, refIsReverse ? BNodeRefs.FLAGS_REVERSED : 0);
                        bNeighbourTile.setNodeRef(neighbourNode, refIsReverse, neighbourRef);
                        if (nodeRefs.getHeuristicCost(neighbourRef) < nodeRefs.getHeuristicCost(ref)) {
                            mgLog.e("Inconsistency detected ");
                        }
                        double matchCost = getMatchCost(bNeighbourTile, neighbourNode);
                        if (matchCost < bestMatchCost) {
                            matchTile = bNeighbourTile;
                            matchNode = neighbourNode;
                            bestMatchCost = matchCost;
                            mgLog.d("matchTile=" + matchTile + "matchNode=" + matchNode + " matchCost=" + matchCost);
                        }
                    }
                }
                nodeRefs.setFlag(ref, BNodeRefs.FLAGS_SETTLED, true);
                if (refIsReverse){
                    if (( bestReverseHeuristicRef == -1) || (nodeRefs.getHeuristic(ref) < nodeRefs.getHeuristic(bestReverseHeuristicRef))){
                        bestReverseHeuristicRef = ref;
                    }
                } else {
                    if (( bestHeuristicRef == -1) || (nodeRefs.getHeuristic(ref) < nodeRefs.getHeuristic(bestHeuristicRef))){
                        bestHeuristicRef = ref;
                    }
                }
            }
            ref = nodeRefs.getNextNode(ref);
            if (ref == -1) {
                mgLog.i("exit performAlgo 2 - ref=-1, no more nodes to handle, no path found");
                break; // no more nodes to handle - no path found
            }
        }
        duration = System.currentTimeMillis() - tStart;

        // generate statistics
        cntTotal = 0;
        cntRelaxed = 0;
        cntSettled = 0;
        resultPathLength = 0;

        for (short iTile=0; iTile<bGraphMulti.getNumTiles(); iTile++){
            BGraphTile bTile = bGraphMulti.getTile(iTile);
            for (short node=0; node < bTile.nodes.nodesUsed; node++){
                if (!bTile.nodes.isFlag(node, FLAG_INVALID)){
                    cntTotal++;
                    int nodeRef = bTile.getNodeRef(node, false);
                    int reverseNodeRef = bTile.getNodeRef(node, true);
                    if ((nodeRef >= 0) || (reverseNodeRef >= 0)){
                        cntRelaxed++;
                        if (((nodeRef >= 0) && nodeRefs.isFlag(nodeRef, BNodeRefs.FLAGS_SETTLED)) ||
                                ((reverseNodeRef >= 0) && nodeRefs.isFlag(reverseNodeRef, BNodeRefs.FLAGS_SETTLED))){
                            cntSettled++;
                        }
                    }
                }
            }
        }


        MultiPointModelImpl resultPath = new MultiPointModelImpl();
        if (matchTile != null){
            resultPath = getPath(matchTile.getNodeRef(matchNode, false));
            resultPath.addPoint(0, source);
            resultPath.removePoint(resultPath.size()-1); // remove matchNode
            resultPathLength = resultPath.size();
            for (PointModel pm : getPath(matchTile.getNodeRef(matchNode, true))){
                resultPath.addPoint(resultPathLength,pm);
            }
            resultPath.addPoint(target);
        }
        resultPathLength = resultPath.size();
        return resultPath;
    }

    float heuristic(boolean reverse, PointModel node){
        double h;
        double hf = routingProfile.heuristic(node, target);
        double hr = routingProfile.heuristic(source, node);
        if (reverse){
            h = (hr - hf) / 2;
        } else {
            h = (hf - hr) / 2;
        }
        return (float) h;
    }
    float heuristic(boolean reverse, double lat, double lon, float ele) {
        double h;
        double df = PointModelUtil.distance(lat, lon, target.getLat(), target.getLon());
        float vdf = target.getEle() - ele;
        double hf = routingProfile.heuristic(df, vdf);

        double dr = PointModelUtil.distance(source.getLat(), source.getLon(), lat, lon);
        float vdr = ele - source.getEle();
        double hr = routingProfile.heuristic(dr, vdr);
        if (reverse){
            h = (hr - hf) / 2;
        } else {
            h = (hf - hr) / 2;
        }
        return (float) h;
    }


    void createInitialNodeRef(BNode node1, BNode node2, PointModel approachNode, boolean reverse){
        BGraphTile tile = node1.bGraphTile;
        short neighbour12 = tile.getNeighbour(node1.nodeIdx, node2.nodeIdx);
        short waIdx = tile.neighbours.getWayAttributes(neighbour12);
        WayAttributs wayAttributs = (waIdx < 0)?null:tile.wayAttributes[waIdx];
        float cost = (float)routingProfile.getCost(wayAttributs, node1, approachNode, tile.neighbours.isReverse(neighbour12) || (waIdx < 0));
        float heuristic = heuristic(reverse, node1);
        nodeRefs.createNodeRef(tile.idxInMulti, node1.nodeIdx, cost, heuristic, (short)-1, (short)-1, neighbour12, (byte)0);
    }

    protected boolean preNodeRelax(BGraphTile bGraphTile, short node){
        if ( bGraphMulti.preNodeRelax(bGraphTile, node)){
            if (MemoryUtil.checkLowMemory(2)){
                mgLog.w("abort routing due low memory");
                return true;
            }
        }
        return false;
    }

    public MultiPointModelImpl getPath(int ref){
        MultiPointModelImpl path = new MultiPointModelImpl();
        if (ref != -1){
            while (true){
                BGraphTile bTile = bGraphMulti.getTile( nodeRefs.getNodeTileIdx( ref ));
                short node = nodeRefs.getNodeIdx(ref);
                path.addPoint(0, new BNode(bTile, node));
                boolean refIsReverse = nodeRefs.isFlag(ref, BNodeRefs.FLAGS_REVERSED);

                short prevNodeTileIdx = nodeRefs.getPrevNodeTileIdx(ref);
                if (prevNodeTileIdx < 0) break;
                BGraphTile bPrevTile = bGraphMulti.getTile( prevNodeTileIdx);
                short prevNodeIdx = nodeRefs.getPrevNodeIdx(ref);
                ref = bPrevTile.getNodeRef(prevNodeIdx, refIsReverse);
            }
        }
        return path;
    }



    public ArrayList<MultiPointModel> getBestPath(){
        ArrayList<MultiPointModel> paths = new ArrayList<>();
        MultiPointModelImpl bestPath = getPath(bestHeuristicRef);
        bestPath.addPoint(0, source);
        paths.add(bestPath);
        bestPath = getPath(bestReverseHeuristicRef);
        bestPath.addPoint(0, target);
        paths.add(bestPath);
        return paths;
    }

    public String getResult(){
        String res = "\n";
        res += String.format(Locale.GERMAN, "%s  tiles: %d, graphNodes: %d, relaxed: %d, settled: %d, duration %dms\n",this.getClass().getSimpleName(),bGraphMulti.getNumTiles(), cntTotal,cntRelaxed, cntSettled, duration);
        if (matchTile == null){
            res += "no path found";
        } else {
            res += String.format(Locale.GERMAN,"target path found - hop count=%d cost=%.2f",resultPathLength,getMatchCost(matchTile, matchNode));
        }
        return res;
    }

    private double getMatchCost(BGraphTile bGraphTile, short node){
        int nodeRef1 = bGraphTile.getNodeRef(node, false);
        int nodeRef2 = bGraphTile.getNodeRef(node, true);
        if ((node < 0) || (nodeRef1 < 0) || (nodeRef2 < 0)) return Double.MAX_VALUE;
        return nodeRefs.getCost(nodeRef1) + nodeRefs.getCost(nodeRef2);
    }

    private String getRefDetails(int ref){
        if (ref < 0) return "";
        return String.format(Locale.ENGLISH, " %s settled=%b cost=%.2f heuristic=%.2f hcost=%.2f",
                nodeRefs.isFlag(ref, BNodeRefs.FLAGS_REVERSED)?"rv":"fw",
                nodeRefs.isFlag(ref, BNodeRefs.FLAGS_SETTLED),
                nodeRefs.getCost(ref),
                nodeRefs.getHeuristic(ref),
                nodeRefs.getHeuristicCost(ref));
    }

}
