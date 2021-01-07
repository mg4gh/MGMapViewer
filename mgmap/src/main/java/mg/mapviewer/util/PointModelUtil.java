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

import android.util.Log;

import org.mapsforge.core.util.LatLongUtils;

import java.util.ArrayList;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.MultiPointModel;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.PointModelImpl;
import mg.mapviewer.model.TrackLogPoint;
import mg.mapviewer.model.TrackLogRefApproach;
import mg.mapviewer.model.WriteablePointModel;
import mg.mapviewer.model.WriteablePointModelImpl;

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

    public static final long NO_POS = new PointModelImpl(PointModel.NO_LAT_LONG, PointModel.NO_LAT_LONG).getLaLo();

    /**
     * The equatorial radius as defined by the <a href="http://en.wikipedia.org/wiki/World_Geodetic_System">WGS84
     * ellipsoid</a>. WGS84 is the reference coordinate system used by the Global Positioning System.
     */
    public static final double EQUATORIAL_RADIUS = 6378137.0;

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
        return c * EQUATORIAL_RADIUS;
//        return c * 6378137.0D;
    }

    public static double distance(MultiPointModel mpm){
        double length = 0;
        for (int i=1, j=0; i<mpm.size(); j=i++){
            length += distance(mpm.get(i), mpm.get(j));
        }
        return length;
    }

    public static int getCloseThreshold(){
        return closeThreshold;
    }

    public static boolean findApproach(PointModel pm, PointModel segmentEnd1, PointModel segmentEnd2, WriteablePointModel pmResult) {
        return findApproach(pm,segmentEnd1,segmentEnd2,pmResult,closeThreshold);
    }
    /** Check, whether pm has an approach to the segment segmentEnd1-segmentEnd2. Return the closest point in pmResult.
     * Be careful with passing references to pmResult !!*/
    public static boolean findApproachOld(PointModel pm, PointModel segmentEnd1, PointModel segmentEnd2, WriteablePointModel pmResult, int threshold){

        double f = Math.cos(Math.toRadians(pm.getLat()));

        double minLat = Math.min(segmentEnd1.getLat(), segmentEnd2.getLat());
        double maxLat = Math.max(segmentEnd1.getLat(), segmentEnd2.getLat());
        double minLong = Math.min(segmentEnd1.getLon(), segmentEnd2.getLon());
        double maxLong = Math.max(segmentEnd1.getLon(), segmentEnd2.getLon());

        double closeDeltaLat = LatLongUtils.latitudeDistance(threshold);
        double closeDeltaLong = LatLongUtils.longitudeDistance(threshold,pm.getLat());

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


    public static boolean findApproach(PointModel pm, PointModel segmentEnd1, PointModel segmentEnd2,
                                        WriteablePointModel pmResult, int threshold) {

        double f = Math.cos(Math.toRadians(pm.getLat()));

        double minLat = Math.min(segmentEnd1.getLat(), segmentEnd2.getLat());
        double maxLat = Math.max(segmentEnd1.getLat(), segmentEnd2.getLat());
        double minLong = Math.min(segmentEnd1.getLon(), segmentEnd2.getLon());
        double maxLong = Math.max(segmentEnd1.getLon(), segmentEnd2.getLon());

        double closeDeltaLat = latitudeDistance(threshold);
        double closeDeltaLong = longitudeDistance(threshold, pm.getLat());

        if ((minLong == maxLong) && (minLat == maxLat))
            return false;
        if ((pm.getLat() > (maxLat + closeDeltaLat)) || (pm.getLat() < (minLat - closeDeltaLat)))
            return false;
        if ((pm.getLon() > (maxLong + closeDeltaLong)) || (pm.getLon() < (minLong - closeDeltaLong)))
            return false;

        double resLong;
        double resLat;

        // vector v1 = Vector from segmentEnd1 to segmentEnd2
        double v1x = segmentEnd2.getLon() - segmentEnd1.getLon();
        double v1y = segmentEnd2.getLat() - segmentEnd1.getLat();
        // vector v1o = orthogonal vector from v1
        double v1ox = v1y;
        double v1oy = -v1x;
        // vector dp = Vector from segmentEnd1 to pm
        double dpx = pm.getLon() - segmentEnd1.getLon();
        double dpy = pm.getLat() - segmentEnd1.getLat();
        // vector v2 = orthogonal Vector from v1 - scaled by latitude factor f
        double v2x = v1y/(f*f);
        double v2y = -v1x;
        // segmentEnd1 + n * v1 == pm + m * v2
        double m= crossProduct(v1ox, v1oy, dpx, dpy) / crossProduct(v1ox, v1oy, v2x, v2y); //resolve the two vector equations to eliminate n
        resLong = pm.getLon() - m*v2x;
        resLat = pm.getLat() - m*v2y;

        pmResult.setLon(Math.min(maxLong, Math.max(minLong, resLong)));
        pmResult.setLat(Math.min(maxLat, Math.max(minLat, resLat)));
        return true;
    }

    public static double crossProduct(double x1, double y1, double x2, double y2) {
        return x1 * x2 + y1 * y2;
    }


    public static double calcDegree(PointModel pm1, PointModel pm2, PointModel pm3) {
        double degree = calcAngle(pm1,pm2,pm3);
        if ((degree >= 0) && (!turnLeft(pm1,pm2,pm3))){
            degree = 360 - degree;
        }
        return degree;
    }

    public static int clock4degree(double degree){
        if ((degree < 0)||(degree>360)) return -1;
        int clock = ((int)(degree+195))/30;
        return (clock > 12)?(clock-12):clock;
    }

    public static double calcAngle(PointModel pm1, PointModel pm2, PointModel pm3){
        double d3 = distance(pm1, pm2);
        double d2 = distance(pm1, pm3);
        double d1 = distance(pm2, pm3);
        if ((Math.abs(d3) < 0.01) || (Math.abs(d1) < 0.01)){ // if distance is too short, can't calculate angle
            return -1;
        }
        return Math.acos(((d1*d1)+(d3*d3)-(d2*d2))/(2*d1*d3)) * 180 / Math.PI;
    }

    public static boolean turnLeft(PointModel pm1, PointModel pm2, PointModel pm3) {

        if (pm1.getLat() == pm2.getLat()){
            return ((pm1.getLon() < pm2.getLon()) == (pm3.getLat() > pm1.getLat()));
        }
        if (pm1.getLon() == pm2.getLon()){
            return ((pm1.getLat() < pm2.getLat()) == (pm3.getLon() < pm1.getLon()));
        }

        double deltaLong = pm2.getLon() - pm1.getLon();
        double deltaLat = pm2.getLat() - pm1.getLat();
        double anstieg = deltaLat / deltaLong;
        double nulldurchgangSegment = pm1.getLat() - anstieg * pm1.getLon();
        double y3 = anstieg*pm3.getLon() + nulldurchgangSegment;
        return ((pm1.getLon() < pm2.getLon()) == (pm3.getLat() > y3));
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

    public static PointModel interpolate(PointModel pm1, PointModel pm2, double distFromPm1){
        double distance = distance(pm1,pm2);
        double lat = interpolate(0, distance, pm1.getLat(), pm2.getLat(), distFromPm1);
        double lon = interpolate(0, distance, pm1.getLon(), pm2.getLon(), distFromPm1);
        PointModel pm = new PointModelImpl(lat,lon);
        Log.i(MGMapApplication.LABEL, NameUtil.context()+String.format(" distCheck: %1.1fm",distance(pm1,pm)));
        return pm;
    }

    private static double interpolate(double refMin, double refMax, double valMin, double valMax, double ref){
        double scale = (ref - refMin) / (refMax - refMin);
        return scale * (valMax - valMin) + valMin ;
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

    public static void getBestDistance(ArrayList<? extends MultiPointModel> mpms, PointModel pm, TrackLogRefApproach bestMatch){
        WriteablePointModel pmApproachCandidate = new WriteablePointModelImpl();//new TrackLogPoint();
        WriteablePointModel pmApproach = new WriteablePointModelImpl();//new TrackLogPoint();
        for (int segmentIdx = 0; segmentIdx< mpms.size(); segmentIdx++){
            MultiPointModel segment = mpms.get(segmentIdx);
            for (int i = 1, j = 0; i < segment.size(); j = i++) {
                if (PointModelUtil.findApproach(pm, segment.get(i), segment.get(j), pmApproachCandidate, (int)(bestMatch.getDistance()+1) )){
                    double distance = PointModelUtil.distance( pm, pmApproachCandidate);
                    if (distance < bestMatch.getDistance()){
                        bestMatch.setSegmentIdx( segmentIdx );
                        if (bestMatch.getApproachPoint() == null) bestMatch.setApproachPoint(pmApproach);
                        pmApproach.setLat(pmApproachCandidate.getLat());
                        pmApproach.setLon(pmApproachCandidate.getLon());
                        bestMatch.setDistance( distance );
                        bestMatch.setEndPointIndex(i);
                    }
                }
            }
        }
    }

    public static  void getBestPoint(ArrayList<? extends MultiPointModel> mpms, PointModel pm, TrackLogRefApproach bestMatch){
        for (int segmentIdx = 0; segmentIdx< mpms.size(); segmentIdx++){
            MultiPointModel segment = mpms.get(segmentIdx);
            for (int i = 0; i < segment.size(); i++) {
                double distance = PointModelUtil.distance(pm,segment.get(i));
                if (distance < bestMatch.getDistance()){
                    bestMatch.setSegmentIdx( segmentIdx );
                    bestMatch.setApproachPoint(segment.get(i));
                    bestMatch.setDistance( distance );
                    bestMatch.setEndPointIndex(i);
                }
            }
        }
    }

}
