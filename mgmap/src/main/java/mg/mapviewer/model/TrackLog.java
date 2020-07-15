/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mapviewer.model;

import androidx.annotation.NonNull;


import mg.mapviewer.util.PointModelUtil;

import java.util.ArrayList;
import java.util.Observable;

/** A TrackLog consists of multiple TrackLogSegment objects, a total TrackLogStatistic over all segments and a track log name */
public class TrackLog extends Observable implements Comparable<TrackLog>{

    protected ArrayList<TrackLogSegment> trackLogSegments = new ArrayList<>();
    protected TrackLogStatistic trackStatistic = new TrackLogStatistic(-1);
    protected String name = "";
    protected boolean available = true;


    public void clear(){
        trackStatistic = null;
        name = "";
        trackLogSegments.clear();
    }

    public TrackLogStatistic getTrackStatistic() {
        return trackStatistic;
    }
    public void setTrackStatistic(TrackLogStatistic trackStatistic) {
        this.trackStatistic = trackStatistic;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public ArrayList<TrackLogSegment> getTrackLogSegments(){
        return trackLogSegments;
    }

    public TrackLogSegment getTrackLogSegment(int idx) {
        TrackLogSegment segment = trackLogSegments.get(idx);

        // this realize a kind of lazy loading of lalo values for available track logs
//        if (!segment.isAvailable()) {
//            MetaData.readLaLosOfSegment(this, segment);
//            segment.setAvailable(true);
//        }
        return segment;

    }



    @Override
    public int compareTo(@NonNull TrackLog trackLog) {
        return name.compareTo(trackLog.name);
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




    public TrackLogRefApproach getBestDistance(PointModel pm, double maxDistance){
        TrackLogRefApproach bestMatch = new TrackLogRefApproach(this, -1, maxDistance);
        PointModelUtil.getBestDistance(trackLogSegments,pm,bestMatch);

//        bestMatch.setDistance(maxDistance);
//
//        WriteablePointModel pmApproachCandidate = new WriteablePointModelImpl();//new TrackLogPoint();
//        WriteablePointModel pmApproach = new WriteablePointModelImpl();//new TrackLogPoint();
//        for (int segmentIdx = 0; segmentIdx< getNumberOfSegments(); segmentIdx++){
//            TrackLogSegment segment = getTrackLogSegment(segmentIdx);
//            for (int i = 1, j = 0; i < segment.size(); j = i++) {
//                if (PointModelUtil.findApproach(pm, segment.get(i), segment.get(j), pmApproachCandidate)){
//                    double distance = PointModelUtil.distance( pm, pmApproachCandidate);
//                    if (distance < bestMatch.getDistance()){
//                        bestMatch.setSegmentIdx( segmentIdx );
//                        if (bestMatch.getApproachPoint() == null) bestMatch.setApproachPoint(pmApproach);
//                        pmApproach.setLat(pmApproachCandidate.getLat());
//                        pmApproach.setLon(pmApproachCandidate.getLon());
//                        bestMatch.setDistance( distance );
//                        bestMatch.setEndPointIndex(i);
//                    }
//                }
//            }
//        }
        return (bestMatch.getApproachPoint()==null)?null:bestMatch;

    }

    public TrackLogRefApproach getBestDistance(PointModel pm){
        return getBestDistance(pm, PointModelUtil.getCloseThreshold());
    }

    public TrackLogRefApproach getBestPoint(PointModel pm, double threshold){
        TrackLogRefApproach bestMatch = new TrackLogRefApproach(this, -1, threshold);
        PointModelUtil.getBestPoint(trackLogSegments,pm,bestMatch);

//        bestMatch.setDistance( threshold );
//        for (int segmentIdx = 0; segmentIdx< getNumberOfSegments(); segmentIdx++){
//            TrackLogSegment segment = getTrackLogSegment(segmentIdx);
//            for (int i = 0; i < segment.size(); i++) {
//                double distance = PointModelUtil.distance(pm,segment.get(i));
//                if (distance < bestMatch.getDistance()){
//                    bestMatch.setSegmentIdx( segmentIdx );
//                    bestMatch.setApproachPoint(segment.get(i));
//                    bestMatch.setDistance( distance );
//                    bestMatch.setEndPointIndex(i);
//                }
//            }
//        }
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


//    private static void addSegmentHeightProfile(Double distance, SparseIntArray array, TrackLogSegment segment){
//        if (segment.size() > 0){
//            PointModel lastTlp = segment.get(0);
//            for (PointModel pm : segment){
//                distance += PointModelUtil.distance(pm, lastTlp);
//                array.put( (int)(distance+0), (int)(pm.getEleA()*1000));
//                lastTlp = pm;
//            }
//        }
//    }
//
//    public SparseIntArray getHeightProfile(int idx){
//        Double distance = 0d;
//        SparseIntArray array = new SparseIntArray();
//
//        if (idx >= 0){
//            addSegmentHeightProfile(distance,array,getTrackLogSegment(idx) );
//        } else {
//            for (int i = 0; i< getNumberOfSegments(); i++){
//                addSegmentHeightProfile(distance,array,getTrackLogSegment(i) );
//            }
//        }
//        return array;
//    }

    public void changed(Object o){
        setChanged();
        notifyObservers(o);
    }

    public ArrayList<MultiPointModel> asMPMList(){
        ArrayList<MultiPointModel> mpms = new ArrayList<>();
        for (int idx=0; idx<getNumberOfSegments(); idx++){
            mpms.add(getTrackLogSegment(idx));
        }
        return mpms;
    }
}
