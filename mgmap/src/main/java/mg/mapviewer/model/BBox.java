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

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.LatLongUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

import mg.mapviewer.util.LaLo;
import mg.mapviewer.util.PointModelUtil;

/** A bounding box implementation */
public class BBox {


    public double minLatitude;
    public double minLongitude;
    public double maxLatitude;
    public double maxLongitude;

    public BBox(){
        clear();
    }

    public BBox clear(){
        minLatitude = LatLongUtils.LATITUDE_MAX ; //Double.MAX_VALUE;
        minLongitude = LatLongUtils.LONGITUDE_MAX; //Double.MAX_VALUE;
        maxLatitude = LatLongUtils.LATITUDE_MIN; // Double.MIN_VALUE;
        maxLongitude = LatLongUtils.LONGITUDE_MIN; //Double.MIN_VALUE;
        return this;
    }

    public BBox extend(double latitude, double longitude){
        double rLat = PointModelUtil.roundMD(latitude);
        double rLong = PointModelUtil.roundMD(longitude);
        minLatitude = Math.min(minLatitude, rLat);
        minLongitude = Math.min(minLongitude, rLong);
        maxLatitude = Math.max(maxLatitude, rLat);
        maxLongitude = Math.max(maxLongitude, rLong);
        return this;
    }

    public BBox extend(double meters){
        double closeDeltaLat = PointModelUtil.latitudeDistance(meters);
        double closeDeltaLong = PointModelUtil.longitudeDistance(meters, (minLatitude+maxLatitude)/2);
        extend(minLatitude-closeDeltaLat, minLongitude-closeDeltaLong);
        return extend(maxLatitude+closeDeltaLat, maxLongitude+closeDeltaLong);
    }
    public BBox extend(PointModel pm){
        return extend(pm.getLat(), pm.getLon());
    }
    public BBox extend(LatLong latLong){
        return extend(latLong.latitude, latLong.longitude);
    }
    public BBox extend(BBox bBox){
        return extend(bBox.minLatitude,bBox.minLongitude).extend(bBox.maxLatitude, bBox.maxLongitude);
    }
    public BBox extend(ArrayList<PointModel> points){
        for (PointModel pointModel : points){
            extend(pointModel);
        }
        return this;
    }

    public boolean intersects(BoundingBox boundingBox) {
        return intersects(boundingBox.minLatitude, boundingBox.minLongitude, boundingBox.maxLatitude, boundingBox.maxLongitude);
    }
    public boolean intersects(BBox bBox) {
        return intersects(bBox.minLatitude, bBox.minLongitude, bBox.maxLatitude, bBox.maxLongitude);
    }
    public boolean intersects(double minLat, double minLong, double maxLat, double maxLong) {
        return (this.maxLatitude >= minLat) && (this.maxLongitude >= minLong) && (this.minLatitude <= maxLat) && (this.minLongitude <= maxLong);
    }


    public boolean contains(PointModel pm) {
        return contains(pm.getLat(), pm.getLon());
    }
    public boolean contains(double latitude, double longitude) {
        return this.minLatitude <= latitude && this.maxLatitude >= latitude && this.minLongitude <= longitude && this.maxLongitude >= longitude;
    }
    public boolean containsStrictly(double latitude, double longitude) {
        return this.minLatitude < latitude && this.maxLatitude > latitude && this.minLongitude < longitude && this.maxLongitude > longitude;
    }

    public boolean isPartOf(BoundingBox boundingBox) {
        return isPartOf(boundingBox.minLatitude, boundingBox.minLongitude, boundingBox.maxLatitude, boundingBox.maxLongitude);
    }
    public boolean isPartOf(double minLat, double minLong, double maxLat, double maxLong) {
        return (this.maxLatitude <= maxLat) && (this.maxLongitude <= maxLong) && (this.minLatitude >= minLat) && (this.minLongitude >= minLong);
    }


    public static BBox fromBoundingBox(BoundingBox boundingBox){
        return new BBox().
                extend(boundingBox.minLatitude, boundingBox.minLongitude).
                extend(boundingBox.maxLatitude, boundingBox.maxLongitude);
    }

    public BoundingBox toBoundingBox(BBox bBox){
        return new BoundingBox(bBox.minLatitude, bBox.minLongitude, bBox.maxLatitude, bBox.maxLongitude);
    }

    public LatLong getCenter(){
        return new LatLong( (maxLatitude+minLatitude)/2, (maxLongitude+minLongitude)/2);
    }

    /**
     *
     * @param lat1 inner point latitude
     * @param lon1 inner point longitude
     * @param lat2 outer point latitude
     * @param lon2 outer point longitude
     * @param clipRes Reference to a WriteablePointModel, which contains the result of the clip action on return
     */
    public void clip(double lat1, double lon1, double lat2, double lon2, WriteablePointModel clipRes){
        if ((lat1 < maxLatitude) && (lat2 > maxLatitude)) {
            lon2 = (((maxLatitude-lat1)*(lon2-lon1)/(lat2-lat1)) + lon1);
            lat2 = maxLatitude;
        }
        if ((lat1 > minLatitude) && (lat2 < minLatitude)) {
            lon2 = (((minLatitude-lat1)*(lon2-lon1)/(lat2-lat1)) + lon1);
            lat2 = minLatitude;
        }
        if ((lon1 < maxLongitude) && (lon2 > maxLongitude)) {
            lat2 = (((maxLongitude-lon1)*(lat2-lat1)/(lon2-lon1)) + lat1);
            lon2 = maxLongitude;
        }
        if ((lon1 > minLongitude) && (lon2 < minLongitude)) {
            lat2 = (((minLongitude-lon1)*(lat2-lat1)/(lon2-lon1)) + lat1);
            lon2 = minLongitude;
        }
        clipRes.setLat(lat2);
        clipRes.setLon(lon2);
    }


    public void toByteBuffer(ByteBuffer buf){
        buf.putInt(LaLo.d2md(minLatitude));
        buf.putInt(LaLo.d2md(maxLatitude));
        buf.putInt(LaLo.d2md(minLongitude));
        buf.putInt(LaLo.d2md(maxLongitude));
    }
    public BBox fromByteBuffer(ByteBuffer buf){
        minLatitude = LaLo.md2d(buf.getInt());
        maxLatitude = LaLo.md2d(buf.getInt());
        minLongitude = LaLo.md2d(buf.getInt());
        maxLongitude = LaLo.md2d(buf.getInt());
        return this;
    }


    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.GERMAN, "minLat=%2.6f, minLon=%2.6f, maxLat=%2.6f, maxLon=%2.6f",minLatitude,minLongitude,maxLatitude,maxLongitude);
    }

}
