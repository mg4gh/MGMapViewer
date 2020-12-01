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

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;

import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.PointModelUtil;


/**
 * Statistic of a TrackLog or of a Segment of a TrackLog.
 */

public class TrackLogStatistic {

    private SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy_HH:mm:ss",Locale.GERMANY);
    private boolean frozen = false; //used to prevent recalc Statistic after MetaData.load ... and later lazy loading of Points

    public int segmentIdx; // -1 means all segments
    long tStart;
    public long duration;


    double totalLength = 0;
    float gain = 0;
    float loss = 0;
    float minEle = -PointModel.NO_ELE;
    float maxEle = PointModel.NO_ELE;
    int numPoints = 0;

    public int getSegmentIdx() {
        return segmentIdx;
    }
    public long getTStart() {
        return tStart;
    }
    public long getTEnd() {
        return tStart+duration;
    }
    public double getTotalLength() {
        return totalLength;
    }
    public float getGain() {
        return gain;
    }
    public float getLoss() {
        return loss;
    }
    public float getMinEle() {
        return minEle;
    }
    public float getMaxEle() {
        return maxEle;
    }
    public int getNumPoints() {
        return numPoints;
    }

    public void setTStart(long tStart) {
        this.tStart = tStart;
    }
    public void setTotalLength(double totalLength) {
        this.totalLength = totalLength;
    }
    public void setGain(int gain) {
        this.gain = gain;
    }
    public void setLoss(int loss) {
        this.loss = loss;
    }
    public void setMinEle(float minEle) {
        this.minEle = minEle;
    }
    public void setMaxEle(float maxEle) {
        this.maxEle = maxEle;
    }


    private PointModel lastPoint4Distance = null;
    private PointModel lastPoint4GainLoss = null;
    private static final float ELE_THRESHOLD = 2.0f; // in meter

    public TrackLogStatistic(){}

    public void reset(){
        setTotalLength(0);
        setMaxEle(PointModel.NO_ELE);
        setMinEle(-PointModel.NO_ELE);
        setGain(0);
        setLoss(0);
        numPoints = 0;
        segmentIdx = -1;
        duration = 0;
        tStart = 0;
        lastPoint4Distance = null;
        lastPoint4GainLoss = null;
    }

    public void resetSegment() {
        lastPoint4Distance = null;
        lastPoint4GainLoss = null;
    }

    public TrackLogStatistic(int segmentIdx) {
        this.segmentIdx = segmentIdx;
    }

    public void toByteBuffer(ByteBuffer buf){
        buf.putInt(segmentIdx);
        buf.putLong(tStart);
        buf.putLong(duration);
        buf.putDouble(totalLength);
        buf.putInt((int)(gain*1000));
        buf.putInt((int)(loss*1000));
        buf.putInt((int)(minEle*1000));
        buf.putInt((int)(maxEle*1000));
        buf.putInt(numPoints);
    }

    public TrackLogStatistic fromByteBuffer(ByteBuffer buf){
        segmentIdx = buf.getInt();
        tStart = buf.getLong();
        duration = buf.getLong();
        totalLength = buf.getDouble();
        gain = buf.getInt()/1000.0f;
        loss = buf.getInt()/1000.0f;
        minEle = buf.getInt()/1000.0f;
        maxEle = buf.getInt()/1000.0f;
        numPoints = buf.getInt();
        return this;
    }



    synchronized public void updateWithPoint(PointModel point) {
        if (frozen) return; // don't update

        if (point != null){ // reset lastGpsPoint to null after TrackSegment finished
            numPoints++;
            if (point.getTimestamp() != PointModel.NO_TIME){
                if (tStart == PointModel.NO_TIME){
                    tStart = point.getTimestamp();
                }
                duration = point.getTimestamp() - tStart;
            }


            if (point.getEleA() != PointModel.NO_ELE) {
                float lastEle = point.getEleA();
                maxEle = Math.max(maxEle, lastEle);
                minEle = Math.min(minEle, lastEle);
            }
            if (point.getEleD() != PointModel.NO_ELE){
                process2GL(point);
            }
            if (lastPoint4Distance != null) {
                totalLength += PointModelUtil.distance(lastPoint4Distance, point);
            }
            lastPoint4Distance = point;
        } else {
            resetSegment();
        }
    }

    synchronized public void updateWithStatistics(TrackLogStatistic statistic){
        if (frozen) return; // don't update

        if ((tStart == PointModel.NO_TIME) && (statistic.tStart != PointModel.NO_TIME)) tStart = statistic.tStart;
        if (statistic.duration > 0) duration += statistic.duration;
        totalLength += statistic.totalLength;
        gain += statistic.gain;
        loss += statistic.loss;
        minEle = Math.min(minEle, statistic.minEle);
        maxEle = Math.max(maxEle, statistic.maxEle);
        numPoints += statistic.numPoints;
    }

    private void process2GL(PointModel point){
        if (( lastPoint4GainLoss == null) /* || ((totalLength == 0))*/){  // no reference point or first one (ignore first point height value)
            lastPoint4GainLoss = point;
        } else{
            float diff = point.getEleD() - lastPoint4GainLoss.getEleD();
            if (Math.abs(diff) > getEleThreshold(point)){
                lastPoint4GainLoss = point;
                if (diff > 0){
                    gain += diff;
                } else {
                    loss -= diff;
                }
            }
        }
    }

    private float getEleThreshold(PointModel point){

        if (point instanceof TrackLogPoint) {
            TrackLogPoint tlp = (TrackLogPoint) point;
            if (tlp.pressureAlt != PointModel.NO_ELE) return ELE_THRESHOLD;
        }
        return ELE_THRESHOLD*5;
    }

    public String durationToString(){
        return Formatter.format(Formatter.FormatType.FORMAT_DURATION, duration);
//        long seconds = duration/1000;
//        long minutes = seconds / 60;
//        long hours = minutes /60;
//        seconds -= minutes * 60;
//        minutes -= hours * 60;
//        return String.format(Locale.ENGLISH,"%d:%02d:%02d",hours,minutes,seconds);
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,"start=%s duration=%s totalLength=%.2f gain=%.1f loss=%.1f minEle=%.1f maxEle=%.1f numPoints=%d",
                sdf.format(tStart),durationToString(),totalLength,gain,loss,minEle,maxEle, numPoints);
    }

    public TrackLogStatistic setFrozen(boolean frozen) {
        this.frozen = frozen;
        return this;
    }
}
