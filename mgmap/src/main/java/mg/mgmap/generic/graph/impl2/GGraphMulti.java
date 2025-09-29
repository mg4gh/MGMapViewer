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

import org.mapsforge.core.util.MercatorProjection;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.MGLog;


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
    public ArrayList<GNode> getGNodes() {
        ArrayList<GNode> nodes = new ArrayList<>( super.getGNodes() );
        for (GGraphTile gGraphTile : gGraphTileFactory.getCached()){
            if (gGraphTile.used){
                nodes.addAll( gGraphTile.getGNodes() );
            }
        }
        return nodes;
    }

    @Override
    public ArrayList<PointModel> getNeighbours(PointModel pointModel, ArrayList<PointModel> neighbourPoints) {
        long mapSize = MercatorProjection.getMapSize(gGraphTileFactory.ZOOM_LEVEL, gGraphTileFactory.TILE_SIZE);

        int tileX = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( pointModel.getLon() , mapSize) , gGraphTileFactory.ZOOM_LEVEL, gGraphTileFactory.TILE_SIZE);
        int tileY = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( pointModel.getLat() , mapSize) , gGraphTileFactory.ZOOM_LEVEL, gGraphTileFactory.TILE_SIZE);
        GGraphTile graph = gGraphTileFactory.getGGraphTile(tileX, tileY);
        neighbourPoints = graph.getNeighbours(pointModel, new ArrayList<>());

        GNode node = graph.getNode(pointModel.getLat(), pointModel.getLon());
        if (node != null){
            for (byte border = GNode.BORDER_NODE_SOUTH; border <= GNode.BORDER_NODE_WEST; border = (byte)(border<<1)){
                if ((node.borderNode & border) != 0){
                    GGraphTile neighbourGraph = graph.neighbourTiles[border];
                    if (neighbourGraph != null) neighbourGraph.getNeighbours(pointModel, neighbourPoints);
                }
            }
        }
        return neighbourPoints;
    }


    // return true, if graph changed
    boolean preNodeRelax(GNode node){
        boolean changed = false;
        if ((node.borderNode != 0) /*&& (gGraphTileMap.size() < GGraphTileFactory.CACHE_LIMIT)*/){ // add lazy expansion of GGraphMulti
            changed |= checkGGraphTileNeighbour(node, GNode.BORDER_NODE_WEST);
            changed |= checkGGraphTileNeighbour(node, GNode.BORDER_NODE_NORTH);
            changed |= checkGGraphTileNeighbour(node, GNode.BORDER_NODE_EAST);
            changed |= checkGGraphTileNeighbour(node, GNode.BORDER_NODE_SOUTH);
        }
        return changed;
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
                gGraphTileNeighbour = gGraphTileFactory.getGGraphTile(tileXn, tileYn, true);
                mgLog.d(String.format(Locale.ENGLISH, "tileX=%d tileY=%d use=%b border=%d tileXn=%d tileYn=%d use=%b",tileX,tileY,gGraphTile.used,border,tileXn,tileYn,gGraphTileNeighbour.used)+" "+gGraphTile.bBox +" "+gGraphTileNeighbour.bBox);
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
