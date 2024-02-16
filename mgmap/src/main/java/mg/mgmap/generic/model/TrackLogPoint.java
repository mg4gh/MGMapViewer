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

import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.basic.LaLo;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Locale;

/**
 * Stores the data of a single point of a TrackLog.
 */
public class TrackLogPoint extends WriteablePointModelImpl implements WriteablePointModel{

    public static TrackLogPoint createGpsLogPoint(long timestamp, double latitude, double longitude, float accuracy, double wgs84ele, float geoidOffset, float wgs84eleAcc){
        TrackLogPoint lp = new TrackLogPoint();
        lp.timestamp = timestamp;
        lp.la = LaLo.d2md(latitude);
        lp.lo = LaLo.d2md(longitude);
        lp.nmeaAcc = (Math.round(accuracy*10))/10.0f;
        if (wgs84ele != NO_ELE){
            if (wgs84ele != 0) { // don't trust wgs84ele value 0
                lp.wgs84ele = (float) wgs84ele;
                lp.nmeaEle = lp.wgs84ele - geoidOffset;
                lp.nmeaEleAcc = (Math.round(wgs84eleAcc*10))/10.0f;
            }
        }
        return lp;
    }

    public static TrackLogPoint createLogPoint(double latitude, double longitude){
        TrackLogPoint lp = new TrackLogPoint();
        lp.la = LaLo.d2md(latitude);
        lp.lo = LaLo.d2md(longitude);
        return lp;
    }


    private long timestamp = NO_TIME;
    private float nmeaAcc = NO_ACC; // m rounded - horizontal accuracy
    private float wgs84ele = NO_ELE; // m
    private float nmeaEle = NO_ELE; // m
    private float nmeaEleAcc = NO_ACC; // m
    private float pressure = NO_PRES; // hpa
    private float pressureEle = NO_ELE; // m
    private float pressureAcc = 0; // delta hpa - only temporary used
    private float pressureEleAcc = NO_ACC; // m
    private float hgtEle = NO_ELE; // m
    private float hgtEleAcc = NO_ACC; // m

    public TrackLogPoint(){}

    public TrackLogPoint(TrackLogPoint tlp){
        this.timestamp = tlp.timestamp;
        this.la = tlp.la;
        this.lo = tlp.lo;
        this.nmeaAcc = tlp.nmeaAcc;
        this.pressure = tlp.pressure;
        this.wgs84ele = tlp.wgs84ele;
        this.nmeaEle = tlp.nmeaEle;
        this.pressureEle = tlp.pressureEle;
        this.ele = tlp.ele;
        this.hgtEle = tlp.hgtEle;
        this.hgtEleAcc = tlp.hgtEleAcc;
        this.nmeaEleAcc = tlp.nmeaEleAcc;
        this.pressureEleAcc = tlp.pressureEleAcc;
        this.pressureAcc = tlp.pressureAcc;
    }


    public void toByteBuffer(ByteBuffer buf){
        buf.putLong(timestamp);
        buf.putInt(la);
        buf.putInt(lo);
        buf.putInt((int)(nmeaAcc *1000));
        buf.putInt((int)(pressure*1000));
        buf.putInt((int)(wgs84ele *1000));
        buf.putInt((int)(nmeaEle *1000));
        buf.putInt((int)(ele*1000));
        buf.putInt((int)(pressureEle *1000));
        buf.putInt((int)(hgtEle *1000));
        buf.putInt((int)(hgtEleAcc *1000));
        buf.putInt((int)(nmeaEleAcc *1000));
        buf.putInt((int)(pressureEleAcc *1000));
    }
    public void fromByteBuffer(ByteBuffer buf){
        timestamp = buf.getLong();
        la = buf.getInt();
        lo = buf.getInt();
        nmeaAcc = buf.getInt()/1000.0f;
        pressure = buf.getInt()/1000.0f;
        wgs84ele = buf.getInt()/1000.0f;
        nmeaEle = buf.getInt()/1000.0f;
        ele = buf.getInt()/1000.0f;
        pressureEle = buf.getInt()/1000.0f;
        hgtEle = buf.getInt()/1000.0f;
        hgtEleAcc = buf.getInt()/1000.0f;
        nmeaEleAcc = buf.getInt()/1000.0f;
        pressureEleAcc = buf.getInt()/1000.0f;
    }

    @Override
    public float getEle() {
        if (ele != PointModel.NO_ELE){
            return ele;
        }
        if (hgtEle != PointModel.NO_ELE){
            return hgtEle;
        }
        if (nmeaEle != PointModel.NO_ELE){
            return nmeaEle;
        }
        return super.getEle();
    }

    @Override
    public float getEleAcc() {
        if (pressureEle != PointModel.NO_ELE){
            return pressureEleAcc;
        }
        if (hgtEle != PointModel.NO_ELE){
            return hgtEleAcc;
        }
        if (nmeaEle != PointModel.NO_ELE){
            return nmeaEleAcc;
        }
        return super.getEleAcc();
    }

    public long getTimestamp(){
        return timestamp;
    }
    public float getNmeaEle(){
        return nmeaEle;
    }
    public float getNmeaAcc() {
        return nmeaAcc;
    }
    public float getPressure() {
        return pressure;
    }
    public float getWgs84ele() {
        return wgs84ele;
    }
    public float getPressureEle() {
        return pressureEle;
    }
    public float getHgtEle() {
        return hgtEle;
    }
    public float getHgtEleAcc() {
        return hgtEleAcc;
    }
    public float getNmeaEleAcc() {
        return nmeaEleAcc;
    }
    public float getPressureAcc() {
        return pressureAcc;
    }
    public float getPressureEleAcc() {
        return pressureEleAcc;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public void setNmeaAcc(float nmeaAcc) {
        this.nmeaAcc = Math.round(nmeaAcc);
    }
    public void setPressure(float pressure) {
        this.pressure = pressure;
    }
    public void setWgs84ele(float wgs84ele) {
        this.wgs84ele = wgs84ele;
    }
    public void setNmeaEle(float nmeaEle) {
        this.nmeaEle = nmeaEle;
    }
    public void setPressureEle(float pressureEle) {
        this.pressureEle = pressureEle;
    }
    public void setHgtEle(float hgtEle) {
        this.hgtEle = hgtEle;
    }
    public void setHgtEleAcc(float hgtEleAcc) {
        this.hgtEleAcc = hgtEleAcc;
    }
    public void setNmeaEleAcc(float nmeaEleAcc) {
        this.nmeaEleAcc = nmeaEleAcc;
    }
    public void setPressureAcc(float pressureAcc) {
        this.pressureAcc = pressureAcc;
    }
    public void setPressureEleAcc(float pressureEleAcc) {
        this.pressureEleAcc = pressureEleAcc;
    }

    public String toLongString(){
        Date date = new Date(getTimestamp());
        return String.format(Locale.ENGLISH,"%s %s: lat=%.6f,lon=%.6f,nmeaAcc=%.1f, nmeaEle=%.1f,nmeaEleAcc=%.1f, hgtEle=%.1f,hgtEleAcc=%.1f, press=%.3f,pressAcc=%.3f,pressEle=%.1f,pressEleAcc=%.1f",
                Formatter.SDF1a.format(date),Formatter.SDF3.format(date),
                getLat(),getLon(),getNmeaAcc(),
                getNmeaEle(),getNmeaEleAcc(),
                getHgtEle(),getHgtEleAcc(),
                getPressure(), getPressureAcc(), getPressureEle(), getPressureEleAcc()
                );

    }

}
