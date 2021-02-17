package mg.mgmap.generic.model;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class TrackLogStatisticTest {

    @Test
    void createTrackLogStatistic1() {
        TrackLogStatistic stat = new TrackLogStatistic();
        assertFalse(stat.isFrozen());
        assertEquals(-1, stat.getSegmentIdx());
        assertEquals(0, stat.getTStart());
        assertEquals(0, stat.getTEnd());
        assertEquals(0, stat.getDuration());
        assertEquals(0, stat.getTotalLength());
        assertEquals(0, stat.getGain());
        assertEquals(0, stat.getLoss());
        assertEquals(-PointModel.NO_ELE, stat.getMinEle());
        assertEquals( PointModel.NO_ELE, stat.getMaxEle());
        assertEquals( 0, stat.getNumPoints());
    }

    @Test
    void createTrackLogStatistic2() {
        TrackLogStatistic stat = new TrackLogStatistic(5);
        assertEquals(5, stat.getSegmentIdx());
        stat.setSegmentIdx(3);
        stat.setTStart(1234567654L);
        stat.setDuration(5111222333L);
        stat.setTotalLength(123456.78);
        stat.setGain(1234);
        stat.setLoss(1243);
        stat.setMinEle(123.45f);
        stat.setMaxEle(543.21f);

        assertEquals(3, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(5111222333L, stat.getDuration());
        assertEquals(1234567654L+5111222333L, stat.getTEnd());
        assertEquals(123456.78, stat.getTotalLength());
        assertEquals(1234, stat.getGain());
        assertEquals(1243, stat.getLoss());
        assertEquals(123.45f, stat.getMinEle());
        assertEquals(543.21f, stat.getMaxEle());
        assertEquals( 0, stat.getNumPoints());
    }

    @Test
    void updateWithPoint() {
        TrackLogStatistic stat = new TrackLogStatistic(-2);

        TrackLogPoint tlp = TrackLogPoint.createGpsLogPoint(1234567654L, 49.4, 8.6, 3.14f, 123.45, 49.49f, 121.21f);
        stat.updateWithPoint(tlp);

        assertEquals(-2, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(0, stat.getDuration());
        assertEquals(0, stat.getTotalLength());
        assertEquals(0, stat.getGain());
        assertEquals(0, stat.getLoss());
        assertTrue(Math.abs(stat.getMinEle()-(123.45f-49.49f)) < 0.001);
        assertTrue(Math.abs(stat.getMaxEle()-(123.45f-49.49f)) < 0.001);
        assertEquals( 1, stat.getNumPoints());

        TrackLogPoint tlp2 = TrackLogPoint.createGpsLogPoint(1234568655L, 49.4001, 8.6001, 3.14f, 126.45, 48.49f, 121.21f);
        stat.updateWithPoint(tlp2);

        assertEquals(-2, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(1001, stat.getDuration());
//        assertEquals(4, stat.getGain());
        assertTrue(Math.abs(stat.getTotalLength()-(13.282)) < 0.001);
        assertTrue(Math.abs(stat.getGain()-(0.0)) < 0.001);
        assertTrue(Math.abs(stat.getLoss()-(0.0)) < 0.001);
        assertTrue(Math.abs(stat.getMinEle()-(123.45f-49.49f)) < 0.001);
        assertTrue(Math.abs(stat.getMaxEle()-(126.45f-48.49f)) < 0.001);
        assertEquals( 2, stat.getNumPoints());

        TrackLogPoint tlp3 = TrackLogPoint.createGpsLogPoint(1234568665L, 49.4002, 8.6002, 3.14f, 132.45, 47.49f, 121.21f);
        stat.updateWithPoint(tlp3);

        assertEquals(-2, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(1011, stat.getDuration());
//        assertEquals(11, stat.getGain());
//        assertEquals(14, stat.getTotalLength());
        assertTrue(Math.abs(stat.getTotalLength()-(26.563)) < 0.001);
        assertTrue(Math.abs(stat.getGain()-(11.0)) < 0.001);
        assertTrue(Math.abs(stat.getLoss()-(0.0)) < 0.001);
        assertTrue(Math.abs(stat.getMinEle()-(123.45f-49.49f)) < 0.001);
        assertTrue(Math.abs(stat.getMaxEle()-(132.45f-47.49f)) < 0.001);
        assertEquals( 3, stat.getNumPoints());

        TrackLogPoint tlp4 = TrackLogPoint.createGpsLogPoint(1234568666L, 49.4003, 8.6003, 3.14f, 112.45, 47.49f, 121.21f);
        stat.updateWithPoint(tlp4);

        assertEquals(-2, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(1012, stat.getDuration());
//        assertEquals(11, stat.getGain());
//        assertEquals(14, stat.getTotalLength());
        assertTrue(Math.abs(stat.getTotalLength()-(39.845)) < 0.001);
        assertTrue(Math.abs(stat.getGain()-(11.0)) < 0.001);
        assertTrue(Math.abs(stat.getLoss()-(20.0)) < 0.001);
        assertTrue(Math.abs(stat.getMinEle()-(112.45f-47.49f)) < 0.001);
        assertTrue(Math.abs(stat.getMaxEle()-(132.45f-47.49f)) < 0.001);
        assertEquals( 4, stat.getNumPoints());

        stat.updateWithPoint(null);

        TrackLogPoint tlp5 = TrackLogPoint.createGpsLogPoint(1234568675L, 49.4004, 8.6004, 3.14f, 152.45, 47.49f, 121.21f);
        stat.updateWithPoint(tlp5);

        assertEquals(-2, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(1021, stat.getDuration());
//        assertEquals(11, stat.getGain());
//        assertEquals(14, stat.getTotalLength());
        assertTrue(Math.abs(stat.getTotalLength()-(39.845)) < 0.001);
        assertTrue(Math.abs(stat.getGain()-(11.0)) < 0.001);
        assertTrue(Math.abs(stat.getLoss()-(20.0)) < 0.001);
        assertTrue(Math.abs(stat.getMinEle()-(112.45f-47.49f)) < 0.001);
        assertTrue(Math.abs(stat.getMaxEle()-(152.45f-47.49f)) < 0.001);
        assertEquals( 5, stat.getNumPoints());

        TrackLogPoint tlp6 = TrackLogPoint.createGpsLogPoint(1234568675L, 49.4004, 8.6004, 3.14f, 152.45, 47.49f, 121.21f);
        tlp6.setPressureAlt(101.96f);
        stat.updateWithPoint(tlp6);

        assertEquals(-2, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(1021, stat.getDuration());
//        assertEquals(11, stat.getLoss());
//        assertEquals(14, stat.getTotalLength());
        assertTrue(Math.abs(stat.getTotalLength()-(39.845)) < 0.001);
        assertTrue(Math.abs(stat.getGain()-(11.0)) < 0.001);
        assertTrue(Math.abs(stat.getLoss()-(23.0)) < 0.001);
        assertTrue(Math.abs(stat.getMinEle()-(112.45f-47.49f)) < 0.001);
        assertTrue(Math.abs(stat.getMaxEle()-(152.45f-47.49f)) < 0.001);
        assertEquals( 6, stat.getNumPoints());

        stat.setFrozen(true);

        TrackLogPoint tlp7 = TrackLogPoint.createGpsLogPoint(1234568675L, 49.4004, 8.6004, 3.14f, 152.45, 47.49f, 121.21f);
        tlp7.setPressureAlt(101.96f);
        stat.updateWithPoint(tlp7);

        assertEquals(-2, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(1021, stat.getDuration());
//        assertEquals(11, stat.getLoss());
//        assertEquals(14, stat.getTotalLength());
        assertTrue(Math.abs(stat.getTotalLength()-(39.845)) < 0.001);
        assertTrue(Math.abs(stat.getGain()-(11.0)) < 0.001);
        assertTrue(Math.abs(stat.getLoss()-(23.0)) < 0.001);
        assertTrue(Math.abs(stat.getMinEle()-(112.45f-47.49f)) < 0.001);
        assertTrue(Math.abs(stat.getMaxEle()-(152.45f-47.49f)) < 0.001);
        assertEquals( 6, stat.getNumPoints());


    }


    @Test
    void reset() {
        TrackLogStatistic stat = new TrackLogStatistic(5);
        assertEquals(5, stat.getSegmentIdx());
        stat.setSegmentIdx(3);
        stat.setTStart(1234567654L);
        stat.setDuration(5111222333L);
        stat.setTotalLength(123456.78);
        stat.setGain(1234);
        stat.setLoss(1243);
        stat.setMinEle(123.45f);
        stat.setMaxEle(543.21f);

        TrackLogPoint tlp = TrackLogPoint.createGpsLogPoint(1234567664L, 49.4, 8.6, 3.14f, 123.45, 0f, 121.21f);
        stat.updateWithPoint(tlp);

        assertEquals(3, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(10, stat.getDuration());
        assertEquals(1234567654L+10, stat.getTEnd());
        assertEquals(123456.78, stat.getTotalLength());
        assertEquals(1234, stat.getGain());
        assertEquals(1243, stat.getLoss());
        assertEquals(123.45f, stat.getMinEle());
        assertEquals(543.21f, stat.getMaxEle());
        assertEquals( 1, stat.getNumPoints());

        stat.reset();

        assertEquals(-1, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(0, stat.getDuration());
        assertEquals(1234567654L, stat.getTEnd());
        assertEquals(0, stat.getTotalLength());
        assertEquals(0, stat.getGain());
        assertEquals(0, stat.getLoss());
        assertEquals(-PointModel.NO_ELE, stat.getMinEle());
        assertEquals(PointModel.NO_ELE, stat.getMaxEle());
        assertEquals( 0, stat.getNumPoints());
    }


    @Test
    void toByteBuffer() {
        TrackLogStatistic stat2 = new TrackLogStatistic(3);
        stat2.setTStart(1234567654L);
        stat2.setDuration(5111222333L);
        stat2.setTotalLength(123456.78);
        stat2.setGain(1234);
        stat2.setLoss(1243);
        stat2.setMinEle(123.45f);
        stat2.setMaxEle(543.21f);

        TrackLogPoint tlp = TrackLogPoint.createGpsLogPoint(1234567664L, 49.4, 8.6, 3.14f, 123.45, 0f, 121.21f);
        stat2.updateWithPoint(tlp);

        ByteBuffer buf = ByteBuffer.allocate(100);
        stat2.toByteBuffer(buf);

        buf.rewind();
        TrackLogStatistic stat = new TrackLogStatistic();
        stat.fromByteBuffer(buf);

        assertEquals(3, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(10, stat.getDuration());
        assertEquals(1234567654L+10, stat.getTEnd());
        assertEquals(123456.78, stat.getTotalLength());
        assertEquals(1234, stat.getGain());
        assertEquals(1243, stat.getLoss());
        assertEquals(123.45f, stat.getMinEle());
        assertEquals(543.21f, stat.getMaxEle());
        assertEquals( 1, stat.getNumPoints());
    }


    @Test
    void updateWithStatistics() {
        TrackLogStatistic stat1 = new TrackLogStatistic(3);
        stat1.setTStart(1234567654L);
        stat1.setDuration(500);
        stat1.setTotalLength(123456.78);
        stat1.setGain(1234);
        stat1.setLoss(1243);
        stat1.setMinEle(123.45f);
        stat1.setMaxEle(543.21f);

        TrackLogPoint tlp = TrackLogPoint.createGpsLogPoint(1234568154L, 49.4, 8.6, 3.14f, 123.45, 0f, 121.21f);
        stat1.updateWithPoint(tlp);

        TrackLogStatistic stat2 = new TrackLogStatistic(4);
        stat2.setTStart(1234567652L);
        stat2.setDuration(300);
        stat2.setTotalLength(543.21);
        stat2.setGain(1234);
        stat2.setLoss(1243);
        stat2.setMinEle(121.45f);
        stat2.setMaxEle(548.21f);

        TrackLogPoint tlp2 = TrackLogPoint.createGpsLogPoint(1234567952L, 49.4, 8.6, 3.14f, 123.45, 0f, 121.21f);
        stat2.updateWithPoint(tlp2);

        TrackLogStatistic stat = new TrackLogStatistic();
        stat.updateWithStatistics(stat1);

        assertEquals(-1, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(500, stat.getDuration());
        assertEquals(1234567654L+500, stat.getTEnd());
        assertEquals(123456.78, stat.getTotalLength());
        assertEquals(1234, stat.getGain());
        assertEquals(1243, stat.getLoss());
        assertEquals(123.45f, stat.getMinEle());
        assertEquals(543.21f, stat.getMaxEle());
        assertEquals( 1, stat.getNumPoints());

        stat.updateWithStatistics(stat2);

        assertEquals(-1, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(800, stat.getDuration());
        assertEquals(1234567654L+800, stat.getTEnd());
        assertEquals(123999.99, stat.getTotalLength());
        assertEquals(2468, stat.getGain());
        assertEquals(2486, stat.getLoss());
        assertEquals(121.45f, stat.getMinEle());
        assertEquals(548.21f, stat.getMaxEle());
        assertEquals( 2, stat.getNumPoints());

        stat.setFrozen(true);

        stat.updateWithStatistics(stat2);

        assertEquals(-1, stat.getSegmentIdx());
        assertEquals(1234567654L, stat.getTStart());
        assertEquals(800, stat.getDuration());
        assertEquals(1234567654L+800, stat.getTEnd());
        assertEquals(123999.99, stat.getTotalLength());
        assertEquals(2468, stat.getGain());
        assertEquals(2486, stat.getLoss());
        assertEquals(121.45f, stat.getMinEle());
        assertEquals(548.21f, stat.getMaxEle());
        assertEquals( 2, stat.getNumPoints());
    }

    @Test
    void durationToString() {
        TrackLogStatistic stat = new TrackLogStatistic();
        long duration = (((2 * 60 + 3) * 60) + 4) * 1000 + 333; // 02:03:04.333
        stat.setDuration(duration);
        assertEquals("2:03:04", stat.durationToString());
    }

    @Test
    void testToString() {
        TrackLogStatistic stat1 = new TrackLogStatistic(3);
        stat1.setTStart(1234567654L);
        stat1.setDuration(500);
        stat1.setTotalLength(123456.78);
        stat1.setGain(1234);
        stat1.setLoss(1243);
        stat1.setMinEle(123.45f);
        stat1.setMaxEle(543.21f);

        TrackLogPoint tlp = TrackLogPoint.createGpsLogPoint(1234569154L, 49.4, 8.6, 3.14f, 123.45, 0f, 121.21f);
        stat1.updateWithPoint(tlp);

        assertEquals("start=15.01.1970_07:56:07 duration=0:00:01 totalLength=123456.78 gain=1234.0 loss=1243.0 minEle=123.4 maxEle=543.2 numPoints=1", stat1.toString());
    }

}