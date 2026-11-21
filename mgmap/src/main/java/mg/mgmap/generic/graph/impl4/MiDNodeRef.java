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
 * Minimal version of DNodeRef - will be used after settle; replaces DNodeRef to save space of double fields cost and heuristic
 */

public class MiDNodeRef {

    static final int FLAG_SETTLED = 0x01;
    static final int FLAG_REVERSE = 0x02;
    static final int FLAG_START = 0x04;


    final int tileIdx;
    final short neighbourIdx;
    byte flags = 0;

    MiDNodeRef(int tileIdx, short neighbourIdx){
        this.tileIdx = tileIdx;
        this.neighbourIdx = neighbourIdx;
    }

    MiDNodeRef(DNodeRef ref){
        this.tileIdx = ref.tileIdx;
        this.neighbourIdx = ref.neighbourIdx;
        this.flags = ref.flags;
    }

    public boolean isSettled() {
        return (flags & FLAG_SETTLED) == FLAG_SETTLED;
    }

    public void setSettled(boolean settled) {
        if (settled){
            flags |= FLAG_SETTLED;
        } else {
            flags &= ~FLAG_SETTLED;
        }
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

    public boolean isStart() {
        return (flags & FLAG_START) == FLAG_START;
    }

    public void setStart(boolean start) {
        if (start){
            flags |= FLAG_START;
        } else {
            flags &= ~FLAG_START;
        }
    }


    @NonNull
    @Override
    public String toString() {
        return "MiDNodeRef{" +
                "tileIdx=" + tileIdx +"("+(tileIdx>>16)+","+(tileIdx&0xFFFF)+")"+
                ", neighbourIdx=" + neighbourIdx +
                ", settled=" + isSettled() +
                ", reverse=" + isReverse() +
                ", start=" + isStart() +
                '}';
    }
}
