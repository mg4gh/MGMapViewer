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
package mg.mapviewer.util;

import org.mapsforge.core.util.LatLongUtils;

import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.WriteablePointModel;

/** Utilities for PointModel.
 * Especially
 * <ul>
 *     <li>provide a "close" threshold</li>
 *     <li>calculate distence between two PointModel objects</li>
 *     <li>find an approach of a point towards a line</li>
 *     <li>compare point models</li>
 * </ul>
 */
public class PointModelUtil {

    protected static int closeThreshold;

    public static void init(int closeThreshold){
        PointModelUtil.closeThreshold = closeThreshold;
    }

    public static double distance(PointModel pm1, PointModel pm2) {
        return distance(pm1.getLat(), pm1.getLon(), pm2.getLat(), pm2.getLon() );
    }
    public static double distance(double lat1, double long1, double lat2, double long2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(long2 - long1);
        double a = Math.sin(dLat / 2.0D) * Math.sin(dLat / 2.0D) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2.0D) * Math.sin(dLon / 2.0D);
        double c = 2.0D * Math.atan2(Math.sqrt(a), Math.sqrt(1.0D - a));
        return c * 6378137.0D;
    }

    public static int getCloseThreshold(){
        return closeThreshold;
    }

    /** Check, whether pm has an approach to the segment segmentEnd1-segmentEnd2. Return the closest point in pmResult.
     * Be careful with passing references to pmResult !!*/
    public static boolean findApproach(PointModel pm, PointModel segmentEnd1, PointModel segmentEnd2, WriteablePointModel pmResult){

        double f = Math.cos(Math.toRadians(pm.getLat()));

        double minLat = Math.min(segmentEnd1.getLat(), segmentEnd2.getLat());
        double maxLat = Math.max(segmentEnd1.getLat(), segmentEnd2.getLat());
        double minLong = Math.min(segmentEnd1.getLon(), segmentEnd2.getLon());
        double maxLong = Math.max(segmentEnd1.getLon(), segmentEnd2.getLon());

        double closeDeltaLat = LatLongUtils.latitudeDistance(closeThreshold);
        double closeDeltaLong = LatLongUtils.longitudeDistance(closeThreshold,pm.getLat());

        if ((pm.getLat() > (maxLat + closeDeltaLat)) || (pm.getLat() < (minLat - closeDeltaLat))) return false;
        if ((pm.getLon() > (maxLong + closeDeltaLong)) || (pm.getLon() < (minLong - closeDeltaLong))) return false;

        double resLong;
        double resLat;
        if (segmentEnd1.getLon() == segmentEnd2.getLon()){
            resLong = segmentEnd1.getLon();
            resLat = pm.getLat();
        } else if (segmentEnd1.getLat() == segmentEnd2.getLat()){
            resLong = pm.getLon();
            resLat = segmentEnd1.getLat();
        } else {
            double deltaLong = segmentEnd2.getLon() - segmentEnd1.getLon();
            double deltaLat = segmentEnd2.getLat() - segmentEnd1.getLat();
            double anstieg = deltaLat / deltaLong;
            double anstiegOrtho = (-1/anstieg) * f*f;
            double nulldurchgangSegment = segmentEnd1.getLat() - anstieg * segmentEnd1.getLon();
            double nulldurchgangOrtho = pm.getLat() - anstiegOrtho * pm.getLon();

            resLong = (nulldurchgangOrtho -nulldurchgangSegment) / (anstieg - anstiegOrtho);
            resLat = anstieg * resLong + nulldurchgangSegment;

        }
        pmResult.setLon( Math.min(maxLong, Math.max(minLong, resLong)) );
        pmResult.setLat( Math.min(maxLat, Math.max(minLat, resLat)) );
        return true;
    }

    public static int compareTo(PointModel pm1, PointModel pm2){
        return compareTo(pm1.getLat(), pm1.getLon(), pm2.getLat(), pm2.getLon() );
    }


    public static int compareTo(double lat1, double long1, double lat2, double long2){
        if (lat1 < lat2) return -1;
        if (lat1 > lat2) return  1;
        if (long1 < long2) return -1;
        if (long1 > long2) return  1;
        return 0;

    }

    /** ensure that comparison of points doesn't fail due to rounding effects */
    public static double roundMD(double d){
        int md = LaLo.d2md(d);
        return LaLo.md2d(md);
    }



    /**
     * Calculates the amount of degrees of latitude for a given distance in meters.
     *
     * @param meters distance in meters
     * @return latitude degrees
     */
    public static double latitudeDistance(double meters) {
        return (meters * 360) / (2 * Math.PI * EQUATORIAL_RADIUS);
    }

    /**
     * Calculates the amount of degrees of longitude for a given distance in meters.
     *
     * @param meters   distance in meters
     * @param latitude the latitude at which the calculation should be performed
     * @return longitude degrees
     */
    public static double longitudeDistance(double meters, double latitude) {
        return (meters * 360) / (2 * Math.PI * EQUATORIAL_RADIUS * Math.cos(Math.toRadians(latitude)));
    }

    /**
     * The equatorial radius as defined by the <a href="http://en.wikipedia.org/wiki/World_Geodetic_System">WGS84
     * ellipsoid</a>. WGS84 is the reference coordinate system used by the Global Positioning System.
     */
    public static final double EQUATORIAL_RADIUS = 6378137.0;
}
