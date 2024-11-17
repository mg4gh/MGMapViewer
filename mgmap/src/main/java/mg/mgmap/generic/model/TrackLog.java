/*
 * Copyright 2017 - 2022 mg4gh
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

import mg.mgmap.generic.util.ObservableImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.Formatter;

import java.util.ArrayList;

/** A TrackLog consists of multiple TrackLogSegment objects, a total TrackLogStatistic over all segments and a track log name */
public class TrackLog extends ObservableImpl implements Comparable<TrackLog>{

    protected final ArrayList<TrackLogSegment> trackLogSegments = new ArrayList<>();
    protected TrackLogStatistic trackStatistic = new TrackLogStatistic(-1);
    protected String name = "";
    protected boolean available = true;
    protected boolean modified = false;
    protected TrackLog referencedTrackLog = null;
    protected boolean filterMatched = true;
    protected final Pref<Boolean> prefSelected = new Pref<>(Boolean.FALSE);
    protected String routingProfileId = null;

    public TrackLogStatistic getTrackStatistic() {
        return trackStatistic;
    }
    public void setTrackStatistic(TrackLogStatistic trackStatistic) {
        this.trackStatistic = trackStatistic;
    }
    public String getName() {
        return name;
    }
    public String getNameKey() {
        String nameKey = name;
        String sTStart = Formatter.SDF.format(trackStatistic.getTStart());
        nameKey = sTStart+"_"+nameKey;
        return nameKey;
    }
    public void setName(String name) {
        this.name = name;
        trackStatistic.logName = "st_"+name; // used only for debug purposes
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isModified(){
        return modified;
    }
    public void setModified(boolean modified) {
        if (this.modified != modified){
            this.modified = modified;
            changed(null);
        }
    }

    public boolean isFilterMatched() {
        return filterMatched;
    }
    public void setFilterMatched(boolean filterMatched) {
        this.filterMatched = filterMatched;
    }

    public ArrayList<TrackLogSegment> getTrackLogSegments(){
        return trackLogSegments;
    }

    public TrackLogSegment getTrackLogSegment(int idx) {
        return trackLogSegments.get(idx);
    }

    public TrackLog getReferencedTrackLog() {
        return referencedTrackLog;
    }

    public void setReferencedTrackLog(TrackLog referencedTrackLog) {
        this.referencedTrackLog = referencedTrackLog;
    }

    @Override
    public int compareTo(@NonNull TrackLog trackLog) {
        return getNameKey().compareTo(trackLog.getNameKey());
    }

    public int getNumberOfSegments(){
        return trackLogSegments.size();
    }

    public BBox getBBox(){
        BBox bBox = new BBox();

        for (TrackLogSegment segment : trackLogSegments){
            bBox.extend(segment.getBBox());
        }
        return bBox;
    }


    public boolean hasGainLoss(){
        return ((trackStatistic.getGain() != 0) || (trackStatistic.getLoss() != 0));
    }


    public TrackLogRefApproach getBestDistance(PointModel pm, double maxDistance){
        TrackLogRefApproach bestMatch = new TrackLogRefApproach(this, -1, maxDistance);
        PointModelUtil.getBestDistance(trackLogSegments,pm,bestMatch);
        return (bestMatch.getApproachPoint()==null)?null:bestMatch;

    }

    public TrackLogRefApproach getBestDistance(PointModel pm){
        return getBestDistance(pm, PointModelUtil.getCloseThreshold());
    }

    public TrackLogRefApproach getBestPoint(PointModel pm, double threshold){
        TrackLogRefApproach bestMatch = new TrackLogRefApproach(this, -1, threshold);
        PointModelUtil.getBestPoint(trackLogSegments,pm,bestMatch);
        return (bestMatch.getApproachPoint()==null)?null:bestMatch;
    }

    public TrackLogRefApproach getStartApproach(TrackLogRefApproach appStart){
        if (appStart == null || appStart.getTrackLog() != this){
            appStart = new TrackLogRefApproach(this, 0, 0);
            appStart.setApproachPoint(getTrackLogSegment(0).get(0));
            appStart.setEndPointIndex(0);
        }
        return appStart;
    }
    public TrackLogRefApproach getEndApproach(TrackLogRefApproach appEnd){
        if (appEnd == null || appEnd.getTrackLog() != this) {
            int segmentIdx = lastNoneEmptySegmentIdx();
            TrackLogSegment segment = getTrackLogSegment(segmentIdx);
            appEnd = new TrackLogRefApproach(this, segmentIdx, 0);
            appEnd.setApproachPoint(segment.getLastPoint());
            appEnd.setEndPointIndex(segment.size() - 1);
        }
        return appEnd;
    }

    public ArrayList<PointModel> getPointList(TrackLogRefApproach appStart, TrackLogRefApproach appEnd){
        appStart = getStartApproach(appStart);
        appEnd = getEndApproach(appEnd);
        ArrayList<PointModel> points = new ArrayList<>();
        if ((appEnd.getSegmentIdx() >= 0) && (appStart.compareTo(appEnd) < 0)){
            for (int segIdx=appStart.segmentIdx; segIdx<=appEnd.getSegmentIdx(); segIdx++){
                TrackLogSegment segment = getTrackLogSegment(segIdx);
                points.add( (segIdx==appStart.segmentIdx)?appStart.approachPoint:null );
                points.addAll(segment.points.subList( (segIdx==appStart.segmentIdx)?appStart.endPointIndex:0, (segIdx==appEnd.segmentIdx)?appEnd.endPointIndex:segment.size() ));
                points.add( (segIdx==appEnd.segmentIdx)?appEnd.approachPoint: null );
            }
            points.add( null );
        }
        return points;
    }

    public int lastNoneEmptySegmentIdx(){
        for (int i=getNumberOfSegments()-1; i>=0; i--){
            if (getTrackLogSegment(i).size() > 0) return i;
        }
        return -1;
    }


    public void changed(Object o){
        setChanged();
        notifyObservers(o);
    }

    public Pref<Boolean> getPrefSelected() {
        return prefSelected;
    }
    public boolean isSelected(){
        return prefSelected.getValue();
    }

    public String getRoutingProfileId() {
        return routingProfileId;
    }

    public void setRoutingProfileId(String routingProfileId) {
        if (!routingProfileId.equals(this.routingProfileId)){
            this.routingProfileId = routingProfileId;
            this.setModified(true);
        }
    }
}
