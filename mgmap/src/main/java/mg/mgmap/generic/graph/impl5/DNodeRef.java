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
package mg.mgmap.generic.graph.impl5;

import androidx.annotation.NonNull;

/**
 * Keeps references on nodes and node relationships including metadata. Only used für routing algorithm processing.
 */

public class DNodeRef implements Comparable<DNodeRef>{

    static final int FLAG_VALID = 0x01;
    static final int FLAG_REVERSE = 0x02;

    double cost;
    double heuristic;
    int tileIdx;
    short neighbourIdx;
    byte flags;

    DNodeRef(){
        tileIdx = 0;
        neighbourIdx = 0;
        flags = 0;
        cost = 0;
        heuristic = 0;
    }


    DNodeRef init(int tileIdx, short neighbourIdx, boolean reverse, double cost , double heuristic){
        this.tileIdx = tileIdx;
        this.neighbourIdx = neighbourIdx;
        this.flags = (byte)((reverse?FLAG_REVERSE:0) | FLAG_VALID);
        this.cost = cost;
        this.heuristic = heuristic;
        return this;
    }

    public int compareTo(@NonNull DNodeRef dNodeRef) {
        if (this == dNodeRef) return 0;
        if (getHeuristicCost() == dNodeRef.getHeuristicCost()){
            int res = Integer.compare(tileIdx, dNodeRef.tileIdx);
            if (res == 0) {
                res = Short.compare(neighbourIdx, dNodeRef.neighbourIdx);
                if (res == 0) res = Byte.compare(flags, dNodeRef.flags);
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

    boolean isValid() {
        return (flags & FLAG_VALID) == FLAG_VALID;
    }
    void setValid() {
        flags |= FLAG_VALID;
    }
    void setInvalid() {
        flags &= ~FLAG_VALID;
    }

    public boolean isReverse() {
        return (flags & FLAG_REVERSE) == FLAG_REVERSE;
    }

    public void setReverse(boolean reverse) {
        if (reverse){
            flags |= FLAG_REVERSE;
        } else {
            flags &= ~FLAG_REVERSE;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "DNodeRef{" +
                ", tileIdx=" + tileIdx +
                ", neighbourIdx=" + neighbourIdx +
                ", flags=" + flags +
                ", hcost=" + getHeuristicCost() +
                ", cost=" + cost +
                ", heuristic=" + heuristic +
                '}';
    }
}
