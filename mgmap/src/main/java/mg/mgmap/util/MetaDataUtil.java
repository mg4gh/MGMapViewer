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
package mg.mgmap.util;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import mg.mgmap.MGMapApplication;
import mg.mgmap.model.BBox;
import mg.mgmap.model.MetaData;
import mg.mgmap.model.PointModel;
import mg.mgmap.model.PointModelImpl;
import mg.mgmap.model.TrackLog;
import mg.mgmap.model.TrackLogSegment;
import mg.mgmap.model.TrackLogStatistic;

public class MetaDataUtil {

    public static final long MAGIC = 0xAFFEAFFED00FD00Fl;
    public static final int VERSION = 0x0200;


    public static void createMetaData(TrackLog trackLog){
        for (int idx=0; idx<trackLog.getNumberOfSegments(); idx++){
            TrackLogSegment segment = trackLog.getTrackLogSegment(idx);
            ArrayList<MetaData> metaDatas = segment.getMetaDatas();

            for (int pIdx=0; pIdx<segment.size(); pIdx++){
                int mIdx = pIdx / MetaData.POINTS_PER_BUF;
                if (mIdx >= metaDatas.size()){                             // if we need next metaData entry
                    metaDatas.add(new MetaData());                        // then create it
                    MetaData metaData = metaDatas.get(mIdx);
                    metaData.buf = ByteBuffer.allocate(MetaData.BUF_SIZE);// allocate the byte buffer
                    metaData.buf.order(ByteOrder.LITTLE_ENDIAN);
                    metaData.buf.putLong(MAGIC);                           // and start with MAGIC
                    metaData.buf.putShort((short)idx);                     // segment index
                    metaData.buf.putShort((short)mIdx);                    // and metaDate per segment index
                }
                MetaData metaData = metaDatas.get(mIdx);

                PointModel pm = segment.get(pIdx);
                if (pm instanceof PointModelImpl) {
                    PointModelImpl pmi = (PointModelImpl) pm;
                    metaData.bBox.extend(pmi);
                    pmi.toBuf(metaData.buf);
                    metaData.numPoints++;
                }
            }
        }
    }

    public static void writeMetaData(FileOutputStream fos, TrackLog trackLog){
        try {
            ByteBuffer buf = ByteBuffer.allocate(MetaData.BUF_SIZE);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            int fosSize = 0;

            buf.rewind(); // set back to pos 0
            buf.putLong(MAGIC-1); // -1 to differentiate from metaDataBlocks
            buf.putInt(VERSION);
            trackLog.getTrackStatistic().toByteBuffer(buf);
            buf.putInt(trackLog.getNumberOfSegments());
            fos.write(buf.array(), 0, buf.position());
            fosSize += buf.position();


            for (int idx=0; idx<trackLog.getNumberOfSegments(); idx++){
                TrackLogSegment segment = trackLog.getTrackLogSegment(idx);

                buf.rewind(); // set back to pos 0
                segment.getStatistic().toByteBuffer(buf);           // first write the segment statistic
                int numMetaDatas = segment.getMetaDatas().size();
                buf.putInt(numMetaDatas);                           // then the number of metaData blocks
                for (MetaData metaData : segment.getMetaDatas()){ // and then per metaData entry:
                    buf.putInt(metaData.numPoints);                 // numPoints in metaData Object
                    metaData.bBox.toByteBuffer(buf);                // and the bbox
                }
                fos.write(buf.array(), 0, buf.position());
                fosSize += buf.position();
            }

            int align = -fosSize;
            while (align < 0) align += MetaData.BUF_SIZE;
            if (align > 0){
                byte[] b = new byte[align];
                buf.rewind(); // set back to pos 0
                buf.put(b);
                fos.write(buf.array(), 0, buf.position());
            }
            // so now there is an alignment to a multiple of BUF_SIZE

            // now write the real point meta data as collected in the buffers
            for (int idx=0; idx<trackLog.getNumberOfSegments(); idx++){
                TrackLogSegment segment = trackLog.getTrackLogSegment(idx);

                for (MetaData metaData : segment.getMetaDatas()){
                    fos.write(metaData.buf.array());
                }
            }


        } catch (IOException e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
            }
        }
    }

    public static void readMetaData(FileInputStream in, TrackLog trackLog) {
        try {
            Log.d(MGMapApplication.LABEL, NameUtil.context()+" "+trackLog.getName());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] b = new byte[MetaData.BUF_SIZE];
            while (in.available() >= b.length){
                in.read(b);
                ByteBuffer buf = ByteBuffer.wrap(b);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                if (buf.getLong() == MAGIC) break;  //indicates bloack with first lat,lon,ele values
                baos.write(b); // still header data
            }
            ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());
            buf.order(ByteOrder.LITTLE_ENDIAN);

            Assert.check( buf.getLong() == MAGIC-1 );
            Assert.check( buf.getInt() == VERSION );

            trackLog.setTrackStatistic( new TrackLogStatistic().fromByteBuffer(buf).setFrozen(true) );
            int numSegments = buf.getInt();
            trackLog.setAvailable(false); // next access to getTrackLogSegment will cause loading of lalos

            for (int idx=0; idx<numSegments; idx++){
                TrackLogSegment segment = new TrackLogSegment(trackLog, idx);
                TrackLogStatistic statistic = new TrackLogStatistic().fromByteBuffer(buf).setFrozen(true);
                segment.setStatistic(statistic);
                trackLog.getTrackLogSegments().add(segment);
//                Log.i(TAG, "readMeta: "+statistic.toString());

                int numMetaDatas = buf.getInt();
                for (int mIdx=0; mIdx<numMetaDatas; mIdx++){
                    MetaData metaData = new MetaData();
                    metaData.numPoints = buf.getInt();
                    metaData.bBox.fromByteBuffer(buf);
                    segment.getMetaDatas().add(metaData);
                    segment.getBBox().extend(metaData.bBox);
                }

            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
            }
        }
    }

    public static ArrayList<TrackLog> loadMetaData(){
        ArrayList<TrackLog> trackLogs = new ArrayList<>();
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" loading meta files started");

        for (String name : PersistenceManager.getInstance().getMetaNames()){
            final TrackLog trackLog = new TrackLog();
            trackLog.setName(name);
            MetaDataUtil.readMetaData(PersistenceManager.getInstance().openMetaInput(name), trackLog);
            trackLogs.add(trackLog);
        }

        Log.i(MGMapApplication.LABEL, NameUtil.context()+" loading meta files finished ("+trackLogs.size()+").");
        return trackLogs;
    }


    public static void loadLaLoBufs(TrackLog trackLog){
        try {
            InputStream in = PersistenceManager.getInstance().openMetaInput(trackLog.getName());
            trackLog.setAvailable(true);
            for (TrackLogSegment segment : trackLog.getTrackLogSegments()){
                for (int mIdx = 0; mIdx<segment.getMetaDatas().size(); mIdx++) {
                    MetaData metaData = segment.getMetaDatas().get(mIdx);
                    ByteBuffer buf = getNextLaloBuf(in);
                    Assert.check (buf != null);
                    Assert.check ( buf.getShort() == segment.getSegmentIdx() );
                    Assert.check ( buf.getShort() == mIdx );
                    // all checks passed, now read the laloel values
                    for (int pIdx=0; pIdx<metaData.numPoints; pIdx++){
                        PointModelImpl pmi = new PointModelImpl();
                        pmi.fromBuf(buf);
                        segment.addPoint(pmi);
                    }
                }
            }
            in.close();
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }

    }

    private static ByteBuffer getNextLaloBuf(InputStream in){
        try {
            Log.i(MGMapApplication.LABEL, NameUtil.context()+ " "+in.available());
            while (in.available() >= MetaData.BUF_SIZE){
                byte[] b = new byte[MetaData.BUF_SIZE];
                in.read(b);
                ByteBuffer buf = ByteBuffer.wrap(b);
                buf.order(ByteOrder.LITTLE_ENDIAN);

                if (buf.getLong() == MAGIC) return buf;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean checkLaLoRecords(TrackLog trackLog, BBox checkBBox){
        for (int idx=0; idx < trackLog.getNumberOfSegments(); idx++){
            TrackLogSegment segment = trackLog.getTrackLogSegment(idx);

            int metaStartPoint = 0;
            for (MetaData metaData : segment.getMetaDatas()){
                if (metaData.bBox.intersects(checkBBox)){
                    // at least rough match
                    for (int pIdx=metaStartPoint; pIdx<metaStartPoint+metaData.numPoints; pIdx++) {
                        if (checkBBox.contains(segment.get(pIdx))) return true;
                    }
                }
                metaStartPoint += metaData.numPoints;
            }
        }
        return false;
    }

}
