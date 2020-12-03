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

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import org.kxml2.io.KXmlParser;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLogRef;
import mg.mapviewer.model.TrackLogRefZoom;
import mg.mapviewer.model.WriteableTrackLog;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogPoint;

import org.mapsforge.core.model.LatLong;
import org.xmlpull.v1.XmlPullParser;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Provides the ability to import a TrackLog from a GPX file.
 */

public class GpxImporter {

    private static SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.GERMANY);

    public static TrackLog checkLoadGpx(MGMapApplication application, Uri uri) {
        try {
            InputStream is;
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                is = application.getContentResolver().openInputStream(uri);
                if (is == null) return null;
            } else {
                String filePath = uri.getEncodedPath();
                if (filePath == null) return null;
                is = new FileInputStream(filePath);
            }
            return new GpxImporter().parseTrackLog("GPX" + System.currentTimeMillis(), is);
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" Track loaded for " + uri);
        return null;
    }



    private XmlPullParser pullParser = new KXmlParser();


    private String getStringAttribute(String name) {
        int n = pullParser.getAttributeCount();
        for (int i = 0; i < n; i++) {
            if (pullParser.getAttributeName(i).equals(name)) {
                return pullParser.getAttributeValue(i);
            }
        }
        return null;
    }

    @SuppressWarnings({"ConstantConditions"})
    public TrackLog parseTrackLog(String filename, InputStream inputStream) throws Exception{
        WriteableTrackLog trackLog = null;
        TrackLogPoint tlp = null;
        String text = null;
        String qName;
        boolean meta = false;

        pullParser.setInput(inputStream, null);

        int eventType = pullParser.getEventType();
        do {
            qName = pullParser.getName();
            if (eventType == XmlPullParser.START_DOCUMENT) {
                trackLog = new WriteableTrackLog(filename);
            } else if (eventType == XmlPullParser.START_TAG) {
                if ("trk".equals(qName)) {
                    trackLog.startTrack(0);
                }
                if ("metadata".equals(qName)) {
                    meta = true;
                }
                if ("trkseg".equals(qName)) {
                    trackLog.startSegment(0);
                }
                if ("trkpt".equals(qName)) {
                    double lat = Double.parseDouble(getStringAttribute("lat"));
                    double lon = Double.parseDouble(getStringAttribute("lon"));
                    tlp = TrackLogPoint.createLogPoint(lat, lon);
                }
                text = null;

            } else if (eventType == XmlPullParser.END_TAG) {
                if ("trkpt".equals(qName)) {
                    if (tlp.getEleD() == PointModel.NO_ELE){
                        tlp.setEle(AltitudeProvider.getAlt(tlp)); // try to enrich data with hgt height information
                    }
                    trackLog.addPoint( tlp );
                }
                if ("trk".equals(qName)) {
                    trackLog.stopTrack(0);
                }
                if ("metadata".equals(qName)) {
                    meta = false;
                }
                if ("trkseg".equals(qName)) {
                    trackLog.stopSegment(0);
                }
                if ("name".equals(qName)) {
                    trackLog.setName(text);
                }
                if ("ele".equals(qName)) {
                    try{
                        tlp.setEle( Float.parseFloat(text) );
                    }catch(Exception e){
                        Log.w(MGMapApplication.LABEL, NameUtil.context()+" Parse elevation failed: "+e.getMessage());
                    }
                }
                if ("time".equals(qName)) {
                    try {
                        if (tlp != null){
                            tlp.setTimestamp( sdf2.parse(text.replaceAll("T","_")).getTime() );
                        }
                        if (meta){
                            trackLog.getTrackStatistic().setTStart( sdf2.parse(text.replaceAll("T","_")).getTime() );
                        }
                    } catch (Exception e) {
                        Log.w(MGMapApplication.LABEL, NameUtil.context()+" Parse time failed: "+e.getMessage());
                    }
                }
                if ("cmt".equals(qName)) {
                    try {
                        for (String part : text.split(",")){
                            String[] val = part.split("=");
                            if (val.length == 2){
                                if (val[0].equals("wgs84altitude")){
                                    tlp.setWgs84alt( Float.parseFloat(val[1]) );
                                }
                                if (val[0].equals("nmeaAltitude")){
                                    tlp.setNmeaAlt( Float.parseFloat(val[1]) );
                                }
                                if (val[0].equals("accuracy")){
                                    tlp.setAccuracy( Float.parseFloat(val[1]) );
                                }
                                if (val[0].equals("pressure")){
                                    tlp.setPressure( Float.parseFloat(val[1]) );
                                }
                                if (val[0].equals("presureAltitude")){
                                    tlp.setPressureAlt( Float.parseFloat(val[1]) );
                                }
                                if (val[0].equals("hgtAltitude")){
                                    tlp.setHgtAlt( Float.parseFloat(val[1]) );
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.w(MGMapApplication.LABEL, NameUtil.context()+" Parse comment failed: "+e.getMessage());
                    }
                }

            } else if (eventType == XmlPullParser.TEXT) {
                text = (text==null)?pullParser.getText():text+pullParser.getText();
            }
            eventType = pullParser.next();
        } while (eventType != XmlPullParser.END_DOCUMENT);
        return trackLog;
    }

}

