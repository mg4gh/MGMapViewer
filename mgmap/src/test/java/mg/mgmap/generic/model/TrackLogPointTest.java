package mg.mgmap.generic.model;

import java.nio.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

public class TrackLogPointTest {

    @Test
    public void createGpsLogPoint1() {
        TrackLogPoint tlp = TrackLogPoint.createGpsLogPoint(123456789L, 49.4, 8.6, 3.14f, 173.45, 49.49f, 1.23f);
        assertEquals(123456789L, tlp.getTimestamp());
        assertEquals(49.4, tlp.getLat(),0);
        assertEquals(8.6, tlp.getLon(),0);
        assertTrue(Math.abs( tlp.getNmeaAcc() - 3.1) < 0.001);
        assertTrue(Math.abs(tlp.getWgs84ele()-173.45) < 0.001 );
        assertTrue(Math.abs(tlp.getNmeaEle()-(173.45 -49.49f)) < 0.001 );
        assertTrue(Math.abs(tlp.getEleA()-(173.45 -49.49f)) < 0.001);
        assertTrue(Math.abs(tlp.getPressure()-PointModel.NO_PRES) < 0.001 );
        assertTrue(Math.abs(tlp.getPressureEle()-PointModel.NO_ELE) < 0.001 );
    }
    @Test
    public void createGpsLogPoint2() {
        TrackLogPoint tlp = TrackLogPoint.createGpsLogPoint(123456789L, 49.4, 8.6, 3.14f, 0, 0, 1.23f);
        assertEquals(123456789L, tlp.getTimestamp());
        assertEquals(49.4, tlp.getLat(),0);
        assertEquals(8.6, tlp.getLon(),0);
        assertTrue(Math.abs( tlp.getNmeaAcc() - 3.1) < 0.001);
        assertTrue(Math.abs(tlp.getWgs84ele()-PointModel.NO_ELE) < 0.001 );
        assertTrue(Math.abs(tlp.getNmeaEle()-PointModel.NO_ELE) < 0.001 );
        assertTrue(Math.abs(tlp.getHgtEle()-PointModel.NO_ELE) < 0.001);
        assertTrue(Math.abs(tlp.getEleA()-PointModel.NO_ELE) < 0.001);
        assertTrue(Math.abs(tlp.getPressure()-PointModel.NO_PRES) < 0.001 );
        assertTrue(Math.abs(tlp.getPressureEle()-PointModel.NO_ELE) < 0.001 );
    }

    @Test
    public void createLogPoint() {
        TrackLogPoint tlp = TrackLogPoint.createLogPoint(49.4, 8.6);
        assertEquals(PointModel.NO_TIME, tlp.getTimestamp());
        assertEquals(49.4, tlp.getLat(),0);
        assertEquals(8.6, tlp.getLon(),0);
        assertEquals(PointModel.NO_ACC, tlp.getNmeaAcc(),0);
        assertEquals(PointModel.NO_ELE, tlp.getWgs84ele(), 0.001 );
        assertEquals(PointModel.NO_ELE, tlp.getNmeaEle(), 0.001 );
        assertEquals(PointModel.NO_ELE, tlp.getHgtEle(), 0.001);
        assertEquals(PointModel.NO_ELE, tlp.getEleA(), 0.001);
        assertEquals(PointModel.NO_PRES,tlp.getPressure(), 0.001 );
        assertEquals(PointModel.NO_ELE, tlp.getPressureEle(),0.001 );
    }

    @Test
    public void createTrackLogPoint1() {
        TrackLogPoint tlp2 = TrackLogPoint.createGpsLogPoint(123456789L, 49.4, 8.6, 3.14f, 173.45, 49.49f, 1.23f);
        tlp2.setPressure(1000.13f);
        tlp2.setPressureEle(133.33f);
        TrackLogPoint tlp = new TrackLogPoint(tlp2);

        assertEquals(123456789L, tlp.getTimestamp());
        assertEquals(49.4, tlp.getLat(),0);
        assertEquals(8.6, tlp.getLon(),0);
        assertTrue(Math.abs(tlp.getNmeaAcc()-3.1) < 0.001 );
        assertTrue(Math.abs(tlp.getWgs84ele()-173.45) < 0.001 );
        assertTrue(Math.abs(tlp.getNmeaEle()-(173.45 -49.49f)) < 0.001 );
        assertEquals(PointModel.NO_ELE, tlp.getHgtEle(), 0);
//        assertTrue(Math.abs(tlp.getHgtEle()-111.11) < 0.001);
        assertEquals((173.45 -49.49f), tlp.getEleA(), 0.001);
        assertEquals(1000.13, tlp.getPressure(), 0.001 );
        assertEquals(133.33, tlp.getPressureEle(), 0.001 );
    }


    @Test
    public void toFromByteBuffer() {
        TrackLogPoint tlp2 = TrackLogPoint.createGpsLogPoint(123456789L, 49.4, 8.6, 3.14f, 173.45, 49.49f, 1.23f);
        tlp2.setPressure(1000.13f);
        tlp2.setPressureEle(133.33f);
        ByteBuffer buf = ByteBuffer.allocate(100);
        tlp2.toByteBuffer(buf);
        assertEquals(56, buf.position());
        buf.rewind();
        TrackLogPoint tlp = new TrackLogPoint();
        tlp.fromByteBuffer(buf);

        assertEquals(123456789L, tlp.getTimestamp());
        assertEquals(49.4, tlp.getLat(),0);
        assertEquals(8.6, tlp.getLon(),0);
        assertTrue(Math.abs(tlp.getNmeaAcc()-3.1) < 0.001 );
        assertTrue(Math.abs(tlp.getWgs84ele()-173.45) < 0.001 );
        assertTrue(Math.abs(tlp.getNmeaEle()-(173.45 -49.49f)) < 0.001 );
        assertTrue(Math.abs(tlp.getHgtEle()-PointModel.NO_ELE) < 0.001);
        assertTrue(Math.abs(tlp.getEleA()-(173.45 -49.49f)) < 0.001);
        assertTrue(Math.abs(tlp.getPressure()-1000.13) < 0.001 );
        assertTrue(Math.abs(tlp.getPressureEle()-133.33) < 0.001 );
    }


    @Test
    public void getEleA() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getEleA()-PointModel.NO_ELE) < 0.001);
        tlp.setHgtEle(111.23f);
        assertTrue(Math.abs(tlp.getHgtEle()-111.23) < 0.001);
        assertTrue(Math.abs(tlp.getEleA()-111.23) < 0.001);
        tlp.setEle(123.45f);
        assertTrue(Math.abs(tlp.getEleA()-123.45) < 0.001);
    }

    @Test
    public void getEleD() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getEleD()-PointModel.NO_ELE) < 0.001);
        tlp.setEle(123.45f);
        assertTrue(Math.abs(tlp.getEleD()-123.45) < 0.001);
        tlp.setHgtEle(111.23f);
        assertTrue(Math.abs(tlp.getHgtEle()-111.23) < 0.001);
        assertTrue(Math.abs(tlp.getEleD()-111.23) < 0.001);
        tlp.setPressureEle(211.45f);
        assertTrue(Math.abs(tlp.getEleD()-211.45) < 0.001);
    }

    @Test
    public void getTimestamp() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertEquals(PointModel.NO_TIME, tlp.getTimestamp());
        tlp.setTimestamp(5432123456L);
        assertEquals(5432123456L, tlp.getTimestamp());
    }

    @Test
    public void getNmeaAlt() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getNmeaEle()-PointModel.NO_ELE) < 0.001);
        tlp.setNmeaEle(111.23f);
        assertEquals(111.23, tlp.getNmeaEle(), 0.001);
    }

    @Test
    public void getAccuracy() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertEquals(PointModel.NO_ACC, tlp.getNmeaAcc(),0);
        tlp.setNmeaAcc(11.23f);
        assertEquals(11,tlp.getNmeaAcc(), 0.001);
    }

    @Test
    public void getPressure() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getPressure()-PointModel.NO_PRES) < 0.001);
        tlp.setPressure(999.88f);
        assertEquals(999.88, tlp.getPressure(), 0.001);
    }

    @Test
    public void getWgs84alt() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getWgs84ele()-PointModel.NO_ELE) < 0.001);
        tlp.setWgs84ele(111.23f);
        assertEquals(111.23, tlp.getWgs84ele(), 0.001);
    }

    @Test
    public void getPressureAlt() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getPressureEle()-PointModel.NO_ELE) < 0.001);
        tlp.setPressureEle(111.23f);
        assertEquals(111.23, tlp.getPressureEle(), 0.001);
   }

    @Test
    public void getHgtAlt() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getHgtEle()-PointModel.NO_ELE) < 0.001);
        tlp.setHgtEle(111.23f);
        assertEquals(111.23, tlp.getHgtEle(), 0.001);
    }

}