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

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLogSegment;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogPoint;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Provides the ability to export a TrackLog to a GPX file.
 */

public class GpxExporter {

    private static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.GERMANY);

    public static void export(TrackLog trackLog) {
        PrintWriter pw = null;
        try {
            if (trackLog.getNumberOfSegments() > 0) {
                pw = PersistenceManager.getInstance().openGpxOutput(trackLog.getName());
                if (pw != null){
                    pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                    pw.println("<gpx version=\"1.1\" creator=\"MGMap\">");
                    pw.println("\t<metadata>");
                    pw.println("\t\t<name>"+trackLog.getName()+"</name>");
                    pw.println("\t\t<time>"+sdf2.format(new Date(trackLog.getTrackStatistic().getTStart())).replace("_","T")+"</time>");
                    pw.println("\t</metadata>");
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
                            float ele = pm.getEleA();
                            if (ele != PointModel.NO_ELE) {
                                pw.println("\t\t\t\t<ele>" + String.format(Locale.ENGLISH,"%.1f",ele) + "</ele>");
                            }
                            if (pm.getTimestamp() != PointModel.NO_TIME){
                                pw.println("\t\t\t\t<time>" + sdf2.format(new Date(pm.getTimestamp())).replace("_","T") + "</time>");
                                if (pm instanceof TrackLogPoint) {
                                    TrackLogPoint lp = (TrackLogPoint) pm;
                                    String cmt = "";
                                    cmt += "wgs84altitude=" + String.format(Locale.ENGLISH,"%.1f",lp.getWgs84alt()) + ",";
                                    cmt += "nmeaAltitude=" + String.format(Locale.ENGLISH,"%.1f",lp.getNmeaAlt()) + ",";
                                    cmt += "accuracy=" + String.format(Locale.ENGLISH,"%.1f",lp.getAccuracy()) + ",";
                                    if (lp.getPressure() > Integer.MIN_VALUE){
                                        cmt += "pressure=" + String.format(Locale.ENGLISH,"%.3f",lp.getPressure()) + ",";
                                        cmt += "presureAltitude=" + String.format(Locale.ENGLISH,"%.1f",lp.getPressureAlt() ) + "," ;
                                    }
                                    if (lp.getHgtAlt() != TrackLogPoint.NO_ELE){
                                        cmt += "hgtAltitude=" + String.format(Locale.ENGLISH,"%.1f",lp.getHgtAlt() ) + "," ;
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
                }
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        } finally {
            if (pw != null){
                pw.close();
            }
        }

    }

}
