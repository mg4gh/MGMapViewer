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

import mg.mgmap.generic.model.WriteablePointModel;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.basic.MemoryUtil;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;


/**
 * Realisation of a graph based on multiple tiles (GGraphTile) objects.
 */

public class GGraphMulti extends GGraph {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final GGraphTileFactory gGraphTileFactory;
//    ArrayList<GOverlayNeighbour> overlayNeighbours = new ArrayList<>(); // used for neighbour tile connections and for approaches
    private int useCnt = 0;

    public GGraphMulti(GGraphTileFactory gGraphTileFactory, ArrayList<GGraphTile> gGraphTiles){
        this.gGraphTileFactory = gGraphTileFactory;
        for (GGraphTile gGraphTile : gGraphTiles){
            use(gGraphTile);
            connectGGraphTile(gGraphTile);
        }
    }

    public int getTileCount(){
        return useCnt;
    }

    /**
     * Redefines getNodes() implementation og GGraph, which simply returns the ArrayList of its nodes.
     * Here we get a new ArrayList Object with all nodes from each included GGraphTile plus additionally the
     * nodes that were created due to connecting neighbour GGraphTile instances, plus the additional
     * overlay nodes from approaches for start and end point of a routing access.
     * @return all node ot the multi graph
     */
    @Override
    public ArrayList<GNode> getNodes() {
        ArrayList<GNode> nodes = new ArrayList<>( super.getNodes() );
        for (GGraphTile gGraphTile : gGraphTileFactory.getCached()){
            if (gGraphTile.used){
                nodes.addAll( gGraphTile.getNodes() );
            }
        }
        return nodes;
    }

    public GNeighbour getNextNeighbour(GNode node, GNeighbour neighbour){
        return neighbour.getNextNeighbour();
    }


    // return true, if routing should be aborted due to low memory
    boolean preNodeRelax(GNode node){
        if ((node.borderNode != 0) /*&& (gGraphTileMap.size() < GGraphTileFactory.CACHE_LIMIT)*/){ // add lazy expansion of GGraphMulti
            boolean changed = checkGGraphTileNeighbour(node,GNode.BORDER_NODE_WEST);
            changed |= checkGGraphTileNeighbour(node,GNode.BORDER_NODE_NORTH);
            changed |= checkGGraphTileNeighbour(node,GNode.BORDER_NODE_EAST);
            changed |= checkGGraphTileNeighbour(node,GNode.BORDER_NODE_SOUTH);
            if (changed && MemoryUtil.checkLowMemory(GGraphTileFactory.LOW_MEMORY_THRESHOLD)){
                mgLog.w("abort routing due low memory");
                return true;
            }
        }
        return false;
    }

    // Returns true, if graph is extended
    private boolean checkGGraphTileNeighbour(GNode node, byte border){
        boolean bRes = false;
        if ( (node.borderNode & border) != 0 ) {
            int tileX = node.tileIdx>>16;
            int tileY = node.tileIdx & 0xFFFF;
            GGraphTile gGraphTile = gGraphTileFactory.getGGraphTile(tileX, tileY, false);
            assert(gGraphTile != null) : "Node tileIdx="+node.tileIdx+" "+(node.tileIdx>>16)+" "+(node.tileIdx & 0xFFFF)+" "+useCnt+" "+node.borderNode;
            int tileXn = gGraphTile.getTileX() + GNode.deltaX(border);
            int tileYn = gGraphTile.getTileY() + GNode.deltaY(border);
            GGraphTile gGraphTileNeighbour = gGraphTileFactory.getGGraphTile(tileXn, tileYn, false);

            if (gGraphTileNeighbour == null){
                mgLog.d(String.format(Locale.ENGLISH, "border=%d tileX=%d tileY=%d",border,tileXn,tileYn));
                gGraphTileNeighbour = gGraphTileFactory.getGGraphTile(tileXn, tileYn, true);
                connectGGraphTile(gGraphTileNeighbour);
                bRes = true;
            } else if (!gGraphTileNeighbour.used){
                connectGGraphTile(gGraphTileNeighbour);
            }
            use(gGraphTileNeighbour);
        }

        return bRes;
    }

    private void connectGGraphTile(GGraphTile newGGraphTile){
        connectTiles(gGraphTileFactory.getGGraphTile(newGGraphTile.getTileX()-1,newGGraphTile.getTileY(), false), newGGraphTile, true);
        connectTiles(newGGraphTile, gGraphTileFactory.getGGraphTile(newGGraphTile.getTileX()+1,newGGraphTile.getTileY(),false), true);
        connectTiles(gGraphTileFactory.getGGraphTile(newGGraphTile.getTileX(),newGGraphTile.getTileY()-1, false), newGGraphTile, false);
        connectTiles(newGGraphTile, gGraphTileFactory.getGGraphTile(newGGraphTile.getTileX(),newGGraphTile.getTileY()+1, false), false);
    }

    private void connectTiles(GGraphTile gGraphTile1, GGraphTile gGraphTile2,boolean horizontal){ // horizontal true: gGraphTile1 is left, gGraphTile2 is right - false: gGraphTile1 is above, gGraphTile2 is below
        if ((gGraphTile1 == null) || (gGraphTile2 == null)) return; // cannot connect yet
        if ((gGraphTile1.neighbourTiles[horizontal?GNode.BORDER_NODE_EAST:GNode.BORDER_NODE_SOUTH] == gGraphTile2) &&
                (gGraphTile2.neighbourTiles[horizontal?GNode.BORDER_NODE_WEST:GNode.BORDER_NODE_NORTH] == gGraphTile1)){ // tiles already connected
            return;
        }
        if ((gGraphTile1.neighbourTiles[horizontal?GNode.BORDER_NODE_EAST:GNode.BORDER_NODE_SOUTH] != null) ||
                (gGraphTile2.neighbourTiles[horizontal?GNode.BORDER_NODE_WEST:GNode.BORDER_NODE_NORTH] != null)){ // inconsistency detected!!!!
            mgLog.e("connectTiles failed"+gGraphTile1.getTileX()+","+gGraphTile1.getTileY()+" "+gGraphTile2.getTileX()+","+gGraphTile2.getTileY()+" "+horizontal);
            mgLog.e("found="+gGraphTile1.neighbourTiles[horizontal?GNode.BORDER_NODE_EAST:GNode.BORDER_NODE_SOUTH]+" expected="+gGraphTile2);
            mgLog.e("found="+gGraphTile2.neighbourTiles[horizontal?GNode.BORDER_NODE_WEST:GNode.BORDER_NODE_NORTH]+" expected="+gGraphTile1);
            throw new IllegalArgumentException("inconsistent neighbour tiles - check logfile for more details.");
        }
        double connectAt = (horizontal)?gGraphTile1.tbBox.maxLongitude:gGraphTile1.tbBox.minLatitude;
        if (horizontal? (connectAt!=gGraphTile2.tbBox.minLongitude):(connectAt!=gGraphTile2.tbBox.maxLatitude)){
            throw new IllegalArgumentException("cannot connectHorizontal Tiles with BB " + gGraphTile1.tbBox +" and "+gGraphTile2.tbBox);
        }
        ArrayList<GNode> borderNodes1 = new ArrayList<>();
        for (GNode node : gGraphTile1.getNodes()){
            if ((horizontal)?(node.getLon() == connectAt):(node.getLat() == connectAt)){
                borderNodes1.add(node);
            }
        }
        ArrayList<GNode> borderNodes2 = new ArrayList<>();
        for (GNode node : gGraphTile2.getNodes()){
            if ((horizontal)?(node.getLon() == connectAt):(node.getLat() == connectAt)){
                borderNodes2.add(node);
            }
        }
        final ArrayList<GNode> remainingNodes1 = new ArrayList<>(borderNodes1);
        final ArrayList<GNode> remainingNodes2 = new ArrayList<>(borderNodes2);
        double threshold = (horizontal)?PointModelUtil.latitudeDistance(CONNECT_THRESHOLD_METER):PointModelUtil.longitudeDistance(CONNECT_THRESHOLD_METER, connectAt);
        for (GNode node1 : borderNodes1){
            for (GNode node2 : borderNodes2){
                if ( (horizontal?Math.abs( node1.getLat() - node2.getLat() ):Math.abs( node1.getLon() - node2.getLon() )) <= threshold){ //distance less than 0.5m -> connect nodes
                    connect(node1, node2);
                    remainingNodes1.remove(node1);
                    remainingNodes2.remove(node2);
                }
            }
        }
        if ((remainingNodes1.size() > 0) && ((remainingNodes2.size() > 0))){
            ArrayList<GNode> stillRemainingNodes1 = new ArrayList<>(remainingNodes1);
            ArrayList<GNode> stillRemainingNodes2 = new ArrayList<>(remainingNodes2);
            mgLog.v(remainingNodes1::toString);
            mgLog.v(remainingNodes2::toString);
            for (GNode node1 : remainingNodes1){
                for (GNode node2 : remainingNodes2){
                    if ((node1.countNeighbours() == 1) && (node2.countNeighbours() == 1) && (PointModelUtil.distance(node1,node2)<CONNECT_THRESHOLD_METER*20)){
                        GNode node1Neighbour = node1.getNeighbour().getNextNeighbour().getNeighbourNode();
                        GNode node2Neighbour = node2.getNeighbour().getNextNeighbour().getNeighbourNode();
                        WriteablePointModel approachPoint = new WriteablePointModelImpl();
                        if (!PointModelUtil.findApproach(node1,node1Neighbour,node2Neighbour,approachPoint,0)) continue; // approach not found try next points
                        if (PointModelUtil.distance(approachPoint,node1) > CONNECT_THRESHOLD_METER) continue;
                        if (!PointModelUtil.findApproach(node2,node1Neighbour,node2Neighbour,approachPoint,0)) continue; // approach not found try next points
                        if (PointModelUtil.distance(approachPoint,node2) > CONNECT_THRESHOLD_METER) continue;
                        mgLog.d(()->"OK, connect: node1 " + node1 + " node1neighbour " + node1Neighbour + " node2 " + node2 + " node2neighbour " + node2Neighbour);
                        connect(node1, node2);
                        stillRemainingNodes1.remove(node1);
                        stillRemainingNodes2.remove(node2);
                    }
                }
            }
            remainingNodes1.clear();
            remainingNodes1.addAll(stillRemainingNodes1);
            remainingNodes2.clear();
            remainingNodes2.addAll(stillRemainingNodes2);
        }
        if ((remainingNodes1.size() > 0) || ((remainingNodes2.size() > 0))){
            mgLog.d(()->"remainings1 " + remainingNodes1);
            mgLog.d(()->"remainings2 " + remainingNodes2);
        }
        gGraphTile1.neighbourTiles[horizontal?GNode.BORDER_NODE_EAST:GNode.BORDER_NODE_SOUTH] = gGraphTile2;
        gGraphTile2.neighbourTiles[horizontal?GNode.BORDER_NODE_WEST:GNode.BORDER_NODE_NORTH] = gGraphTile1;
    }

    public void connect(GNode node1, GNode node2){
        GNeighbour n12 = new GNeighbour(node2,null);
        GNeighbour n21 = new GNeighbour(node1,null);
        n12.setReverse(n21);
        n21.setReverse(n12);
        node1.addNeighbour(n12);
        node2.addNeighbour(n21);
    }

    private void use(GGraphTile gGraphTile){
        if (!gGraphTile.used){
            gGraphTile.resetNodeRefs();
            gGraphTile.used = true;
            useCnt++;
            mgLog.d("use tileX="+gGraphTile.getTileX()+" tileY="+gGraphTile.getTileY()+" size: "+useCnt);
        }
    }

    public void finalizeUsage(){
        mgLog.d("finalizeUsage A cntUsed="+cntUsed());
        for (GGraphTile gGraphTile : gGraphTileFactory.getCached()){
            gGraphTile.used = false;
        }
        useCnt = 0;
        mgLog.d("finalizeUsage E cntUsed="+cntUsed());
    }

    public int cntUsed(){
        return (int)gGraphTileFactory.getCached().stream().filter(f->f.used).count();
    }
}
