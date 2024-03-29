package mg.mgmap.generic.model;

import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

import mg.mgmap.generic.util.Observer;

public class TrackLogTest {


    @Test
    public void createTrackLog(){
        TrackLog trackLog = new TrackLog();
        assertNotNull(trackLog.trackLogSegments);
        assertNotNull(trackLog.trackStatistic);
        assertEquals("", trackLog.name);
        assertTrue(trackLog.available);
        assertFalse(trackLog.modified);
    }

    @Test
    public void testTrackStatistic() {
        TrackLog trackLog = new TrackLog();
        assertEquals(-1, trackLog.getTrackStatistic().getSegmentIdx());
        trackLog.setTrackStatistic( new TrackLogStatistic(5) );
        assertEquals(5, trackLog.getTrackStatistic().getSegmentIdx());
    }

    @Test
    public void testName() {
        TrackLog trackLog = new TrackLog();
        assertEquals("", trackLog.name);
        trackLog.setName("abcdef");
        assertEquals("abcdef", trackLog.name);
    }

    @Test
    public void getNameKey() {
        TrackLog trackLog = new TrackLog();
        trackLog.getTrackStatistic().setTStart(1599123456789L);
        trackLog.setName("abcdef");
        assertEquals("abcdef", trackLog.name);
        assertEquals("abcdef", trackLog.getName());
        assertEquals("20200903_105736_abcdef", trackLog.getNameKey());
    }

    @Test
    public void testAvailable() {
        TrackLog trackLog = new TrackLog();
        assertTrue(trackLog.available);
        assertTrue(trackLog.isAvailable());
        trackLog.setAvailable(false);
        assertFalse(trackLog.available);
    }

    boolean bObserverTest = false;
    @Test
    public void testModified() {
        Observer o = (e) -> bObserverTest = true;
        TrackLog trackLog = new TrackLog();
        trackLog.addObserver(o);
        assertFalse(trackLog.modified);
        assertFalse(trackLog.isModified());
        trackLog.setModified(true);
        assertTrue(trackLog.modified);
        assertTrue(bObserverTest);

        bObserverTest = false;
        trackLog.setModified(true);
        assertFalse(bObserverTest);
    }

    @Test
    public void getTrackLogSegments() {
        TrackLog trackLog = new TrackLog();
        assertEquals(0, trackLog.getTrackLogSegments().size());
    }

    @Test
    public void getTrackLogSegment() {
        TrackLog trackLog = new TrackLog();
        trackLog.getTrackLogSegments().add( new TrackLogSegment(1));
        trackLog.getTrackLogSegments().add( new TrackLogSegment(2));
        trackLog.getTrackLogSegments().add( new TrackLogSegment(3));
        assertEquals(3, trackLog.getTrackLogSegments().size());
        assertEquals(3, trackLog.getTrackLogSegment(2).getSegmentIdx());
        assertEquals(3, trackLog.getNumberOfSegments());
    }

    @Test
    public void compareTo() {
        TrackLog trackLog1 = new TrackLog();
        trackLog1.getTrackStatistic().setTStart(1599123456789L);
        TrackLog trackLog2 = new TrackLog();
        trackLog2.getTrackStatistic().setTStart(1599123456789L);
        TrackLog trackLog3 = new TrackLog();
        trackLog2.getTrackStatistic().setTStart(1599123456799L);
        assertEquals(0, trackLog1.compareTo( trackLog2 ));
        assertEquals(2, trackLog1.compareTo( trackLog3 ));
    }

    @Test
    public void getBBox() {
        TrackLogSegment s1 = new TrackLogSegment(1);
        s1.addPoint(new PointModelImpl(49.4, 8.6));
        TrackLogSegment s2 = new TrackLogSegment(2);
        s2.addPoint(new PointModelImpl(49.5, 8.7));
        TrackLog trackLog = new TrackLog();
        trackLog.getTrackLogSegments().add(s1);
        trackLog.getTrackLogSegments().add(s2);
        assertEquals(49.4, trackLog.getBBox().minLatitude, 0 );
        assertEquals(49.5, trackLog.getBBox().maxLatitude, 0 );
    }

    @Test
    public void getBestDistance() {
        PointModelUtil.init(32);
        TrackLogSegment s1 = new TrackLogSegment(1);
        s1.addPoint(new PointModelImpl(49.4, 8.6));
        s1.addPoint(new PointModelImpl(49.4002, 8.6));
        TrackLog trackLog = new TrackLog();
        trackLog.getTrackLogSegments().add(s1);

        TrackLogRefApproach refApproach = trackLog.getBestDistance(new PointModelImpl(49.4001, 8.6001));
        assertNotNull(refApproach);
        assertEquals(49.4001, refApproach.approachPoint.getLat(),0);
        assertEquals("trackLog.name=, segmentIdx=0, approachPoint=Lat=49.400100, Lon=8.600000, distance=7.24, endPointIndex=1", refApproach.toString());

        refApproach = trackLog.getBestDistance(new PointModelImpl(49.4001, 8.6001),2);
        assertNull(refApproach);
    }

    @Test
    public void getBestPoint() {
        PointModelUtil.init(32);
        TrackLogSegment s1 = new TrackLogSegment(1);
        s1.addPoint(new PointModelImpl(49.4, 8.6));
        s1.addPoint(new PointModelImpl(49.4002, 8.6));
        TrackLog trackLog = new TrackLog();
        trackLog.getTrackLogSegments().add(s1);

        TrackLogRefApproach refApproach = trackLog.getBestPoint(new PointModelImpl(49.40009, 8.6001), 32);
        assertNotNull(refApproach);
        assertEquals(0, refApproach.getEndPointIndex());
        refApproach = trackLog.getBestPoint(new PointModelImpl(49.40011, 8.6001), 32);
        assertNotNull(refApproach);
        assertEquals(1, refApproach.getEndPointIndex());

        refApproach = trackLog.getBestPoint(new PointModelImpl(49.4001, 8.6001),2);
        assertNull(refApproach);
    }

    @Test
    public void getRemainingDistance() {
        PointModelUtil.init(32);
        TrackLogSegment s1 = new TrackLogSegment(1);
        s1.addPoint(new PointModelImpl(49.4, 8.6));
        s1.addPoint(new PointModelImpl(49.4002, 8.6));
        s1.addPoint(new PointModelImpl(49.4004, 8.6));
        s1.addPoint(new PointModelImpl(49.4006, 8.6));
        TrackLog trackLog = new TrackLog();
        trackLog.getTrackLogSegments().add(s1);

        TrackLogRefApproach refApproach1 = trackLog.getBestDistance(new PointModelImpl(49.4001, 8.60001), 32);
        TrackLogRefApproach refApproach2 = trackLog.getBestDistance(new PointModelImpl(49.4005, 8.60001), 32);
        double ref1 = PointModelUtil.distance(new PointModelImpl(49.4001, 8.6),new PointModelImpl(49.4006, 8.6));
        double ref2 = PointModelUtil.distance(new PointModelImpl(49.4001, 8.6),new PointModelImpl(49.4005, 8.6));
        Assert.assertEquals(ref1, PointModelUtil.distance(trackLog.getPointList(refApproach1,null)),0);
        assertEquals(ref2, PointModelUtil.distance(trackLog.getPointList(refApproach1,refApproach2)),0);
        assertEquals(0, PointModelUtil.distance(trackLog.getPointList(refApproach2,refApproach1)),0);
    }


}