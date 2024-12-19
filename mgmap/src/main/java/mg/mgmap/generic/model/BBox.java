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

import androidx.annotation.NonNull;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Locale;

import mg.mgmap.generic.util.basic.LaLo;

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
        minLatitude = PointModel.NO_LAT_LONG ;
        minLongitude = PointModel.NO_LAT_LONG;
        maxLatitude = -PointModel.NO_LAT_LONG;
        maxLongitude = -PointModel.NO_LAT_LONG;
        return this;
    }

    public BBox extend(double latitude, double longitude){
        if ((Math.abs(latitude) < PointModel.NO_LAT_LONG) && (Math.abs(longitude) < PointModel.NO_LAT_LONG)){
            double rLat = PointModelUtil.roundMD(latitude);
            double rLong = PointModelUtil.roundMD(longitude);
            minLatitude = Math.min(minLatitude, rLat);
            minLongitude = Math.min(minLongitude, rLong);
            maxLatitude = Math.max(maxLatitude, rLat);
            maxLongitude = Math.max(maxLongitude, rLong);
        }
        return this;
    }

    public boolean isInitial(){
        return ((minLatitude == PointModel.NO_LAT_LONG) && (minLongitude == PointModel.NO_LAT_LONG) &&
                (maxLatitude == -PointModel.NO_LAT_LONG) && (maxLongitude == -PointModel.NO_LAT_LONG));
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
    public boolean contains(BBox bBox) {
        return contains(bBox.maxLatitude, bBox.maxLongitude) && contains(bBox.minLatitude, bBox.minLongitude);
    }
    public boolean contains(BoundingBox boundingBox) {
        return contains(boundingBox.maxLatitude, boundingBox.maxLongitude) && contains(boundingBox.minLatitude, boundingBox.minLongitude);
    }
    public boolean contains(double latitude, double longitude) {
        return this.minLatitude <= latitude && this.maxLatitude >= latitude && this.minLongitude <= longitude && this.maxLongitude >= longitude;
    }

    public static BBox fromBoundingBox(BoundingBox boundingBox){
        BBox bBox = new BBox();
        if (boundingBox != null){
            bBox.extend(boundingBox.minLatitude, boundingBox.minLongitude);
            bBox.extend(boundingBox.maxLatitude, boundingBox.maxLongitude);
        }
        return bBox;
    }

    public PointModel getCenter(){
        return new PointModelImpl( (maxLatitude+minLatitude)/2, (maxLongitude+minLongitude)/2);
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
    /** @noinspection UnusedReturnValue*/
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
        return String.format(Locale.ENGLISH, "minLat=%2.6f, minLon=%2.6f, maxLat=%2.6f, maxLon=%2.6f",minLatitude,minLongitude,maxLatitude,maxLongitude);
    }

    public long fingerprint(){
        return ((long)LaLo.d2md(this.minLatitude) + LaLo.d2md(this.maxLatitude)) <<32 +  (LaLo.d2md(this.minLongitude)+LaLo.d2md(this.maxLongitude));
    }

}
