package mg.mapviewer.model;

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

    }

    public void startSegment(long timestamp){
        currentSegment = new TrackLogSegment(this, getNumberOfSegments());
        trackLogSegments.add(currentSegment);
    }

    public void stopSegment(long timestamp){
        currentSegment = null;
        recalcTrackStatistic();
    }

    public void stopTrack(long timestamp){
        if (getNumberOfSegments() > 0){
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
