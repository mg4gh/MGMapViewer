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
package mg.mgmap.generic.util.gpx;

import mg.mgmap.application.util.PersistenceManager;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.basic.MGLog;

import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.Locale;

/**
 * Provides the ability to export a TrackLog to a GPX file.
 */

public class GpxExporter {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static void export(PersistenceManager persistenceManager, TrackLog trackLog) {
        if (trackLog.getNumberOfSegments() > 0) {
            try (PrintWriter pw = persistenceManager.openGpxOutput(trackLog.getName())){
                export(pw, trackLog);
            } catch (Exception e) {
                mgLog.e(e);
            }
        }
    }

    public static void export(PrintWriter pw, TrackLog trackLog) {
        if (pw != null){
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<gpx version=\"1.1\" creator=\"MGMap\">");
            pw.println("\t<metadata>");
            pw.println("\t\t<time>"+ Formatter.SDF_GPX.format(new Date(trackLog.getTrackStatistic().getTStart())).replace("_","T")+"</time>");
            TrackLog refTL = trackLog.getReferencedTrackLog();
            if ((refTL != null) && (refTL.getNumberOfSegments() == 1)){ // supposed to be a route - export also the base MarkerTrackPoints
                pw.println("\t\t<keywords>MGMarkerRoute</keywords>");
                if (refTL.getRoutingProfileId() != null){
                    pw.println("\t\t<extensions>");
                    pw.println("\t\t\t<routingProfile id=\""+refTL.getRoutingProfileId()+"\" />");
                    pw.println("\t\t</extensions>");
                }
                pw.println("\t</metadata>");
                TrackLogSegment segment = refTL.getTrackLogSegment(0);
                for (PointModel pm : segment){
                    String sLat = " lat=\"" + String.format(Locale.ENGLISH,"%.6f",pm.getLat()) + "\"";
                    String sLon = " lon=\"" + String.format(Locale.ENGLISH,"%.6f",pm.getLon()) + "\"";
                    pw.println("\t<wpt " + sLat + sLon + " />");
                }
            } else {
                pw.println("\t</metadata>");
            }
            pw.println("\t<trk>");
            pw.println("\t\t<name>"+trackLog.getName()+"</name>");
            pw.println("\t\t<desc>"+trackLog.getTrackStatistic().toString()+"</desc>");

            for (TrackLogSegment segment : trackLog.getTrackLogSegments()){
                pw.println("\t\t<trkseg>");

                pw.println("\t\t\t<desc>"+segment.getStatistic().toString()+"</desc>");
                for (PointModel pm : segment){
                    String sLat = " lat=\"" + String.format(Locale.ENGLISH,"%.6f",pm.getLat()) + "\"";
                    String sLon = " lon=\"" + String.format(Locale.ENGLISH,"%.6f",pm.getLon()) + "\"";
                    pw.println("\t\t\t<trkpt " + sLat + sLon + ">");
                    float ele = pm.getEle();
                    if (ele != PointModel.NO_ELE) {
                        pw.println("\t\t\t\t<ele>" + String.format(Locale.ENGLISH,"%.1f",ele) + "</ele>");
                    }
                    if (pm.getTimestamp() != PointModel.NO_TIME){
                        pw.println("\t\t\t\t<time>" + Formatter.SDF_GPX.format(new Date(pm.getTimestamp())).replace("_","T") + "</time>");
                        if (pm instanceof TrackLogPoint lp) {
                            String cmt = "";
                            cmt += "nmeaAcc=" + String.format(Locale.ENGLISH,"%.1f",lp.getNmeaAcc()) + ",";
                            if (lp.getWgs84ele() != TrackLogPoint.NO_ELE) {
                                cmt += "wgs84ele=" + String.format(Locale.ENGLISH,"%.1f",lp.getWgs84ele()) + ",";
                            }
                            if (lp.getNmeaEle() != TrackLogPoint.NO_ELE) {
                                cmt += "nmeaEle=" + String.format(Locale.ENGLISH,"%.1f",lp.getNmeaEle()) + ",";
                            }
                            if (lp.getNmeaEleAcc() != PointModel.NO_ACC){
                                cmt += "nmeaEleAcc=" + String.format(Locale.ENGLISH,"%.1f",lp.getNmeaEleAcc() ) + "," ;
                            }
                            if (lp.getPressure() != PointModel.NO_PRES){
                                cmt += "pressure=" + String.format(Locale.ENGLISH,"%.3f",lp.getPressure()) + ",";
                                cmt += "pressureEle=" + String.format(Locale.ENGLISH,"%.1f",lp.getPressureEle() ) + "," ;
                            }
                            if (lp.getHgtEle() != TrackLogPoint.NO_ELE){
                                cmt += "hgtEle=" + String.format(Locale.ENGLISH,"%.1f",lp.getHgtEle() ) + "," ;
                            }
                            if (lp.getHgtEleAcc() != PointModel.NO_ACC){
                                cmt += "hgtEleAcc=" + String.format(Locale.ENGLISH,"%.1f",lp.getHgtEleAcc() ) + "," ;
                            }
                            if (lp.getPressureEleAcc() != PointModel.NO_ACC){
                                cmt += "pressureEleAcc=" + String.format(Locale.ENGLISH,"%.1f",lp.getPressureEleAcc() ) + "," ;
                            }
                            pw.println("\t\t\t\t<cmt>" + cmt.substring(0,cmt.length()-1) + "</cmt>");
                        }
                    }
                    pw.println("\t\t\t</trkpt>");

                }
                pw.println("\t\t</trkseg>");
            }
            pw.println("\t</trk>");
            pw.println("</gpx>");
            trackLog.setModified(false);
        }
    }
//        } catch (Exception e) {
//            mgLog.e(e);
//        } finally {
//            if (pw != null){
//                pw.close();
//            }
//        }
//


}
