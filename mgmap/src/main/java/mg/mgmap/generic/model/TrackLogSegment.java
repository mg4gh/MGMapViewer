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

import java.util.ArrayList;
import java.util.Collection;


/** A TrackLogSegment contains additionally to the points in the MultiPointModel a few  more data.
 * These are
 * <ul>
 *     <li>the segment index</li>
 *     <li>metaData with bounding box infos on segment parts</li>
 *     <li>TrackLogStatistic information of this segment</li>
 * </ul>
 * */
public class TrackLogSegment extends MultiPointModelImpl{

    private final int segmentIdx;
    private final ArrayList<MetaData> metaDatas = new ArrayList<>(); //needed for loadFromBB ?? yes, but also for visualization of loaded tracks
    private TrackLogStatistic statistic;

    public TrackLogSegment(int idx, boolean iterateConcurrent){
        this(idx);
        this.iterateConcurrent = iterateConcurrent;
    }

    public TrackLogSegment(int idx){
        this.segmentIdx = idx;
        statistic = new TrackLogStatistic(idx);
    }

    public MultiPointModelImpl addPoint(int idx, PointModel pointModel){
        statistic.updateWithPoint(pointModel);
        return super.addPoint(idx, pointModel);
    }

    public PointModel getLastPoint(){
        if (!points.isEmpty()){
            return points.get(points.size()-1);
        }
        return null;
    }

    
    public ArrayList<MetaData> getMetaDatas() {
        return metaDatas;
    }

    public TrackLogStatistic getStatistic() {
        return statistic;
    }

    public void setStatistic(TrackLogStatistic statistic) {
        this.statistic = statistic;
    }

    public int getSegmentIdx() {
        return segmentIdx;
    }

    public void recalcStatistic(){
        statistic.reset();
        for (PointModel pm : this){
            statistic.updateWithPoint(pm);
        }
    }

    public void removeSegmentPointsFrom(Collection<PointModel> collection){
        collection.removeAll(points);
    }
}
