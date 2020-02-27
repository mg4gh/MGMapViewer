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
package mg.mapviewer.features.rtl;

import android.hardware.SensorManager;
import android.util.Log;

import mg.mapviewer.model.WriteableTrackLog;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLogPoint;
import mg.mapviewer.model.TrackLogSegment;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PersistenceManager;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Record a new TrackLog. This includes the option of an immediate storage of all new TrackLogPoint objects.
 * Created by Martin on 02.08.2017.
 */
public class RecordingTrackLog extends WriteableTrackLog {

    private static final String TAG = NameUtil.getTag();
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMANY);

    private static PersistenceManager persistenceManager = PersistenceManager.getInstance();

    private boolean isTrackRecording = false;
    private boolean isSegmentRecording = false;
    private boolean recordRaw;
    private boolean reworkData = true;

    private enum RawType { E_START_TRACK, E_START_SEGMENT, E_POINT, E_END_SEGMENT, E_END_TRACK }
    private static RawType fromOrdinal(byte ordinal){
        return RawType.values()[ordinal];
    }


    public boolean isTrackRecording() {
        return isTrackRecording;
    }
    public boolean isSegmentRecording() {
        return isSegmentRecording;
    }
    public TrackLogSegment getCurrentSegment() {
        return currentSegment;
    }
    public void setRecordRaw(boolean recordRaw){
        this.recordRaw = recordRaw;
    }
    public void setReworkData(boolean reworkData){
        this.reworkData = reworkData;
    }

    private static void createRawEntry(RawType type, long time, PointModel pointModel){
        ByteBuffer buf = ByteBuffer.allocate(256);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.put((byte) 0); // put length later at offset 0
        buf.put((byte) type.ordinal());
        if (pointModel == null) {
            buf.putLong(time);
        } else {
            if (pointModel instanceof TrackLogPoint) {
                TrackLogPoint trackLogPoint = (TrackLogPoint) pointModel;
                trackLogPoint.toByteBuffer(buf);
            }
        }
        int length = buf.position();
        buf.position(0);
        buf.put((byte)length);
        persistenceManager.recordRaw(buf.array(),0,length);
    }

    public static RecordingTrackLog initFromRaw(){
        byte[] b = persistenceManager.getRawData();
        if (b == null){
            persistenceManager.clearRaw();
        } else {
            try {
                ByteBuffer buf = ByteBuffer.wrap(b);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                RecordingTrackLog rtl = new RecordingTrackLog(false);
                while (buf.remaining() > 0){
                    byte length = buf.get(); // read length
                    if ((length >= 0) && (length < 10)){
                        break;
                    }
                    switch (fromOrdinal( buf.get() )){
                        case E_START_TRACK:
                            if (length == 10){
                                rtl.startTrack(buf.getLong());
                            }
                            break;
                        case E_START_SEGMENT:
                            rtl.startSegment(buf.getLong());
                            break;
                        case E_END_SEGMENT:
                            rtl.stopSegment(buf.getLong());
                            break;
                        case E_END_TRACK:
                            rtl.stopTrack(buf.getLong());
                            break;
                        case E_POINT:
                            TrackLogPoint lp = new TrackLogPoint();
                            lp.fromByteBuffer(buf);
                            rtl.addPoint(lp);
                            break;
                        default:
                            throw new RuntimeException("should never happen");
                    }
                }
                return rtl;
            } catch (Exception e){
                persistenceManager.clearRaw();
                throw e;
            }
        }
        return null;
    }




    public RecordingTrackLog(boolean recordRaw){
        this.recordRaw = recordRaw;
    }


    public void startTrack(long tStartTrack) {
        isTrackRecording = true;

        if (recordRaw) createRawEntry(RawType.E_START_TRACK, tStartTrack, null);
        if (name.equals("")) name = sdf.format(tStartTrack)+"_GPS";
        changed(null);
    }

    public void startSegment(long tStartSegment) {
        isSegmentRecording = true;
        super.startSegment(tStartSegment);
        if (recordRaw) createRawEntry(RawType.E_START_SEGMENT, tStartSegment, null);
        changed(null);
    }

    public void stopSegment(long tStopSegment) {
        isSegmentRecording = false;
        if (recordRaw) createRawEntry(RawType.E_END_SEGMENT, tStopSegment, null);

        reworkData(currentSegment);
        super.stopSegment(tStopSegment);
        changed(null);
    }
    public void stopTrack(long tStopTrack) {
        isTrackRecording = false;
        if (recordRaw) createRawEntry(RawType.E_END_TRACK, tStopTrack, null);
        super.stopTrack(tStopTrack); // rework tStart and duration i TrackLogStatistic
        if (recordRaw) persistenceManager.clearRaw();
        changed(null);
    }


    public void addPoint(PointModel lp){
        if (isSegmentRecording) {
            super.addPoint(lp);
            if (recordRaw) createRawEntry(RawType.E_POINT, lp.getTimestamp(), lp);
            changed(null);
        }
    }



    private void reworkData(TrackLogSegment segment) {
        if (reworkData){
            ArrayList<TrackLogPoint> listAll = new ArrayList<>();
            for (PointModel pm : segment){
                if (pm instanceof TrackLogPoint) {
                    TrackLogPoint tlp = (TrackLogPoint) pm;
                    if (tlp.getWgs84alt() != 0){
                        listAll.add(tlp);
                    }
                }
            }
            if (listAll.isEmpty()) return; // no rework possible
            ArrayList<TrackLogPoint> listAll2 = new ArrayList<>(listAll);
            ArrayList<TrackLogPoint> listWithoutPressure = new ArrayList<>();

            for (int i=0; (i<listAll.size()) && (i<2); i++){ // don't trust frist two pressure values
                listAll.get(i).setPressure(PointModel.NO_PRES);
            }

            TrackLogPoint firstPressurePoint = null;
            for (TrackLogPoint tlp : listAll) {
                if (tlp.getPressure() == PointModel.NO_PRES) {
                    listWithoutPressure.add(tlp);
                } else {
                    firstPressurePoint = tlp;
                    break;
                }
            }
            listAll.removeAll(listWithoutPressure); // don't use these points for calibration data


            if (firstPressurePoint != null) {
                for (TrackLogPoint tlp : listWithoutPressure) {
                    tlp.setPressure( firstPressurePoint.getPressure() );
                    tlp.setPressureAlt( firstPressurePoint.getPressureAlt() );
                }

                TrackLogPoint refLp = listAll.get(0);
                float refAlt = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, refLp.getPressure());

                int cntAlt = 0;
                float sumAlt = 0;

                for (TrackLogPoint lp : listAll) {
                    float curAlt = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, lp.getPressure());
                    float curNmea = lp.getNmeaAlt() - (curAlt - refAlt);

                    cntAlt++;
                    sumAlt += curNmea;

                    if (cntAlt > 30) break; // take only the first 30 points for calibration
                }

                float calAlt = sumAlt / cntAlt;
                Log.i(TAG, "reworkData: "+ String.format(Locale.ENGLISH, "Calibration data: pressure=%.3f refAlt=%.1f calAlt=%.1f", refLp.getPressure() , refAlt, calAlt));

                float maxEle = PointModel.NO_ELE;
                float minEle = -PointModel.NO_ELE;
                for (TrackLogPoint lp : listAll2) { // now take all points from segment
                    if (lp.getPressure() != PointModel.NO_PRES) {
                        float lpEle = calAlt + (lp.getPressureAlt() - refAlt) ;
                        lp.setEle( lpEle ); // set calibratedAlt
                        maxEle = Math.max(maxEle, lpEle);
                        minEle = Math.min(minEle, lpEle);
                    }
                }
                if (maxEle != PointModel.NO_ELE){
                    segment.getStatistic().setMaxEle(maxEle);
                    segment.getStatistic().setMinEle(minEle);
                }
            }
        }
    }

}
