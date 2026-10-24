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

import mg.mgmap.generic.graph.Graph;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.MGLog;


/**
 * Realisation of a graph based on multiple tiles (GGraphTile) objects.
 */

public class DGraphMulti implements Graph {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final DGraphTileFactory dGraphTileFactory;
    private int useCnt = 0;

    public DGraphMulti(DGraphTileFactory dGraphTileFactory, ArrayList<DGraphTile> gGraphTiles){
        this.dGraphTileFactory = dGraphTileFactory;
        for (DGraphTile dGraphTile : gGraphTiles){
            use(dGraphTile);
        }
    }

    public int getTileCount(){
        return useCnt;
    }

//    /**
//     * Redefines getNodes() implementation of GGraph, which simply returns the ArrayList of its nodes.
//     * Here we get a new ArrayList Object with all nodes from each included GGraphTile
//     * @return all node ot the multi graph
//     */
//    @Override
//    public ArrayList<GNode> getGNodes() {
//        ArrayList<GNode> nodes = new ArrayList<>( super.getGNodes() );
//        for (GGraphTile gGraphTile : dGraphTileFactory.getCached()){
//            if (gGraphTile.used){
//                nodes.addAll( gGraphTile.getGNodes() );
//            }
//        }
//        return nodes;
//    }

    @Override
    public ArrayList<PointModel> getNeighbours(PointModel pointModel, ArrayList<PointModel> neighbourPoints) {
        return new ArrayList<>();
//        long mapSize = MercatorProjection.getMapSize(dGraphTileFactory.getZoomLevel(), dGraphTileFactory.getTileSize());
//
//        int tileX = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( pointModel.getLon() , mapSize) , dGraphTileFactory.getZoomLevel(), dGraphTileFactory.getTileSize());
//        int tileY = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( pointModel.getLat() , mapSize) , dGraphTileFactory.getZoomLevel(), dGraphTileFactory.getTileSize());
//        DGraphTile graph = dGraphTileFactory.getDGraphTile(tileX, tileY);
//        neighbourPoints = graph.getNeighbours(pointModel, new ArrayList<>());
//
//        GNode node = graph.getNode(pointModel.getLat(), pointModel.getLon());
//        if (node != null){
//            for (byte border = GNode.BORDER_NODE_SOUTH; border <= GNode.BORDER_NODE_WEST; border = (byte)(border<<1)){
//                if ((node.borderNode & border) != 0){
//                    GGraphTile neighbourGraph = graph.neighbourTiles[border];
//                    if (neighbourGraph != null) neighbourGraph.getNeighbours(pointModel, neighbourPoints);
//                }
//            }
//        }
//        return neighbourPoints;
    }


    // return true, if graph changed
    boolean preNodeRelax(DNode node, byte border){
        boolean changed = false;
        if (border != 0 /*&& (gGraphTileMap.size() < GGraphTileFactory.CACHE_LIMIT)*/){ // add lazy expansion of GGraphMulti
            changed |= checkGGraphTileNeighbour(node, border, Border.BORDER_NODE_WEST);
            changed |= checkGGraphTileNeighbour(node, border, Border.BORDER_NODE_NORTH);
            changed |= checkGGraphTileNeighbour(node, border, Border.BORDER_NODE_EAST);
            changed |= checkGGraphTileNeighbour(node, border, Border.BORDER_NODE_SOUTH);
        }
        return changed;
    }

    // Returns true, if graph is extended
    private boolean checkGGraphTileNeighbour(DNode node, byte nodeBorder, byte border){
        boolean bRes = false;
        if ( (nodeBorder & border) != 0 ) { // OK, this border is relevant for given node
            int tileX = DGraphTileFactory.getTileX(node.tileIdx);
            int tileY = DGraphTileFactory.getTileY(node.tileIdx);
            DGraphTile dGraphTile = dGraphTileFactory.getDGraphTile(tileX, tileY, false);
            assert(dGraphTile != null) : "Node tileIdx="+node.tileIdx+" "+tileX+" "+tileY+" "+useCnt+" "+nodeBorder;
            int tileXn = tileX + Border.deltaX(border);
            int tileYn = tileY + Border.deltaY(border);
            DGraphTile dGraphTileNeighbour = dGraphTileFactory.getDGraphTile(tileXn, tileYn, false);

            if (dGraphTileNeighbour == null){
                dGraphTileNeighbour = dGraphTileFactory.getDGraphTile(tileXn, tileYn, true);
                mgLog.d(String.format(Locale.ENGLISH, "tileX=%d tileY=%d use=%b border=%d tileXn=%d tileYn=%d use=%b",tileX,tileY,dGraphTile.used,border,tileXn,tileYn,dGraphTileNeighbour.used)+" "+dGraphTile.bBox +" "+dGraphTileNeighbour.bBox);
                bRes = true;
            }
            use(dGraphTileNeighbour);
        }

        return bRes;
    }

    private void use(DGraphTile dGraphTile){
        if (!dGraphTile.used){
            dGraphTile.resetNodeRefs();
            dGraphTile.used = true;
            useCnt++;
            mgLog.d("use tileX="+dGraphTile.tile.tileX+" tileY="+dGraphTile.tile.tileY+" size: "+useCnt);
        }
    }

    @Override
    public ArrayList<PointModel> segmentNodes(PointModel node1, PointModel node2) {
        return null;
    }

    @Override
    public String getRefDetails(PointModel node) {
        return "";
    }

    public void finalizeUsage(){
        mgLog.d("finalizeUsage A cntUsed="+cntUsed());
        for (DGraphTile dGraphTile : dGraphTileFactory.getCached()){
            dGraphTile.used = false;
        }
        useCnt = 0;
        mgLog.d("finalizeUsage E cntUsed="+cntUsed());
    }

    public int cntUsed(){
        return (int) dGraphTileFactory.getCached().stream().filter(f->f.used).count();
    }
}
