package mg.mgmap.generic.model;

import java.util.Iterator;

import org.junit.Test;
import static org.junit.Assert.*;

public class MultiPointModelImplTest {

    @Test
    public void createMultiPointModelImplTest() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        assertNotEquals(mpmi.points, null);
        assertFalse(mpmi.route);
    }

    @Test
    public void addPoint1() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        assertEquals(1, mpmi.points.get(0).getLat(),0);
        assertEquals(2, mpmi.points.get(1).getLat(),0);
        assertEquals(3, mpmi.points.get(2).getLat(),0);
        assertEquals(3, mpmi.points.size());
        assertEquals(3, mpmi.bBox.maxLatitude,0);
        assertEquals(3, mpmi.bBox.maxLongitude,0);
        assertEquals(1, mpmi.bBox.minLatitude,0);
        assertEquals(1, mpmi.bBox.minLongitude,0);
    }

    @Test
    public void addPoint2() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(1, new PointModelImpl(3,3));
        assertEquals(1, mpmi.points.get(0).getLat(),0);
        assertEquals(3, mpmi.points.get(1).getLat(),0);
        assertEquals(2, mpmi.points.get(2).getLat(),0);
        assertEquals(3, mpmi.points.size());

        assertEquals(3, mpmi.bBox.maxLatitude,0);
        assertEquals(3, mpmi.bBox.maxLongitude,0);
        assertEquals(1, mpmi.bBox.minLatitude,0);
        assertEquals(1, mpmi.bBox.minLongitude,0);
    }


    @Test
    public void removePoint1() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        boolean res = mpmi.removePoint(new PointModelImpl(2,2));
        assertTrue(res);
        assertEquals(1, mpmi.points.get(0).getLat(),0);
        assertEquals(3, mpmi.points.get(1).getLat(),0);
        assertEquals(2, mpmi.points.size());

        assertEquals(3, mpmi.bBox.maxLatitude,0);
        assertEquals(3, mpmi.bBox.maxLongitude,0);
        assertEquals(1, mpmi.bBox.minLatitude,0);
        assertEquals(1, mpmi.bBox.minLongitude,0);
    }

    @Test
    public void removePoint2() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        boolean res = mpmi.removePoint(new PointModelImpl(2,3));
        assertFalse(res);
        assertEquals(1, mpmi.points.get(0).getLat(),0);
        assertEquals(2, mpmi.points.get(1).getLat(),0);
        assertEquals(3, mpmi.points.get(2).getLat(),0);
        assertEquals(3, mpmi.points.size());

        assertEquals(3, mpmi.bBox.maxLatitude,0);
        assertEquals(3, mpmi.bBox.maxLongitude,0);
        assertEquals(1, mpmi.bBox.minLatitude,0);
        assertEquals(1, mpmi.bBox.minLongitude,0);
    }

    @Test
    public void removePoint3() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        boolean res = mpmi.removePoint(new PointModelImpl(3,3));
        assertTrue(res);
        assertEquals(1, mpmi.points.get(0).getLat(),0);
        assertEquals(2, mpmi.points.get(1).getLat(),0);
        assertEquals(2, mpmi.points.size());

        assertEquals(2, mpmi.bBox.maxLatitude,0);
        assertEquals(2, mpmi.bBox.maxLongitude,0);
        assertEquals(1, mpmi.bBox.minLatitude,0);
        assertEquals(1, mpmi.bBox.minLongitude,0);
    }

    @Test
    public void size() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        assertEquals(1, mpmi.points.get(0).getLat(),0);
        assertEquals(2, mpmi.points.get(1).getLat(),0);
        assertEquals(3, mpmi.points.get(2).getLat(),0);
        assertEquals(3, mpmi.size());
    }

    @Test
    public void get() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        assertEquals(1, mpmi.get(0).getLat(),0);
        assertEquals(2, mpmi.get(1).getLat(),0);
        assertEquals(3, mpmi.get(2).getLat(),0);
        assertEquals(3, mpmi.size());
    }

    @Test
    public void iterator() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        Iterator<PointModel> iterator = mpmi.iterator();
        assertEquals(1, iterator.next().getLat(),0);
        assertEquals(2, iterator.next().getLat(),0);
        assertEquals(3, iterator.next().getLat(),0);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void getBBox() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        BBox bBox = mpmi.getBBox();

        assertEquals(3, bBox.maxLatitude,0);
        assertEquals(3, bBox.maxLongitude,0);
        assertEquals(1, bBox.minLatitude,0);
        assertEquals(1, bBox.minLongitude,0);
    }

    @Test
    public void isRoute() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(3,3));
        assertFalse(mpmi.isRoute());
        mpmi.setRoute(true);
        assertTrue(mpmi.isRoute());
    }

    @Test
    public void setRoute() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(3,3));
        mpmi.setRoute(true);
        assertTrue(mpmi.route);
        mpmi.setRoute(false);
        assertFalse(mpmi.route);
    }
}