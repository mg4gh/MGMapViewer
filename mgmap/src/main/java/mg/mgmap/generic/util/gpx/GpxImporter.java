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

import org.kxml2.io.KXmlParser;

import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogPoint;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.basic.MGLog;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.Objects;

/**
 * Provides the ability to import a TrackLog from a GPX file.
 */

public class GpxImporter {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final ElevationProvider elevationProvider;
    private final XmlPullParser pullParser = new KXmlParser();

    public GpxImporter(ElevationProvider elevationProvider){
        this.elevationProvider = elevationProvider;
    }


    private String getStringAttribute(String name) {
        int n = pullParser.getAttributeCount();
        for (int i = 0; i < n; i++) {
            if (pullParser.getAttributeName(i).equals(name)) {
                return pullParser.getAttributeValue(i);
            }
        }
        return null;
    }

    public TrackLog parseTrackLog(String filename, InputStream inputStream) throws Exception{
        WriteableTrackLog trackLog = null;
        TrackLogPoint tlp = null;
        String text = null;
        String qName;
        boolean meta = false;
        WriteableTrackLog referencedTrackLog = null;

        pullParser.setInput(inputStream, null);

        int eventType = pullParser.getEventType();
        do {
            qName = pullParser.getName();
            if (eventType == XmlPullParser.START_DOCUMENT) {
                trackLog = new WriteableTrackLog(filename);
            } else if (eventType == XmlPullParser.START_TAG) {
                if ("trk".equals(qName)) {
                    assert trackLog != null;
                    trackLog.startTrack(PointModel.NO_TIME);
                }
                if ("metadata".equals(qName)) {
                    meta = true;
                }
                if ("trkseg".equals(qName)) {
                    assert trackLog != null;
                    trackLog.startSegment(PointModel.NO_TIME);
                }
                if ("trkpt".equals(qName)) {
                    double lat = Double.parseDouble(Objects.requireNonNull(getStringAttribute("lat")));
                    double lon = Double.parseDouble(Objects.requireNonNull(getStringAttribute("lon")));
                    tlp = TrackLogPoint.createLogPoint(lat, lon);
                }
                if ("wpt".equals(qName)) {
                    double lat = Double.parseDouble(Objects.requireNonNull(getStringAttribute("lat")));
                    double lon = Double.parseDouble(Objects.requireNonNull(getStringAttribute("lon")));
                    tlp = TrackLogPoint.createLogPoint(lat, lon);
                    elevationProvider.setElevation(tlp); // try to enrich data with hgt height information
                    if (referencedTrackLog != null){
                        referencedTrackLog.getTrackLogSegment(0).addPoint(tlp);
                    }
                }
                if ("routingProfile".equals(qName)) {
                    String rp = getStringAttribute("id");
                    if ((trackLog != null) && (trackLog.getReferencedTrackLog() != null) && (rp != null)){
                        trackLog.getReferencedTrackLog().setRoutingProfileId(rp);
                    }
                }

                text = null;

            } else if (eventType == XmlPullParser.END_TAG) {
                if ("trkpt".equals(qName)) {
                    assert tlp != null;
                    if (tlp.getEle() == PointModel.NO_ELE){
                        elevationProvider.setElevation(tlp); // try to enrich data with hgt height information
                    }
                    assert trackLog != null;
                    trackLog.addPoint( tlp );
                }
                if ("trk".equals(qName)) {
                    assert trackLog != null;
                    trackLog.stopTrack(PointModel.NO_TIME);
                }
                if ("metadata".equals(qName)) {
                    meta = false;
                }
                if ("trkseg".equals(qName)) {
                    assert trackLog != null;
                    trackLog.stopSegment(PointModel.NO_TIME);
                }
                if ("keywords".equals(qName)) {
                    if ("MGMarkerRoute".equals(text)){
                        String name = filename.replaceAll("MarkerRoute$","MarkerTrack");
                        referencedTrackLog = new WriteableTrackLog(name);
                        referencedTrackLog.getTrackLogSegments().add(new TrackLogSegment(0));
                        assert trackLog != null;
                        trackLog.setReferencedTrackLog(referencedTrackLog);
                        referencedTrackLog.getTrackStatistic().setTStart(trackLog.getTrackStatistic().getTStart());
                    }
                }
                if ("ele".equals(qName)) {
                    try{
                        assert tlp != null;
                        assert text != null;
                        tlp.setEle( Float.parseFloat(text) );
                    }catch(Exception e){
                        mgLog.w("Parse elevation failed: "+e.getMessage());
                    }
                }
                if ("time".equals(qName)) {
                    try {
                        if (tlp != null){
                            assert text != null;
                            tlp.setTimestamp( Objects.requireNonNull(Formatter.SDF_GPX.parse(text.replaceAll("T", "_"))).getTime() );
                        }
                        if (meta){
                            assert trackLog != null;
                            assert text != null;
                            trackLog.getTrackStatistic().setTStart( Objects.requireNonNull(Formatter.SDF_GPX.parse(text.replaceAll("T", "_"))).getTime() );
                        }
                    } catch (Exception e) {
                        mgLog.w("Parse time failed: "+e.getMessage());
                    }
                }
                if ("cmt".equals(qName)) {
                    try {
                        assert text != null;
                        for (String part : text.split(",")){
                            String[] val = part.split("=");
                            if (val.length == 2){
                                assert tlp != null;
                                switch (val[0]) {
                                    case "wgs84ele":
                                    case "wgs84altitude":
                                        tlp.setWgs84ele(Float.parseFloat(val[1]));
                                        break;
                                    case "nmeaEle":
                                    case "nmeaAltitude":
                                        tlp.setNmeaEle(Float.parseFloat(val[1]));
                                        break;
                                    case "nmeaAcc":
                                    case "accuracy":
                                        tlp.setNmeaAcc(Float.parseFloat(val[1]));
                                        break;
                                    case "pressure":
                                        tlp.setPressure(Float.parseFloat(val[1]));
                                        break;
                                    case "pressureEle":
                                    case "presureAltitude":
                                        tlp.setPressureEle(Float.parseFloat(val[1]));
                                        break;
                                    case "hgtEle":
                                    case "hgtAltitude":
                                        tlp.setHgtEle(Float.parseFloat(val[1]));
                                        break;
                                    case "hgtEleAcc":
                                    case "hgtAltAcc":
                                        tlp.setHgtEleAcc(Float.parseFloat(val[1]));
                                        break;
                                    case "nmeaEleAcc":
                                    case "altAccuracy":
                                        tlp.setNmeaEleAcc(Float.parseFloat(val[1]));
                                        break;
                                    case "pressureEleAcc":
                                    case "pressureAltAccuracy":
                                        tlp.setPressureEleAcc(Float.parseFloat(val[1]));
                                        break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        mgLog.w("Parse comment failed: "+e.getMessage());
                    }
                }

            } else if (eventType == XmlPullParser.TEXT) {
                text = (text==null)?pullParser.getText():text+pullParser.getText();
            }
            eventType = pullParser.next();
        } while (eventType != XmlPullParser.END_DOCUMENT);
        if (trackLog != null){
            if (trackLog.getNumberOfSegments() == 0){
                trackLog = null;
            } else {
                trackLog.setModified(true);
            }
        }
        return trackLog;
    }

}

