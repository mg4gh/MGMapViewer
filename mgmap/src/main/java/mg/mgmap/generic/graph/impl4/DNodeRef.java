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
package mg.mgmap.generic.graph.impl4;

import androidx.annotation.NonNull;

/**
 * Keeps references on nodes and node relationships including metadata. Only used für routing algorithm processing.
 */

public class DNodeRef extends MiDNodeRef implements Comparable<DNodeRef>{

    final double cost;
    final double heuristic;

    DNodeRef(int tileIdx, short neighbourIdx, double cost , double heuristic){
        super(tileIdx, neighbourIdx);
        this.cost = cost;
        this.heuristic = heuristic;
    }

    public int compareTo(@NonNull DNodeRef dNodeRef) {
        if (this == dNodeRef) return 0;
        if (getHeuristicCost() == dNodeRef.getHeuristicCost()){
            int res = Integer.compare(tileIdx, dNodeRef.tileIdx);
            if (res == 0) {
                res = Short.compare(neighbourIdx, dNodeRef.neighbourIdx);
                if (res == 0) res = Integer.compare(hashCode(), dNodeRef.hashCode());
            }
            return res;
        }
        if (getHeuristicCost() < dNodeRef.getHeuristicCost()){
            return -1;
        }
        return 1;
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


    @NonNull
    @Override
    public String toString() {
        return "DNodeRef{" +
                super.toString() +
                ", cost=" + cost +
                ", heuristic=" + heuristic +
                '}';
    }
}
