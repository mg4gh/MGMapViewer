package mg.mgmap.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WriteableTrackLogTest {


    @Test
    void createWriteableTrackLogTest1() {
        WriteableTrackLog trackLog = new WriteableTrackLog("abcdef");
        assertEquals("abcdef", trackLog.name);
        assertEquals(null, trackLog.currentSegment);
    }
    @Test
    void createWriteableTrackLogTest2() {
        WriteableTrackLog trackLog = new WriteableTrackLog();
        assertEquals(null, trackLog.currentSegment);
    }


    @Test
    void startTrack1() {
        WriteableTrackLog trackLog = new WriteableTrackLog("abcdef");

        assertEquals(PointModel.NO_TIME, trackLog.trackStatistic.getTStart());
        trackLog.startTrack(556644332211L);
        assertEquals(556644332211L, trackLog.trackStatistic.getTStart());
    }
    @Test
    void startTrack2() {
        WriteableTrackLog trackLog = new WriteableTrackLog("abcdef");

        assertEquals(PointModel.NO_TIME, trackLog.trackStatistic.getTStart());
        trackLog.startTrack(556644332211L);
        trackLog.startTrack(PointModel.NO_TIME);
        assertEquals(556644332211L, trackLog.trackStatistic.getTStart());
    }

    @Test
    void startSegment() {
        WriteableTrackLog trackLog = new WriteableTrackLog("abcdef");

        assertEquals(PointModel.NO_TIME, trackLog.trackStatistic.getTStart());
        trackLog.startTrack(556644332211L);
        trackLog.startSegment(556644332233L);
        assertEquals(1,trackLog.getNumberOfSegments());
        TrackLogSegment segment = trackLog.getTrackLogSegment(0);
        assertEquals(556644332211L, trackLog.trackStatistic.getTStart());
        assertEquals(556644332233L, segment.getStatistic().getTStart());
        assertEquals(segment, trackLog.currentSegment);

        trackLog.startSegment(PointModel.NO_TIME);
        assertEquals(556644332211L, trackLog.trackStatistic.getTStart());

    }

    @Test
    void stopSegment() {
        WriteableTrackLog trackLog = new WriteableTrackLog("abcdef");
        trackLog.startTrack(556644332211L);
        trackLog.startSegment(556644332233L);

        TrackLogSegment segment = trackLog.getTrackLogSegment(0);
        WriteablePointModel pm1 = new WriteablePointModelImpl(49.4, 8.6);
        segment.addPoint(pm1);
        WriteablePointModel pm2 = new WriteablePointModelImpl(49.4001, 8.6001);
        segment.addPoint(pm2);

        assertTrue( (Math.abs(trackLog.trackStatistic.getTotalLength())-13.282) < 0.01);

        pm2.setLat(49.4002);
        pm2.setLon(8.6002);
        trackLog.stopSegment(556644332236L);

        assertTrue( (Math.abs(trackLog.trackStatistic.getTotalLength())-26.563) < 0.01);
        assertEquals(null, trackLog.currentSegment);
    }

    @Test
    void stopTrack() {
        WriteableTrackLog trackLog = new WriteableTrackLog("abcdef");
        trackLog.startTrack(0);
        assertEquals(PointModel.NO_TIME, trackLog.trackStatistic.getTStart());
        trackLog.startSegment(0);
        assertEquals(PointModel.NO_TIME, trackLog.trackStatistic.getTStart());
        WriteablePointModelImpl pm1 = new WriteablePointModelImpl(49.4001, 8.6001);
        pm1.setTimestamp(556644332235L);
        trackLog.addPoint(pm1);
        WriteablePointModelImpl pm2 = new WriteablePointModelImpl(49.4001, 8.6002);
        pm2.setTimestamp(556644332239L);
        trackLog.addPoint(pm2);
        trackLog.stopSegment(556644332240L);
        trackLog.stopTrack(556644332242L);
        assertEquals(556644332235L, trackLog.getTrackLogSegment(0).getStatistic().getTStart());
        assertEquals(556644332235L, trackLog.trackStatistic.getTStart());
        assertEquals(4, trackLog.trackStatistic.getDuration());
    }

    @Test
    void addPoint() {
        WriteableTrackLog trackLog = new WriteableTrackLog("abcdef");
        trackLog.startTrack(0);
        assertEquals(PointModel.NO_TIME, trackLog.trackStatistic.getTStart());
        trackLog.startSegment(0);
        WriteablePointModelImpl pm1 = new WriteablePointModelImpl(49.4001, 8.6001);
        pm1.setTimestamp(556644332235L);
        trackLog.addPoint(pm1);
        trackLog.stopSegment(556644332240L);
        WriteablePointModelImpl pm2 = new WriteablePointModelImpl(49.4001, 8.6002);
        pm2.setTimestamp(556644332239L);
        trackLog.addPoint(pm2);
        trackLog.stopTrack(556644332242L);
        assertEquals(2, trackLog.getTrackLogSegment(0).getStatistic().getNumPoints());
    }

    @Test
    void remainStatistic() {
        WriteableTrackLog trackLog = new WriteableTrackLog("abcdef");
        {
            trackLog.startTrack(0);
            trackLog.startSegment(0);
            WriteablePointModelImpl pm1 = new WriteablePointModelImpl(49.4001, 8.6001);
            pm1.setTimestamp(556644332235L);
            trackLog.addPoint(pm1);
            WriteablePointModelImpl pm2 = new WriteablePointModelImpl(49.4003, 8.6001);
            pm2.setTimestamp(556644332239L);
            trackLog.addPoint(pm2);
            assertTrue( (Math.abs(trackLog.trackStatistic.getTotalLength())-22.264) < 0.01);
            trackLog.stopSegment(PointModel.NO_TIME);
        }
        {
            trackLog.startTrack(0);
            trackLog.startSegment(0);
            WriteablePointModelImpl pm1 = new WriteablePointModelImpl(49.4004, 8.6001);
            pm1.setTimestamp(556644332245L);
            trackLog.addPoint(pm1);
            trackLog.stopSegment(556644332240L);
            WriteablePointModelImpl pm2 = new WriteablePointModelImpl(49.4006, 8.6001);
            pm2.setTimestamp(556644332249L);
            trackLog.addPoint(pm2);
            assertTrue( (Math.abs(trackLog.trackStatistic.getTotalLength())-44.528) < 0.01);
            trackLog.stopSegment(PointModel.NO_TIME);
        }
        TrackLogStatistic stat = new TrackLogStatistic();
        stat.updateWithStatistics(trackLog.trackStatistic);
        assertTrue( (Math.abs(trackLog.trackStatistic.getTotalLength())-44.528) < 0.01);

        trackLog.remainStatistic(stat, new PointModelImpl(49.4002, 8.6001), 0, 1);
        assertTrue( (Math.abs(stat.getTotalLength())-33.394) < 0.01);
    }

    @Test
    void recalcStatistic1() {
        WriteableTrackLog trackLog = new WriteableTrackLog("abcdef");
        trackLog.startTrack(0);
        assertEquals(PointModel.NO_TIME, trackLog.trackStatistic.getTStart());
        trackLog.startSegment(0);
        WriteablePointModelImpl pm1 = new WriteablePointModelImpl(49.4001, 8.6001);
        pm1.setTimestamp(556644332235L);
        trackLog.addPoint(pm1);
        trackLog.stopSegment(556644332240L);
        WriteablePointModelImpl pm2 = new WriteablePointModelImpl(49.4002, 8.6002);
        pm2.setTimestamp(556644332239L);
        trackLog.addPoint(pm2);
        assertTrue( (Math.abs(trackLog.trackStatistic.getTotalLength())-13.282) < 0.01);

        pm2.setLat(49.4002);
        pm2.setLon(8.6002);
        trackLog.recalcStatistic();
        assertTrue( (Math.abs(trackLog.trackStatistic.getTotalLength())-26.563) < 0.01);

        trackLog.stopTrack(556644332242L);
    }
    @Test
    void recalcStatistic2() {
        WriteableTrackLog trackLog = new WriteableTrackLog("abcdef");
        trackLog.startTrack(0);
        assertEquals(PointModel.NO_TIME, trackLog.trackStatistic.getTStart());
        trackLog.startSegment(0);
        WriteablePointModelImpl pm1 = new WriteablePointModelImpl(49.4001, 8.6001);
        pm1.setTimestamp(556644332235L);
        trackLog.addPoint(pm1);
        trackLog.stopSegment(556644332240L);
        WriteablePointModelImpl pm2 = new WriteablePointModelImpl(49.4002, 8.6002);
        pm2.setTimestamp(556644332239L);
        trackLog.addPoint(pm2);
        assertTrue( (Math.abs(trackLog.trackStatistic.getTotalLength())-13.282) < 0.01);
        trackLog.stopSegment(PointModel.NO_TIME);

        pm2.setLat(49.4002);
        pm2.setLon(8.6002);
        trackLog.recalcStatistic();
        assertTrue( (Math.abs(trackLog.trackStatistic.getTotalLength())-26.563) < 0.01);

        trackLog.stopTrack(556644332242L);
    }
}