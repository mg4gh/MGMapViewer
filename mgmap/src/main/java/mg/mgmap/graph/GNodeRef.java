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
package mg.mgmap.graph;

import androidx.annotation.NonNull;

import mg.mgmap.util.PointModelUtil;

/**
 * Keeps references on nodes and node relationships including meta data. Only used für routing algorithm processing.
 */

public class GNodeRef implements Comparable<GNodeRef>{

    private GNode node;
    private GNode predecessor;
    private GNeighbour neighbour;
    private double cost = 0;
    private double heuristic = 0;
    private boolean settled = false;

    GNodeRef(GNode node, double cost, GNode predecessor, GNeighbour neighbour, double heuristic){
        this.node = node;
        this.cost = cost;
        this.predecessor  = predecessor;
        this.neighbour = neighbour;
        this.heuristic = heuristic;
    }


    public int compareTo(@NonNull GNodeRef gNodeRef) {
        if (getHeuristicCost() == gNodeRef.getHeuristicCost()){
            return PointModelUtil.compareTo(node, gNodeRef.getNode());
        }
        if (getHeuristicCost() < gNodeRef.getHeuristicCost()){
            return -1;
        }
        return 1;
    }


    public GNode getNode() {
        return node;
    }


    public GNode getPredecessor() {
        return predecessor;
    }


    public GNeighbour getNeighbour() {
        return neighbour;
    }


    public double getCost() {
        return cost;
    }


    public double getHeuristic() {
        return heuristic;
    }


    public double getHeuristicCost() {
        return cost+heuristic;
    }


    public boolean isSetteled() {
        return settled;
    }


    public void setSettled(boolean settled) {
        this.settled = settled;
    }
}
