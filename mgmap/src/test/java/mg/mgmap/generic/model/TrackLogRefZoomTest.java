package mg.mgmap.generic.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class TrackLogRefZoomTest {


    @Test
    public void createTrackLogRefZoom() {
        TrackLog trackLog = new TrackLog();
        TrackLogRefZoom ref = new TrackLogRefZoom(trackLog, 3, true);
        assertEquals(trackLog, ref.trackLog);
        assertEquals(3, ref.segmentIdx);
        assertEquals(true, ref.zoomForBB);
    }
    @Test
    public void testZoomForBB() {
        TrackLog trackLog = new TrackLog();
        TrackLogRefZoom ref = new TrackLogRefZoom(trackLog, 3, false);
        assertEquals(trackLog, ref.trackLog);
        assertEquals(3, ref.segmentIdx);
        assertEquals(false, ref.zoomForBB);
        assertEquals(false, ref.isZoomForBB());

        ref.setZoomForBB(true);
        assertEquals(true, ref.isZoomForBB());
    }

}