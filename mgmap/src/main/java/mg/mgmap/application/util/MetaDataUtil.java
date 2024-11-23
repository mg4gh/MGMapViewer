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
package mg.mgmap.application.util;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MetaData;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.generic.util.basic.MGLog;

public class MetaDataUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static final long MAGIC = 0xAFFEAFFED00FD00FL;
    public static final int VERSION = 0x0200;

    final MGMapApplication application;
    final PersistenceManager persistenceManager;

    public MetaDataUtil(MGMapApplication application, PersistenceManager persistenceManager){
        this.application = application;
        this.persistenceManager = persistenceManager;
    }

    public void createMetaData(TrackLog trackLog){
        for (int idx=0; idx<trackLog.getNumberOfSegments(); idx++){
            TrackLogSegment segment = trackLog.getTrackLogSegment(idx);
            ArrayList<MetaData> metaDatas = segment.getMetaDatas();
            metaDatas.clear();                                            // just in case there are already some meta data

            for (int pIdx=0; pIdx<segment.size(); pIdx++){
                int mIdx = pIdx / MetaData.POINTS_PER_BUF;
                if (mIdx >= metaDatas.size()){                            // if we need next metaData entry
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
                if (pm instanceof PointModelImpl pmi) {
                    metaData.bBox.extend(pmi);
                    pmi.toBuf(metaData.buf);
                    metaData.numPoints++;
                }
            }
        }
    }

    public void writeMetaData(FileOutputStream fos, TrackLog trackLog){
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
            mgLog.e(e);
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                mgLog.e(e);
            }
        }
    }

    public void readMetaData(FileInputStream in, TrackLog trackLog) {
        try {
            mgLog.v(trackLog.getName());
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] b = new byte[MetaData.BUF_SIZE];
            while (in.available() >= b.length){
                int inl = in.read(b);
                assert (b.length == inl);
                ByteBuffer buf = ByteBuffer.wrap(b);
                buf.order(ByteOrder.LITTLE_ENDIAN);
                if (buf.getLong() == MAGIC) break;  //indicates block with first lat,lon,ele values
                baos.write(b); // still header data
            }
            ByteBuffer buf = ByteBuffer.wrap(baos.toByteArray());
            buf.order(ByteOrder.LITTLE_ENDIAN);

            long magic = buf.getLong();
            assert( magic == MAGIC-1 );
            int version = buf.getInt();
            assert( version == VERSION );

            trackLog.setTrackStatistic( new TrackLogStatistic().fromByteBuffer(buf).setFrozen(true) );
            int numSegments = buf.getInt();
            trackLog.setAvailable(false); // next access to getTrackLogSegment will cause loading of lalos

            for (int idx=0; idx<numSegments; idx++){
                TrackLogSegment segment = new TrackLogSegment(idx);
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
            mgLog.e(e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                mgLog.e(e);
            }
        }
    }

    public void loadLaLoBufs(TrackLog trackLog){
        try {
            InputStream in = persistenceManager.openMetaInput(trackLog.getName());
            trackLog.setAvailable(true);
            for (TrackLogSegment segment : trackLog.getTrackLogSegments()){
                for (int mIdx = 0; mIdx<segment.getMetaDatas().size(); mIdx++) {
                    MetaData metaData = segment.getMetaDatas().get(mIdx);
                    ByteBuffer buf = getNextLaloBuf(in);
                    assert  (buf != null);
                    short segIdx = buf.getShort();
                    assert  ( segIdx == segment.getSegmentIdx() );
                    short metaIdx = buf.getShort();
                    assert  ( metaIdx == mIdx );
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
            mgLog.e(e);
        }

    }

    private ByteBuffer getNextLaloBuf(InputStream in){
        try {
            mgLog.d(in.available());
            while (in.available() >= MetaData.BUF_SIZE){
                byte[] b = new byte[MetaData.BUF_SIZE];
                int inl = in.read(b);
                assert (b.length == inl);
                ByteBuffer buf = ByteBuffer.wrap(b);
                buf.order(ByteOrder.LITTLE_ENDIAN);

                if (buf.getLong() == MAGIC) return buf;
            }
        } catch (IOException e) {
            mgLog.e(e);
        }
        return null;
    }

    public boolean checkLaLoRecords(TrackLog trackLog, BBox checkBBox){
        for (int idx=0; idx < trackLog.getNumberOfSegments(); idx++){
            TrackLogSegment segment = trackLog.getTrackLogSegment(idx);

            int metaStartPoint = 0;
            for (MetaData metaData : segment.getMetaDatas()){
                if (metaData.bBox.intersects(checkBBox)){
                    // at least rough match is given
                    checkAvailability(trackLog); // before we check the points, ensure that they are available
                    for (int pIdx=metaStartPoint; pIdx<metaStartPoint+metaData.numPoints; pIdx++) {
                        if (checkBBox.contains(segment.get(pIdx))) return true;
                    }
                }
                metaStartPoint += metaData.numPoints;
            }
        }
        return false;
    }

    public void checkAvailability(TrackLog trackLog){
        if (!trackLog.isAvailable()){
            loadLaLoBufs(trackLog);
        }
    }

}
