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

import android.util.Log;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.basic.NameUtil;


/**
 * Statistic of a TrackLog or of a Segment of a TrackLog.
 */

public class TrackLogStatistic {

    private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy_HH:mm:ss",Locale.GERMANY);
    private boolean frozen = false; //used to prevent recalc Statistic after MetaData.load ... and later lazy loading of Points

    private int segmentIdx = -1; // -1 means all segments; // -2 remainings statistic
    private long tStart = PointModel.NO_TIME;
    private long duration = 0;

    private double totalLength = 0;
    private float gain = 0;
    private float loss = 0;
    private float minEle = -PointModel.NO_ELE;
    private float maxEle = PointModel.NO_ELE;
    private int numPoints = 0;

    public int getSegmentIdx() {
        return segmentIdx;
    }
    public long getTStart() {
        return tStart;
    }
    public long getTEnd() {
        return tStart+duration;
    }
    public long getDuration() {
        return duration;
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

    public void setSegmentIdx(int segmentIdx) {
        this.segmentIdx = segmentIdx;
    }
    public void setTStart(long tStart) {
        this.tStart = tStart;
    }
    public void setDuration(long duration) {
        this.duration = duration;
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
    private float lastSmoothing4GainLoss = 0;
    private static final float ELE_THRESHOLD_BARO = 2.0f; // in meter
    private static final float ELE_THRESHOLD_ELSE = 10.0f; // in meter

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
//        tStart = 0; // tStart shall survive
        lastPoint4Distance = null;
        lastPoint4GainLoss = null;
        lastSmoothing4GainLoss = 0;
    }

    public void resetSegment() {
        lastPoint4Distance = null;
        lastPoint4GainLoss = null;
        lastSmoothing4GainLoss = 0;
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
        if ( lastPoint4GainLoss == null){  // no reference point or first one (ignore first point height value)
            lastPoint4GainLoss = point;
        } else {
            float smoothingFactor = Math.min(20, point.getEleAcc()) / 20.0f;
            float smoothingDiff = (point.getEleD() - lastPoint4GainLoss.getEleD());
            float smoothing = smoothingDiff * smoothingFactor / 2; // smoothing is maximal half of the difference

            float diff = (point.getEleD()-smoothing) - (lastPoint4GainLoss.getEleD()-lastSmoothing4GainLoss);
            Log.v(MGMapApplication.LABEL, NameUtil.context()+" point.getEleAcc()="+point.getEleAcc()+" smoothingFactor="+smoothingFactor+" smoothingDiff="+smoothingDiff+" smoothing="+smoothing+" eleD="+point.getEleD());
            if (Math.abs(diff) >= getEleThreshold(point)){
                lastPoint4GainLoss = point;
                lastSmoothing4GainLoss = smoothing;
                if (diff > 0){
                    gain += diff;
                } else {
                    loss -= diff;
                }
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" use it: smoothing="+smoothing+" eleD="+point.getEleD()+" gain="+gain+" loss="+loss);
            }
        }
    }

    private float getEleThreshold(PointModel point){

        if (point instanceof TrackLogPoint) {
            TrackLogPoint tlp = (TrackLogPoint) point;
            if (tlp.getPressureAlt() != PointModel.NO_ELE) return ELE_THRESHOLD_BARO;
        }
        return ELE_THRESHOLD_ELSE;
    }

    public String durationToString(){
        return Formatter.format(Formatter.FormatType.FORMAT_DURATION, duration);
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,"start=%s duration=%s totalLength=%.2f gain=%.1f loss=%.1f minEle=%.1f maxEle=%.1f numPoints=%d",
                sdf.format(tStart),durationToString(),totalLength,gain,loss,minEle,maxEle, numPoints);
    }

    boolean isFrozen(){
        return frozen;
    }
    public TrackLogStatistic setFrozen(boolean frozen) {
        this.frozen = frozen;
        return this;
    }
}
