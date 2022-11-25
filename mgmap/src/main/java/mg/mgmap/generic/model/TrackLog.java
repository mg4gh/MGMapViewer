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

import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.Formatter;

import java.util.ArrayList;
import java.util.Observable;

/** A TrackLog consists of multiple TrackLogSegment objects, a total TrackLogStatistic over all segments and a track log name */
public class TrackLog extends Observable implements Comparable<TrackLog>{

    protected ArrayList<TrackLogSegment> trackLogSegments = new ArrayList<>();
    protected TrackLogStatistic trackStatistic = new TrackLogStatistic(-1);
    protected String name = "";
    protected boolean available = true;
    protected boolean modified = false;
    protected TrackLog referencedTrackLog = null;
    protected boolean filterMatched = true;
    protected Pref<Boolean> prefSelected = new Pref<>(Boolean.FALSE);

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



    public double getRemainingDistance(TrackLogRefApproach ref){
        TrackLogSegment segment = getTrackLogSegment(ref.segmentIdx);
        double remainingDistance = 0;
        PointModel p1 = ref.getApproachPoint();
        for (int idx = ref.getEndPointIndex(); idx < segment.size(); idx++){
            PointModel p2 = segment.get(idx);
            remainingDistance += PointModelUtil.distance(p1,p2);
            p1 = p2;
        }
        return remainingDistance;
    }

    public double getRemainingDistance(TrackLogRefApproach ref1, TrackLogRefApproach ref2){
        if (ref2.getEndPointIndex() < ref1.getEndPointIndex()){
            TrackLogRefApproach ref = ref1;
            ref1 = ref2;
            ref2 = ref;
        }

        TrackLogSegment segment = getTrackLogSegment(ref1.segmentIdx);
        double remainingDistance = 0;
        PointModel p1 = ref1.getApproachPoint();
        for (int idx = ref1.getEndPointIndex(); idx < ref2.getEndPointIndex(); idx++){
            PointModel p2 = segment.get(idx);
            remainingDistance += PointModelUtil.distance(p1,p2);
            p1 = p2;
        }
        remainingDistance += PointModelUtil.distance(p1,ref2.getApproachPoint());
        return remainingDistance;
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
}
