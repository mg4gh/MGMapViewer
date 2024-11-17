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
package mg.mgmap.generic.model;

import androidx.annotation.NonNull;

/**
 * Reference on a TrackLog (and a particular Segment of it).
 */
public class TrackLogRef {

    TrackLog trackLog = null;
    int segmentIdx = -1;


    public TrackLogRef(){}

    public TrackLogRef(TrackLog trackLog, int segmentIdx) {
        this.trackLog = trackLog;
        this.segmentIdx = segmentIdx;
    }

    public TrackLog getTrackLog() {
        return trackLog;
    }

    public void setTrackLog(TrackLog trackLog) {
        this.trackLog = trackLog;
    }

    public int getSegmentIdx() {
        return segmentIdx;
    }

    public void setSegmentIdx(int segmentIdx) {
        this.segmentIdx = segmentIdx;
    }


    public TrackLogSegment getSegment(){
        return trackLog.getTrackLogSegment(segmentIdx);
    }

    @NonNull
    @Override
    public String toString() {
        return
                "trackLog.name=" + trackLog.name +
                ", segmentIdx=" + segmentIdx ;
    }
}
