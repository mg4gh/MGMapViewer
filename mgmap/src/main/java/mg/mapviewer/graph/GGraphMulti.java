/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mapviewer.graph;

import mg.mapviewer.util.LaLo;
import mg.mapviewer.util.PointModelUtil;

import java.util.ArrayList;


/**
 * Realisation of a graph based on multiple tiles (GGraphTile) objects.
 */

public class GGraphMulti extends GGraph {


    private ArrayList<GGraphTile> gGraphTiles = new ArrayList<>();

    public GGraphMulti(ArrayList<GGraphTile> gGraphTiles){
        for (GGraphTile gGraphTile : gGraphTiles){
            connectGGraphTile(gGraphTile);
        }
    }

    private void connectGGraphTile(GGraphTile newGGraphTile){
        for (GGraphTile gGraphTile : gGraphTiles){
            if (gGraphTile.tile.equals(newGGraphTile.tile.getLeft())){
                connectHorizontal(gGraphTile, newGGraphTile);
            }
            if (gGraphTile.tile.equals(newGGraphTile.tile.getRight())){
                connectHorizontal(newGGraphTile, gGraphTile);
            }
            if (gGraphTile.tile.equals(newGGraphTile.tile.getAbove())){
                connectVertical(gGraphTile, newGGraphTile);
            }
            if (gGraphTile.tile.equals(newGGraphTile.tile.getBelow())){
                connectVertical(newGGraphTile, gGraphTile);
            }
        }
        gGraphTiles.add(newGGraphTile);
    }

    private void connectHorizontal(GGraphTile gGraphTile1, GGraphTile gGraphTile2){ // gGraphTile1 is left, gGraphTile2 is right
        double loConnect = gGraphTile1.tbBox.maxLongitude;
        if (loConnect != gGraphTile2.tbBox.minLongitude){
            throw new IllegalArgumentException("cannot connectHorizontal Tiles with BB " + gGraphTile1.tbBox +" and "+gGraphTile2.tbBox);
        }
        ArrayList<GNode> borderNodes1 = new ArrayList<>();
        for (GNode node : gGraphTile1.getNodes()){
            if (node.getLon() == loConnect){
                borderNodes1.add(node);
            }
        }
        ArrayList<GNode> borderNodes2 = new ArrayList<>();
        for (GNode node : gGraphTile2.getNodes()){
            if (node.getLon()  == loConnect){
                borderNodes2.add(node);
            }
        }
        for (GNode node1 : borderNodes1){
            for (GNode node2 : borderNodes2){
                if (Math.abs( node1.getLat() - node2.getLat() ) <= LaLo.md2d(CONNECT_THRESHOLD) ){ // difference in la value is less/equal 1 -> connect nodes
                    connect(node1, node2);
                }
            }
        }
    }

    private void connectVertical(GGraphTile gGraphTile1, GGraphTile gGraphTile2){  // gGraphTile1 is above, gGraphTile2 is below
        double laConnect = gGraphTile1.tbBox.minLatitude;
        if (laConnect != gGraphTile2.tbBox.maxLatitude){
            throw new IllegalArgumentException("cannot connectVertical Tiles with BB " + gGraphTile1.tbBox +" and "+gGraphTile2.tbBox);
        }
        ArrayList<GNode> borderNodes1 = new ArrayList<>();
        for (GNode node : gGraphTile1.getNodes()){
            if ( node.getLat() == laConnect){
                borderNodes1.add(node);
            }
        }
        ArrayList<GNode> borderNodes2 = new ArrayList<>();
        for (GNode node : gGraphTile2.getNodes()){
            if ( node.getLat() == laConnect){
                borderNodes2.add(node);
            }
        }
        for (GNode node1 : borderNodes1){
            for (GNode node2 : borderNodes2){
                if (Math.abs( node1.getLon() - node2.getLon() ) <= LaLo.md2d(CONNECT_THRESHOLD)){ // difference in la value is less/equal 1 -> connect nodes
                    connect(node1,node2);
                }
            }
        }
    }

    private void connect(GNode node1, GNode node2){
        double cost = PointModelUtil.distance(node1, node2);
        cost += 0.01;
        addNextNeighbour(node1,node1.getLastNeighbour(),new GNeighbour(node2,cost));
        addNextNeighbour(node2,node2.getLastNeighbour(),new GNeighbour(node1,cost));
    }



    @Override
    public ArrayList<GNode> getNodes() {
        ArrayList<GNode> nodes = new ArrayList<>( super.getNodes() );
        for (GGraphTile gGraphTile : gGraphTiles){
            nodes.addAll( gGraphTile.getNodes() );
        }
        return nodes;
    }

}
