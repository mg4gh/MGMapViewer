package mg.mgmap.generic.model;

import java.util.ArrayList;
import java.util.Iterator;

import org.junit.Test;
import static org.junit.Assert.*;

public class TrackLogSegmentTest {

    @Test
    public void createTrackLogSegment() {
        TrackLogSegment segment = new TrackLogSegment(3);
        assertEquals(3,segment.getSegmentIdx());
        assertNotNull(segment.getStatistic());
        assertNotNull(segment.getMetaDatas());
        assertEquals(3,segment.getStatistic().getSegmentIdx());
        assertEquals(0,segment.getMetaDatas().size());
    }

    @Test
    public void addPoint() {
        TrackLogSegment segment = new TrackLogSegment(3);
        assertEquals(0, segment.size());
        assertEquals(0,segment.getStatistic().getNumPoints());
        PointModel pm1 = new PointModelImpl(49.4, 8.6);
        segment.addPoint(pm1);
        assertEquals(1, segment.size());
        assertEquals(1,segment.getStatistic().getNumPoints());
        PointModel pm2 = new PointModelImpl(49.5, 8.7);
        segment.addPoint(pm2);
        assertEquals(2, segment.size());
        assertEquals(2,segment.getStatistic().getNumPoints());
        assertEquals(pm2, segment.get(1));
        PointModel pm3 = new PointModelImpl(49.6, 8.8);
        segment.addPoint(0, pm3);
        assertEquals(3, segment.size());
        assertEquals(3,segment.getStatistic().getNumPoints());
        assertEquals(pm3, segment.get(0));
        assertEquals(pm2, segment.get(2));

        assertEquals(pm2, segment.getLastPoint());
    }

    @Test
    public void getLastPoint() {
        TrackLogSegment segment = new TrackLogSegment(3);
        assertEquals(null, segment.getLastPoint());

    }

    @Test
    public void iterator() {
        TrackLogSegment segment = new TrackLogSegment(3);
        PointModel pm1 = new PointModelImpl(49.4, 8.6);
        segment.addPoint(pm1);
        PointModel pm2 = new PointModelImpl(49.5, 8.7);
        segment.addPoint(pm2);
        Iterator<PointModel> iter = segment.iterator();
        ArrayList<PointModel> pts = new ArrayList<>();
        pts.add(pm1);
        pts.add(pm2);
        while (iter.hasNext()){
            assertEquals(pts.remove(0), iter.next());
        }

    }

    @Test
    public void setStatistic() {
        TrackLogSegment segment = new TrackLogSegment(3);
        TrackLogStatistic stat = new TrackLogStatistic(5);
        segment.setStatistic(stat);
        assertEquals(5, segment.getStatistic().getSegmentIdx());
    }

    @Test
    public void getBBox() {
        TrackLogSegment segment = new TrackLogSegment(3);
        PointModel pm1 = new PointModelImpl(49.4, 8.6);
        segment.addPoint(pm1);
        PointModel pm2 = new PointModelImpl(49.5, 8.7);
        segment.addPoint(pm2);
        assertEquals(49.4, segment.getBBox().minLatitude,0);
    }

    @Test
    public void recalcStatistic() {
        TrackLogSegment segment = new TrackLogSegment(3);
        WriteablePointModel pm1 = new WriteablePointModelImpl(49.4, 8.6);
        segment.addPoint(pm1);
        WriteablePointModel pm2 = new WriteablePointModelImpl(49.4001, 8.6001);
        segment.addPoint(pm2);

        assertTrue( (Math.abs(segment.getStatistic().getTotalLength())-13.282) < 0.01);

        pm2.setLat(49.4002);
        pm2.setLon(8.6002);
        segment.recalcStatistic();
        assertTrue( (Math.abs(segment.getStatistic().getTotalLength())-26.563) < 0.01);
    }

}