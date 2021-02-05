package mg.mgmap.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrackLogRefZoomTest {


    @Test
    void createTrackLogRefZoom() {
        TrackLog trackLog = new TrackLog();
        TrackLogRefZoom ref = new TrackLogRefZoom(trackLog, 3, true);
        assertEquals(trackLog, ref.trackLog);
        assertEquals(3, ref.segmentIdx);
        assertEquals(true, ref.zoomForBB);
    }
    @Test
    void testZoomForBB() {
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