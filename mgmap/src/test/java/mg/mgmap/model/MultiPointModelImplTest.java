package mg.mgmap.model;

import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MultiPointModelImplTest {

    @Test
    void createMultiPointModelImplTest() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        assertNotEquals(mpmi.points, null);
        assertFalse(mpmi.route);
    }

    @Test
    void addPoint1() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        assertEquals(1, mpmi.points.get(0).getLat());
        assertEquals(2, mpmi.points.get(1).getLat());
        assertEquals(3, mpmi.points.get(2).getLat());
        assertEquals(3, mpmi.points.size());
        assertEquals(3, mpmi.bBox.maxLatitude);
        assertEquals(3, mpmi.bBox.maxLongitude);
        assertEquals(1, mpmi.bBox.minLatitude);
        assertEquals(1, mpmi.bBox.minLongitude);
    }

    @Test
    void addPoint2() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(1, new PointModelImpl(3,3));
        assertEquals(1, mpmi.points.get(0).getLat());
        assertEquals(3, mpmi.points.get(1).getLat());
        assertEquals(2, mpmi.points.get(2).getLat());
        assertEquals(3, mpmi.points.size());

        assertEquals(3, mpmi.bBox.maxLatitude);
        assertEquals(3, mpmi.bBox.maxLongitude);
        assertEquals(1, mpmi.bBox.minLatitude);
        assertEquals(1, mpmi.bBox.minLongitude);
    }


    @Test
    void removePoint1() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        boolean res = mpmi.removePoint(new PointModelImpl(2,2));
        assertTrue(res);
        assertEquals(1, mpmi.points.get(0).getLat());
        assertEquals(3, mpmi.points.get(1).getLat());
        assertEquals(2, mpmi.points.size());

        assertEquals(3, mpmi.bBox.maxLatitude);
        assertEquals(3, mpmi.bBox.maxLongitude);
        assertEquals(1, mpmi.bBox.minLatitude);
        assertEquals(1, mpmi.bBox.minLongitude);
    }

    @Test
    void removePoint2() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        boolean res = mpmi.removePoint(new PointModelImpl(2,3));
        assertFalse(res);
        assertEquals(1, mpmi.points.get(0).getLat());
        assertEquals(2, mpmi.points.get(1).getLat());
        assertEquals(3, mpmi.points.get(2).getLat());
        assertEquals(3, mpmi.points.size());

        assertEquals(3, mpmi.bBox.maxLatitude);
        assertEquals(3, mpmi.bBox.maxLongitude);
        assertEquals(1, mpmi.bBox.minLatitude);
        assertEquals(1, mpmi.bBox.minLongitude);
    }

    @Test
    void removePoint3() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        boolean res = mpmi.removePoint(new PointModelImpl(3,3));
        assertTrue(res);
        assertEquals(1, mpmi.points.get(0).getLat());
        assertEquals(2, mpmi.points.get(1).getLat());
        assertEquals(2, mpmi.points.size());

        assertEquals(2, mpmi.bBox.maxLatitude);
        assertEquals(2, mpmi.bBox.maxLongitude);
        assertEquals(1, mpmi.bBox.minLatitude);
        assertEquals(1, mpmi.bBox.minLongitude);
    }

    @Test
    void size() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        assertEquals(1, mpmi.points.get(0).getLat());
        assertEquals(2, mpmi.points.get(1).getLat());
        assertEquals(3, mpmi.points.get(2).getLat());
        assertEquals(3, mpmi.size());
    }

    @Test
    void get() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        assertEquals(1, mpmi.get(0).getLat());
        assertEquals(2, mpmi.get(1).getLat());
        assertEquals(3, mpmi.get(2).getLat());
        assertEquals(3, mpmi.size());
    }

    @Test
    void iterator() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        Iterator<PointModel> iterator = mpmi.iterator();
        assertEquals(1, iterator.next().getLat());
        assertEquals(2, iterator.next().getLat());
        assertEquals(3, iterator.next().getLat());
        assertFalse(iterator.hasNext());
    }

    @Test
    void getBBox() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(2,2));
        mpmi.addPoint(new PointModelImpl(3,3));
        BBox bBox = mpmi.getBBox();

        assertEquals(3, bBox.maxLatitude);
        assertEquals(3, bBox.maxLongitude);
        assertEquals(1, bBox.minLatitude);
        assertEquals(1, bBox.minLongitude);
    }

    @Test
    void isRoute() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(3,3));
        assertFalse(mpmi.isRoute());
        mpmi.setRoute(true);
        assertTrue(mpmi.isRoute());
    }

    @Test
    void setRoute() {
        MultiPointModelImpl mpmi = new MultiPointModelImpl();
        mpmi.addPoint(new PointModelImpl(1,1));
        mpmi.addPoint(new PointModelImpl(3,3));
        mpmi.setRoute(true);
        assertTrue(mpmi.route);
        mpmi.setRoute(false);
        assertFalse(mpmi.route);
    }
}