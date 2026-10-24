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

import androidx.annotation.NonNull;

/**
 * Keeps references on nodes and node relationships including meta data. Only used für routing algorithm processing.
 */

public class DNodeRef implements Comparable<DNodeRef>{

    final int tileIdx;
    final short neighbourIdx;
    final double cost;
    final double heuristic;
    boolean settled = false;
    boolean reverse = false;

    DNodeRef(int tileIdx, short neighbourIdx, double cost , double heuristic){
        this.tileIdx = tileIdx;
        this.neighbourIdx = neighbourIdx;
        this.cost = cost;
        this.heuristic = heuristic;
    }

    public int compareTo(@NonNull DNodeRef gNodeRef) {
        if (this == gNodeRef) return 0;
        if (getHeuristicCost() == gNodeRef.getHeuristicCost()){
            int res = Integer.compare(tileIdx, gNodeRef.tileIdx);
            if (res == 0) {
                res = Short.compare(neighbourIdx, gNodeRef.neighbourIdx);
                if (res == 0) res = Integer.compare(hashCode(), gNodeRef.hashCode());
            }
            return res;
        }
        if (getHeuristicCost() < gNodeRef.getHeuristicCost()){
            return -1;
        }
        return 1;
    }

    short getNeighbourIdx(DGraphTile dGraphTile){
        short nIdx = neighbourIdx;
        if (reverse){
            nIdx = dGraphTile.getNbReverseIdx(nIdx);
        }
        return nIdx;
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
        return "DNodeRef{" +
                "tileIdx=" + tileIdx +
                ", neighbourIdx=" + neighbourIdx +
                ", cost=" + cost +
                ", heuristic=" + heuristic +
                ", settled=" + settled +
                ", reverse=" + reverse +
                '}';
    }
}
