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

import java.util.Locale;

/** A specific kind of {@link mg.mgmap.generic.model.TrackLogRef} to reference an approach to a TrackLog.
 * Additionally it references the ApproachPoint, the distance to the approachPoint and
 * the index of the endpoint in the segment. */
public class TrackLogRefApproach extends TrackLogRef implements Comparable<TrackLogRefApproach>{

    PointModel approachPoint = null;
    double distance;
    int endPointIndex = 0; //endpoint of the approached segment

    public TrackLogRefApproach(TrackLog trackLog, int segmentIdx, double distance) {
        super(trackLog, segmentIdx);
        this.distance = distance;
    }

    public PointModel getApproachPoint() {
        return approachPoint;
    }

    public void setApproachPoint(PointModel approachPoint) {
        this.approachPoint = approachPoint;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public int getEndPointIndex() {
        return endPointIndex;
    }

    public void setEndPointIndex(int endPointIndex) {
        this.endPointIndex = endPointIndex;
    }


    @Override
    public int compareTo(TrackLogRefApproach o) {
        assert trackLog == o.trackLog; // can compare only approaches to the same track
        if (getSegmentIdx() > o.getSegmentIdx()){
            return  1;
        } else if (getSegmentIdx() == o.getSegmentIdx()){
            if (getEndPointIndex() > o.getEndPointIndex()){
                return  1;
            } else if (getEndPointIndex() == o.getEndPointIndex()){
                return 0;
            }
        }
        return -1;
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() +
                ", approachPoint=" + approachPoint +
                String.format(Locale.ENGLISH,", distance=%.2f", distance )+
                ", endPointIndex=" + endPointIndex ;
    }
}
