package mg.mapviewer.model;

import mg.mapviewer.util.MGPref;

/**
 * Creates new TrackLog objects based on existing GPX data, requires special handling with the timestamps.
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
            trackStatistic.tStart = timestamp;
        }
    }

    public void startSegment(long timestamp){
        currentSegment = new TrackLogSegment(this, getNumberOfSegments());
        trackLogSegments.add(currentSegment);
        if (timestamp != PointModel.NO_TIME){
            currentSegment.getStatistic().setTStart(timestamp);
        }
    }

    public void stopSegment(long timestamp){
        currentSegment = null;
        recalcTrackStatistic();
    }

    public void stopTrack(long timestamp){
        if ((trackStatistic.tStart == PointModel.NO_TIME) && (getNumberOfSegments() > 0)){
            trackStatistic.tStart = getTrackLogSegment(0).getStatistic().tStart;
        }
        long duration = 0;
        for (TrackLogSegment segment : getTrackLogSegments()){
            duration += segment.getStatistic().duration;
        }
        trackStatistic.duration = duration;
    }

    public void addPoint(PointModel lp){
        if (currentSegment == null){
            currentSegment = getTrackLogSegment(getNumberOfSegments()-1);
        }
        currentSegment.addPoint(lp);
        trackStatistic.updateWithPoint(lp);
    }

//    public void recalcStatistic(PointModel approachPoint, int idxSegmentStart, int idxPointStart) { // calculate remainings statistic from given approach point - continue with given segment and point index
//        for (int idxSegment = 0; idxSegment < getNumberOfSegments() ; idxSegment++){
//            if (idxSegment < idxSegmentStart) continue;
//            TrackLogSegment segment = getTrackLogSegment(idxSegment);
//            TrackLogStatistic segmentStatistic = segment.getStatistic();
//            segmentStatistic.reset();
//            if (idxSegment == idxSegmentStart) segmentStatistic.updateWithPoint(approachPoint);
//            for (int idxPoint=0; idxPoint < segment.size(); idxPoint++){
//                if ((idxSegment == idxSegmentStart) && (idxPoint<idxPointStart)) continue;
//                segmentStatistic.updateWithPoint(segment.get(idxPoint));
//            }
//        }
//        recalcTrackStatistic();
//    }


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

    public void setPrefModified(MGPref<Boolean> prefModified){
        this.prefModified = prefModified;
    }
}
