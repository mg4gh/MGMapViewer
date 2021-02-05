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
package mg.mgmap.model;

/**
 * Creates new TrackLog object, requires special handling with the timestamps.
 * Created by Martin on 16.10.2017.
 */
public class WriteableTrackLog extends TrackLog {

    protected TrackLogSegment currentSegment = null;

    public WriteableTrackLog(String name){
        this.name = name;
    }
    public WriteableTrackLog(){}


    public void startTrack(long timestamp){
        if (timestamp != PointModel.NO_TIME){
            trackStatistic.setTStart(timestamp);
        }
    }

    public void startSegment(long timestamp){
        currentSegment = new TrackLogSegment(getNumberOfSegments());
        trackLogSegments.add(currentSegment);
        currentSegment.getStatistic().setTStart(timestamp);
        if (trackStatistic.getTStart() == PointModel.NO_TIME){
            trackStatistic.setTStart(timestamp);
        }
    }

    public void stopSegment(long timestamp){
        recalcTrackStatistic();
        currentSegment = null;
    }

    public void stopTrack(long timestamp){
    }

    public void addPoint(PointModel lp){
        if (currentSegment == null){
            currentSegment = getTrackLogSegment(getNumberOfSegments()-1);
        }
        currentSegment.addPoint(lp);
        trackStatistic.updateWithPoint(lp);
    }

    // calculate remainings statistic from given approach point - continue with given segment and point index
    // but don't change the TrackLog statistic itself, rather calculate on a separate TrackLogStatistic Object
    public void remainStatistic(TrackLogStatistic rStat, PointModel approachPoint, int idxSegmentStart, int idxPointStart) {
        rStat.reset();
        TrackLogSegment segment = getTrackLogSegment(idxSegmentStart);
        if (segment == null) return;
        rStat.updateWithPoint(approachPoint);
        for (int idxPoint=idxPointStart; idxPoint < segment.size(); idxPoint++){ // update with remaining points in this segment
            rStat.updateWithPoint(segment.get(idxPoint));
        }
        for (int idxSegment=idxSegmentStart+1;idxSegment<getNumberOfSegments();idxSegment++){ // update with remaining segments
            segment = getTrackLogSegment(idxSegment);
            rStat.updateWithStatistics(segment.getStatistic());
        }
    }

    public void recalcStatistic(){
        if (currentSegment == null){
            currentSegment = getTrackLogSegment(getNumberOfSegments()-1);
        }
        currentSegment.recalcStatistic();
        recalcTrackStatistic();
    }

    private void recalcTrackStatistic(){
        trackStatistic.reset();
        for (TrackLogSegment segment : getTrackLogSegments()){
            trackStatistic.updateWithStatistics(segment.getStatistic());
        }
    }
}
