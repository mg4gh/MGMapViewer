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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.generic.util.basic.LaLo;
import mg.mgmap.generic.util.basic.MGLog;

/** Utilities for PointModel.
 * Especially
 * <ul>
 *     <li>provide a "close" threshold</li>
 *     <li>calculate distance between two PointModel objects</li>
 *     <li>find an approach of a point towards a line</li>
 *     <li>compare point models</li>
 * </ul>
 */
public class PointModelUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static final long NO_POS = LaLo.getLaLo(PointModel.NO_LAT_LONG_MD, PointModel.NO_LAT_LONG_MD);
    public static final float ELE_FACTOR = 100.0f;

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
        if ((pm1 == null) || (pm2 == null)) return 0;
        return distance(pm1.getLat(), pm1.getLon(), pm2.getLat(), pm2.getLon() );
    }

    public static double distance(double lat1, double long1, double lat2, double long2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(long2 - long1);
        double a = Math.sin(dLat / 2.0D) * Math.sin(dLat / 2.0D) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2.0D) * Math.sin(dLon / 2.0D);
        double c = 2.0D * Math.atan2(Math.sqrt(a), Math.sqrt(1.0D - a));
        return c * EQUATORIAL_RADIUS;
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
    public static boolean findApproach(PointModel pm, PointModel segmentEnd1, PointModel segmentEnd2, WriteablePointModel pmResult, int threshold) {
        return findApproach(pm, segmentEnd1.getLat(), segmentEnd1.getLon(), segmentEnd1.getEle(), segmentEnd1.getTimestamp(),
                                segmentEnd2.getLat(), segmentEnd2.getLon(), segmentEnd2.getEle(), segmentEnd2.getTimestamp(), pmResult, threshold);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static boolean findApproach(PointModel pm, double latEnd1, double lonEnd1, float eleEnd1, long tsEnd1, double latEnd2, double lonEnd2, float eleEnd2, long tsEnd2, WriteablePointModel pmResult, int threshold) {
        double minLat = Math.min(latEnd1, latEnd2);
        double maxLat = Math.max(latEnd1, latEnd2);
        double minLong = Math.min(lonEnd1, lonEnd2);
        double maxLong = Math.max(lonEnd1, lonEnd2);

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
        double v1x = lonEnd2 - lonEnd1;
        double v1y = latEnd2 - latEnd1;
        // vector v1o = orthogonal vector from v1
        double v1ox = v1y;
        double v1oy = -v1x;
        // vector dp = Vector from segmentEnd1 to pm
        double dpx = pm.getLon() - lonEnd1;
        double dpy = pm.getLat() - latEnd1;
        // vector v2 = orthogonal Vector from v1 - scaled by latitude factor f
        double f = Math.cos(Math.toRadians(pm.getLat()));
        double v2x = v1y/(f*f);
        double v2y = -v1x;
        // segmentEnd1 + n * v1 == pm + m * v2
        double m= crossProduct(v1ox, v1oy, dpx, dpy) / crossProduct(v1ox, v1oy, v2x, v2y); //resolve the two vector equations to eliminate n
        resLong = pm.getLon() - m*v2x;
        resLat = pm.getLat() - m*v2y;

        pmResult.setLon(Math.min(maxLong, Math.max(minLong, resLong)));
        pmResult.setLat(Math.min(maxLat, Math.max(minLat, resLat)));

        if ((v1x == 0) && (v1y == 0)){
            pmResult.setEle(eleEnd1);
            pmResult.setTimestamp(tsEnd1);
        } else if (Math.abs(v1x) > Math.abs(v1y)) { // interpolate based on longitude
            pmResult.setEle( (float) interpolate(lonEnd1, lonEnd2, eleEnd1, eleEnd2, pmResult.getLon()) );
            pmResult.setTimestamp( (long) interpolate(lonEnd1, lonEnd2, tsEnd1, tsEnd2, pmResult.getLon()) );
        } else { // interpolate based on latitude
            pmResult.setEle( (float) interpolate(latEnd1, latEnd2, eleEnd1, eleEnd2, pmResult.getLat()) );
            pmResult.setTimestamp( (long) interpolate(latEnd1, latEnd2, tsEnd1, tsEnd2, pmResult.getLat()) );
        }
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
        if ((pm1 == null) && (pm2 == null)) return 0;
        if (pm1 == null) return -1;
        if (pm2 == null) return 1;
        return compareTo(pm1.getLat(), pm1.getLon(), pm2.getLat(), pm2.getLon() );
    }


    public static int compareTo(double lat1, double long1, double lat2, double long2){
        if (lat1 < lat2) return -1;
        if (lat1 > lat2) return  1;
        return Double.compare(long1, long2);
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
        return new PointModelImpl(lat,lon);
    }

    public static double interpolate(double refMin, double refMax, double valMin, double valMax, double ref){
        double scale = (ref - refMin) / (refMax - refMin);
        return scale * (valMax - valMin) + valMin ;
    }

    public static void interpolateELe(PointModel pm1, PointModel pm2, WriteablePointModel wpm){
        if (compareTo(pm1,pm2) == 0){
            wpm.setEle(pm1.getEle());
        } else {
            if (Math.abs(pm2.getLon() - pm1.getLon()) > Math.abs(pm2.getLat() - pm1.getLat())){ // interpolate based on longitude
                wpm.setEle( (float) interpolate(pm1.getLon(), pm2.getLon(), pm1.getEle(), pm2.getEle(), wpm.getLon()) );
            } else { // interpolate based on latitude
                wpm.setEle( (float) interpolate(pm1.getLat(), pm2.getLat(), pm1.getEle(), pm2.getEle(), wpm.getLat()) );
            }
        }
    }

    public static void  interpolateTimestamp(PointModel pm1, PointModel pm2, WriteablePointModel wpm) {
        if ((pm1.getTimestamp() == PointModel.NO_TIME) || (pm1.getTimestamp() == PointModel.NO_TIME)){
            wpm.setTimestamp(PointModel.NO_TIME);
        } else {
            if (Math.abs(pm2.getLon() - pm1.getLon()) > Math.abs(pm2.getLat() - pm1.getLat())){ // interpolate based on longitude
                wpm.setTimestamp( (long) interpolate(pm1.getLon(), pm2.getLon(), pm1.getTimestamp(), pm2.getTimestamp(), wpm.getLon()) );
            } else { // interpolate based on latitude
                wpm.setTimestamp( (long) interpolate(pm1.getLat(), pm2.getLat(), pm1.getTimestamp(), pm2.getTimestamp(), wpm.getLat()) );
            }
        }
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
            getBestDistance(mpms.get(segmentIdx), segmentIdx, pm, bestMatch, pmApproach, pmApproachCandidate);
        }
    }

    public static void getBestDistance(MultiPointModel segment, PointModel pm, TrackLogRefApproach bestMatch){
        getBestDistance(segment, -1, pm, bestMatch, new WriteablePointModelImpl(), new WriteablePointModelImpl());
    }

    // speedup based on meta data would be possible for TrackLogSegments (not for routes), but speed improvement is just a few ms and blows up the code - therefore this attempt is discarded
    private static void getBestDistance(MultiPointModel segment, int segmentIdx, PointModel pm, TrackLogRefApproach bestMatch, WriteablePointModel pmApproach, WriteablePointModel pmApproachCandidate){
        for (int i = 1, j = 0; i < segment.size(); j = i++) {
            if (PointModelUtil.findApproach(pm, segment.get(i), segment.get(j), pmApproachCandidate, (int)(bestMatch.getDistance()+1) )){
                double distance = PointModelUtil.distance( pm, pmApproachCandidate);
                if (distance < bestMatch.getDistance()){
                    bestMatch.setSegmentIdx( segmentIdx );
                    if (bestMatch.getApproachPoint() == null) bestMatch.setApproachPoint(pmApproach);
                    pmApproach.setLat(pmApproachCandidate.getLat());
                    pmApproach.setLon(pmApproachCandidate.getLon());
                    pmApproach.setEle(pmApproachCandidate.getEle());
                    pmApproach.setTimestamp(pmApproachCandidate.getTimestamp());
                    bestMatch.setDistance( distance );
                    bestMatch.setEndPointIndex(i);
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


    private final static int shift = 1000;
    public static int getLower(double d) {
        return ((int)(d+shift) - shift);
    }

    public static double distance(ArrayList<PointModel> points){
        PointModel lastPoint = null;
        double distance = 0;
        for (PointModel point : points){
            distance += distance(lastPoint,point);
            lastPoint = point;
        }
        return distance;
    }
    public static void getHeightList(ArrayList<PointModel> points, double dist, float[] heights){
        assert (heights.length >= 2);
        dist *=  0.999999;
        PointModel lastPoint = null;
        int hIdx = 0;
        double remainingDist = dist * 0.999999;
        for (PointModel point : points){
            double pointDist = distance(lastPoint,point);
            double distFromLastPoint = -remainingDist;
            while ((pointDist >= distFromLastPoint + dist) && (lastPoint != null) && (point != null)){
                distFromLastPoint += dist;
                heights[hIdx++] = (float)interpolate(0, pointDist,lastPoint.getEle(),point.getEle(),distFromLastPoint);
                if (hIdx == heights.length) return;
            }
            remainingDist = pointDist-distFromLastPoint;
            lastPoint = point;
        }
    }


    public static float verticalDistance(TrackLogPoint pm1, TrackLogPoint pm2){
        if ((pm1.getPressureEle() != PointModel.NO_ELE) && (pm2.getPressureEle() != PointModel.NO_ELE)){
            return pm2.getPressureEle() - pm1.getPressureEle();
        }
        if ((pm1.getHgtEle() != PointModel.NO_ELE) && (pm2.getHgtEle() != PointModel.NO_ELE)){
            return pm2.getHgtEle() - pm1.getHgtEle();
        }
        if ((pm1.getNmeaEle() != PointModel.NO_ELE) && (pm2.getNmeaEle() != PointModel.NO_ELE)){
            return pm2.getNmeaEle() - pm1.getNmeaEle();
        }
        if ((pm1.getEle() != PointModel.NO_ELE) && (pm2.getEle() != PointModel.NO_ELE)){
            return pm2.getEle() - pm1.getEle();
        }
        mgLog.w("no valid ele comparison: pm1: "+pm1+ " pm2: "+pm2);
        return 0; //
    }


    public static float verticalDistance(PointModel pm1, PointModel pm2){
        if ((pm1 instanceof TrackLogPoint) && (pm2 instanceof TrackLogPoint)) return verticalDistance((TrackLogPoint)pm1, (TrackLogPoint)pm2 );
        if ((pm1.getEle() == PointModel.NO_ELE) || (pm2.getEle() == PointModel.NO_ELE)) return 0;
        return pm2.getEle() - pm1.getEle();
    }

    /**
     *
     * @param pm1 start first line
     * @param pm2 end first line
     * @param pm3 start second line
     * @param pm4 end second line
     * @param threshold threshold to consider a point close to another line
     * @return true, if second line is basically an overlapping continuation of first line
     *
     */
    public static boolean isOverlappingLine(PointModel pm1, PointModel pm2, PointModel pm3, PointModel pm4, int threshold){
        WriteablePointModel wpm = new WriteablePointModelImpl();
        return (findApproach(pm3, pm1, pm2, wpm, threshold) &&
                findApproach(pm2, pm3, pm4, wpm, threshold) &&
                findApproach(pm2, pm1, pm4, wpm, threshold) &&
                findApproach(pm3, pm1, pm4, wpm, threshold));
    }

    /**
     * Adopted from <a href="http://alienryderflex.com/polygon/">http://alienryderflex.com/polygon/</a>
     * @param pm point to check
     * @param mpm polygon
     * @return true, if point is inside the polygon
     */
    public static boolean pointInPolygon(PointModel pm, MultiPointModel mpm) {
        int i, j= mpm.size()-1 ;
        boolean oddNodes = false;

        for (i=0; i<mpm.size(); i++) {
            if (( (mpm.get(i).getLat() < pm.getLat()) && (mpm.get(j).getLat() >= pm.getLat())
                    ||   (mpm.get(j).getLat() < pm.getLat()) && (mpm.get(i).getLat() >= pm.getLat()))
                    &&  (mpm.get(i).getLon()<=pm.getLon() || mpm.get(j).getLon()<=pm.getLon())) {
                oddNodes^=(mpm.get(i).getLon()+(pm.getLat()-mpm.get(i).getLat())/(mpm.get(j).getLat()-mpm.get(i).getLat())*(mpm.get(j).getLon()-mpm.get(i).getLon())<pm.getLon());
            }
            j=i;
        }
        return oddNodes;
    }
}
