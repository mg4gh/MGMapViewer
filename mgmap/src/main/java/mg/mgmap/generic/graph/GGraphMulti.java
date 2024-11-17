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

import mg.mgmap.generic.util.basic.MGLog;
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
    private int useCnt = 0;

    public GGraphMulti(GGraphTileFactory gGraphTileFactory, ArrayList<GGraphTile> gGraphTiles){
        this.gGraphTileFactory = gGraphTileFactory;
        for (GGraphTile gGraphTile : gGraphTiles){
            use(gGraphTile);
        }
    }

    public int getTileCount(){
        return useCnt;
    }

    /**
     * Redefines getNodes() implementation of GGraph, which simply returns the ArrayList of its nodes.
     * Here we get a new ArrayList Object with all nodes from each included GGraphTile
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
                bRes = true;
            }
            use(gGraphTileNeighbour);
        }

        return bRes;
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
