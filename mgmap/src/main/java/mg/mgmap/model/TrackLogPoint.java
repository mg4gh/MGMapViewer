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
package mg.mgmap.model;

import android.location.Location;

import org.mapsforge.core.model.LatLong;
import mg.mgmap.util.LaLo;

import mg.mgmap.util.AltitudeProvider;
import mg.mgmap.util.Geoid;

import java.nio.ByteBuffer;

/**
 * Stores the data of a single point of a TrackLog.
 */
public class TrackLogPoint extends WriteablePointModelImpl implements WriteablePointModel{


//    public static final int NO_ELE = Integer.MIN_VALUE;

    public TrackLogPoint(){}

    public TrackLogPoint(TrackLogPoint tlp){
        this.timestamp = tlp.timestamp;
        this.la = tlp.la;
        this.lo = tlp.lo;
        this.accuracy = tlp.accuracy;
        this.pressure = tlp.pressure;
        this.wgs84alt = tlp.wgs84alt;
        this.nmeaAlt = tlp.nmeaAlt;
        this.pressureAlt = tlp.pressureAlt;
        this.ele = tlp.ele;
        this.hgtAlt = tlp.hgtAlt;
    }


    public static TrackLogPoint createGpsLogPoint(Location location){
        TrackLogPoint lp = new TrackLogPoint();
        lp.timestamp = System.currentTimeMillis();
        lp.la = LaLo.d2md(location.getLatitude());
        lp.lo = LaLo.d2md(location.getLongitude());
        lp.accuracy = Math.round(location.getAccuracy());
        if (location.hasAltitude()){
            lp.wgs84alt = (float)location.getAltitude();
            float geoidOffset = Geoid.getInstance().getGeoidOffset(lp.getLat(),lp.getLon());
            lp.nmeaAlt = lp.wgs84alt - geoidOffset;
        }
        lp.hgtAlt = AltitudeProvider.getAlt(lp);
        if (lp.wgs84alt != 0){
            lp.ele = lp.nmeaAlt;
        } else { // don't trust an exact 0 value for wgs84alt
            lp.ele = lp.hgtAlt;
        }
        return lp;
    }


    public static TrackLogPoint createLogPoint(double latitude, double longitude){
        TrackLogPoint lp = new TrackLogPoint();
        lp.la = LaLo.d2md(latitude);
        lp.lo = LaLo.d2md(longitude);
        return lp;
    }


    protected long timestamp = 0;

    float accuracy = 0; // m rounded
    float pressure = NO_PRES; // hpa
    float wgs84alt = NO_ELE; // m
    float nmeaAlt = NO_ELE; // m
    float pressureAlt = NO_ELE; // m
//    float calibratedAlt = NO_ELE; // m
    float hgtAlt = NO_ELE; // m


    public void toByteBuffer(ByteBuffer buf){
        buf.putLong(timestamp);
        buf.putInt(la);
        buf.putInt(lo);
        buf.putInt((int)accuracy);
        buf.putInt((int)(pressure*1000));
        buf.putInt((int)(wgs84alt*1000));
        buf.putInt((int)(nmeaAlt*1000));
        buf.putInt((int)(ele*1000));
        buf.putInt((int)(pressureAlt*1000));
        buf.putInt((int)(hgtAlt*1000));
    }
    public void fromByteBuffer(ByteBuffer buf){
        timestamp = buf.getLong();
        la = buf.getInt();
        lo = buf.getInt();
        accuracy = buf.getInt();
        pressure = buf.getInt()/1000.0f;
        wgs84alt = buf.getInt()/1000.0f;
        nmeaAlt = buf.getInt()/1000.0f;
        ele = buf.getInt()/1000.0f;
        pressureAlt = buf.getInt()/1000.0f;
        hgtAlt = buf.getInt()/1000.0f;
    }

    @Override
    public float getEleA() {
        if (ele != PointModel.NO_ELE){
            return ele;
        }
        if (hgtAlt != PointModel.NO_ELE){
            return hgtAlt;
        }
        return PointModel.NO_ELE;
    }

    @Override
    public float getEleD() {
        if (pressureAlt != PointModel.NO_ELE){
            return pressureAlt;
        }
        if (ele != PointModel.NO_ELE){
            return ele;
        }
        if (hgtAlt != PointModel.NO_ELE){
            return hgtAlt;
        }
        return PointModel.NO_ELE;
    }

    public LatLong getLatLong(){
        return new LatLong(LaLo.md2d(la),LaLo.md2d(lo));
    }


    public long getTimestamp(){
        return timestamp;
    }
    public float getNmeaAlt(){
        return nmeaAlt;
    }
//    public float getCalibratedAlt(){
//        return ele;
//    }
    public float getAccuracy() {
        return accuracy;
    }
    public float getPressure() {
        return pressure;
    }
    public float getWgs84alt() {
        return wgs84alt;
    }
    public float getPressureAlt() {
        return pressureAlt;
    }
    public float getHgtAlt() {
        return hgtAlt;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }
    public void setPressure(float pressure) {
        this.pressure = pressure;
    }
    public void setWgs84alt(float wgs84alt) {
        this.wgs84alt = wgs84alt;
    }
    public void setNmeaAlt(float nmeaAlt) {
        this.nmeaAlt = nmeaAlt;
    }
    public void setPressureAlt(float pressureAlt) {
        this.pressureAlt = pressureAlt;
    }
    public void setHgtAlt(float hgtAlt) {
        this.hgtAlt = hgtAlt;
    }
//    public void setCalibratedAlt(float calibratedAlt) {
//        this.ele = calibratedAlt;
//    }

}