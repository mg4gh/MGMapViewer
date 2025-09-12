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
package mg.mgmap.generic.graph.impl;

import androidx.annotation.NonNull;

/**
 * Keeps references on nodes and node relationships including meta data. Only used für routing algorithm processing.
 */

public class GNodeRef implements Comparable<GNodeRef>{

    private final GNode node;
    private final GNode predecessor;
    private final GNeighbour neighbour;
    private final double cost;
    private final double heuristic;
    private boolean settled = false;
    private boolean reverse = false;

    GNodeRef(GNode node, double cost, GNode predecessor, GNeighbour neighbour, double heuristic){
        this.node = node;
        this.cost = cost;
        this.predecessor  = predecessor;
        this.neighbour = neighbour;
        this.heuristic = heuristic;
    }

    public int compareTo(@NonNull GNodeRef gNodeRef) {
        if (this == gNodeRef) return 0;
        if (getHeuristicCost() == gNodeRef.getHeuristicCost()){
            return Integer.compare(hashCode(), gNodeRef.hashCode());
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
        return getCost()+getHeuristic();
    }

    public boolean isSetteled() {
        return settled;
    }

    public void setSettled(boolean settled) {
        this.settled = settled;
    }

    public boolean isReverse() {
        return reverse;
    }

    public void setReverse(boolean reverse) {
        this.reverse = reverse;
    }

    @NonNull
    @Override
    public String toString() {
        return "GNodeRef{" +
                "node=" + node +
                ", predecessor=" + predecessor +
                ", neighbour=" + neighbour +
                ", cost=" + cost +
                ", heuristic=" + heuristic +
                ", settled=" + settled +
                ", reverse=" + reverse +
                '}';
    }
}
