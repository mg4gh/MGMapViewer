package mg.mgmap.generic.model;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TrackLogPointTest {

    @Test
    void createGpsLogPoint1() {
        TrackLogPoint tlp = TrackLogPoint.createGpsLogPoint(123456789L, 49.4, 8.6, 3.14f, 173.45, 49.49f, 1.23f);
        assertEquals(123456789L, tlp.getTimestamp());
        assertEquals(49.4, tlp.getLat());
        assertEquals(8.6, tlp.getLon());
        assertEquals(3, tlp.getNmeaAcc());
        assertTrue(Math.abs(tlp.getWgs84ele()-173.45) < 0.001 );
        assertTrue(Math.abs(tlp.getNmeaEle()-(173.45 -49.49f)) < 0.001 );
        assertTrue(Math.abs(tlp.getEleA()-(173.45 -49.49f)) < 0.001);
        assertTrue(Math.abs(tlp.getPressure()-PointModel.NO_PRES) < 0.001 );
        assertTrue(Math.abs(tlp.getPressureEle()-PointModel.NO_ELE) < 0.001 );
    }
    @Test
    void createGpsLogPoint2() {
        TrackLogPoint tlp = TrackLogPoint.createGpsLogPoint(123456789L, 49.4, 8.6, 3.14f, 0, 0, 1.23f);
        assertEquals(123456789L, tlp.getTimestamp());
        assertEquals(49.4, tlp.getLat());
        assertEquals(8.6, tlp.getLon());
        assertEquals(3, tlp.getNmeaAcc());
        assertTrue(Math.abs(tlp.getWgs84ele()-PointModel.NO_ELE) < 0.001 );
        assertTrue(Math.abs(tlp.getNmeaEle()-PointModel.NO_ELE) < 0.001 );
        assertTrue(Math.abs(tlp.getHgtEle()-111.11) < 0.001);
        assertTrue(Math.abs(tlp.getEleA()-111.11) < 0.001);
        assertTrue(Math.abs(tlp.getPressure()-PointModel.NO_PRES) < 0.001 );
        assertTrue(Math.abs(tlp.getPressureEle()-PointModel.NO_ELE) < 0.001 );
    }

    @Test
    void createLogPoint() {
        TrackLogPoint tlp = TrackLogPoint.createLogPoint(49.4, 8.6);
        assertEquals(PointModel.NO_TIME, tlp.getTimestamp());
        assertEquals(49.4, tlp.getLat());
        assertEquals(8.6, tlp.getLon());
        assertEquals(0, tlp.getNmeaAcc());
        assertTrue(Math.abs(tlp.getWgs84ele()-PointModel.NO_ELE) < 0.001 );
        assertTrue(Math.abs(tlp.getNmeaEle()-PointModel.NO_ELE) < 0.001 );
        assertTrue(Math.abs(tlp.getHgtEle()-PointModel.NO_ELE) < 0.001);
        assertTrue(Math.abs(tlp.getEleA()-PointModel.NO_ELE) < 0.001);
        assertTrue(Math.abs(tlp.getPressure()-PointModel.NO_PRES) < 0.001 );
        assertTrue(Math.abs(tlp.getPressureEle()-PointModel.NO_ELE) < 0.001 );
    }

    @Test
    void createTrackLogPoint1() {
        TrackLogPoint tlp2 = TrackLogPoint.createGpsLogPoint(123456789L, 49.4, 8.6, 3.14f, 173.45, 49.49f, 1.23f);
        tlp2.setPressure(1000.13f);
        tlp2.setPressureEle(133.33f);
        TrackLogPoint tlp = new TrackLogPoint(tlp2);

        assertEquals(123456789L, tlp.getTimestamp());
        assertEquals(49.4, tlp.getLat());
        assertEquals(8.6, tlp.getLon());
        assertEquals(3, tlp.getNmeaAcc());
        assertTrue(Math.abs(tlp.getWgs84ele()-173.45) < 0.001 );
        assertTrue(Math.abs(tlp.getNmeaEle()-(173.45 -49.49f)) < 0.001 );
        assertTrue(Math.abs(tlp.getHgtEle()-111.11) < 0.001);
        assertTrue(Math.abs(tlp.getEleA()-(173.45 -49.49f)) < 0.001);
        assertTrue(Math.abs(tlp.getPressure()-1000.13) < 0.001 );
        assertTrue(Math.abs(tlp.getPressureEle()-133.33) < 0.001 );
    }


    @Test
    void toFromByteBuffer() {
        TrackLogPoint tlp2 = TrackLogPoint.createGpsLogPoint(123456789L, 49.4, 8.6, 3.14f, 173.45, 49.49f, 1.23f);
        tlp2.setPressure(1000.13f);
        tlp2.setPressureEle(133.33f);
        ByteBuffer buf = ByteBuffer.allocate(100);
        tlp2.toByteBuffer(buf);
        assertEquals(44, buf.position());
        buf.rewind();
        TrackLogPoint tlp = new TrackLogPoint();
        tlp.fromByteBuffer(buf);

        assertEquals(123456789L, tlp.getTimestamp());
        assertEquals(49.4, tlp.getLat());
        assertEquals(8.6, tlp.getLon());
        assertEquals(3, tlp.getNmeaAcc());
        assertTrue(Math.abs(tlp.getWgs84ele()-173.45) < 0.001 );
        assertTrue(Math.abs(tlp.getNmeaEle()-(173.45 -49.49f)) < 0.001 );
        assertTrue(Math.abs(tlp.getHgtEle()-111.11) < 0.001);
        assertTrue(Math.abs(tlp.getEleA()-(173.45 -49.49f)) < 0.001);
        assertTrue(Math.abs(tlp.getPressure()-1000.13) < 0.001 );
        assertTrue(Math.abs(tlp.getPressureEle()-133.33) < 0.001 );
    }


    @Test
    void getEleA() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getEleA()-PointModel.NO_ELE) < 0.001);
        tlp.setHgtEle(111.23f);
        assertTrue(Math.abs(tlp.getHgtEle()-111.23) < 0.001);
        assertTrue(Math.abs(tlp.getEleA()-111.23) < 0.001);
        tlp.setEle(123.45f);
        assertTrue(Math.abs(tlp.getEleA()-123.45) < 0.001);
    }

    @Test
    void getEleD() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getEleD()-PointModel.NO_ELE) < 0.001);
        tlp.setHgtEle(111.23f);
        assertTrue(Math.abs(tlp.getHgtEle()-111.23) < 0.001);
        assertTrue(Math.abs(tlp.getEleD()-111.23) < 0.001);
        tlp.setEle(123.45f);
        assertTrue(Math.abs(tlp.getEleD()-123.45) < 0.001);
        tlp.setPressureEle(211.45f);
        assertTrue(Math.abs(tlp.getEleD()-211.45) < 0.001);
    }

    @Test
    void getTimestamp() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertEquals(PointModel.NO_TIME, tlp.getTimestamp());
        tlp.setTimestamp(5432123456L);
        assertEquals(5432123456L, tlp.getTimestamp());
    }

    @Test
    void getNmeaAlt() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getNmeaEle()-PointModel.NO_ELE) < 0.001);
        tlp.setNmeaEle(111.23f);
        assertTrue(Math.abs(tlp.getNmeaEle()-111.23) < 0.001);
    }

    @Test
    void getAccuracy() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getNmeaAcc()-0) < 0.001);
        tlp.setNmeaAcc(11.23f);
        assertTrue(Math.abs(tlp.getNmeaAcc()-11) < 0.001);
    }

    @Test
    void getPressure() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getPressure()-PointModel.NO_PRES) < 0.001);
        tlp.setPressure(999.88f);
        assertTrue(Math.abs(tlp.getPressure()-999.88) < 0.001);
    }

    @Test
    void getWgs84alt() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getWgs84ele()-PointModel.NO_ELE) < 0.001);
        tlp.setWgs84ele(111.23f);
        assertTrue(Math.abs(tlp.getWgs84ele()-111.23) < 0.001);
    }

    @Test
    void getPressureAlt() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getPressureEle()-PointModel.NO_ELE) < 0.001);
        tlp.setPressureEle(111.23f);
        assertTrue(Math.abs(tlp.getPressureEle()-111.23) < 0.001);
   }

    @Test
    void getHgtAlt() {
        TrackLogPoint tlp = new TrackLogPoint();
        assertTrue(Math.abs(tlp.getHgtEle()-PointModel.NO_ELE) < 0.001);
        tlp.setHgtEle(111.23f);
        assertTrue(Math.abs(tlp.getHgtEle()-111.23) < 0.001);
    }

}