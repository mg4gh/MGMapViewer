package mg.mgmap.generic.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class TrackLogRefTest {


    @Test
    public void createTrackLogRef1() {
        TrackLogRef ref = new TrackLogRef();
        assertNull(ref.trackLog);
        assertEquals(-1, ref.segmentIdx);
    }

    @Test
    public void createTrackLogRef2() {
        TrackLog trackLog = new TrackLog();
        TrackLogRef ref = new TrackLogRef(trackLog,3);
        assertEquals(trackLog, ref.trackLog);
        assertEquals(3, ref.segmentIdx);
    }

    @Test
    public void testTrackLog() {
        TrackLogRef ref = new TrackLogRef();
        assertNull(ref.trackLog);

        TrackLog trackLog = new TrackLog();
        ref.setTrackLog(trackLog);
        assertEquals(trackLog, ref.trackLog);
        assertEquals(trackLog, ref.getTrackLog());
    }

    @Test
    public void getSegmentIdx() {
        TrackLog trackLog = new TrackLog();
        trackLog.getTrackLogSegments().add( new TrackLogSegment(0));
        trackLog.getTrackLogSegments().add( new TrackLogSegment(1));
        trackLog.getTrackLogSegments().add( new TrackLogSegment(2));
        TrackLogRef ref = new TrackLogRef(trackLog, 2);
        assertEquals(2, ref.getSegment().getSegmentIdx());
    }

    @Test
    public void setSegmentIdx() {
        TrackLogRef ref = new TrackLogRef();
        assertEquals(-1, ref.segmentIdx);
        ref.setSegmentIdx(5);
        assertEquals(5, ref.segmentIdx);
        assertEquals(5, ref.getSegmentIdx());
    }

    @Test
    public void testToString() {
        TrackLog trackLog = new TrackLog();
        trackLog.setName("abc");
        TrackLogRef ref = new TrackLogRef(trackLog,3);
        assertEquals("trackLog.name=abc, segmentIdx=3", ref.toString());
    }
}

