package mg.mgmap.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PointModelTest {

    @Test
    void testNoLatLong() {
        assertEquals(200, PointModel.NO_LAT_LONG);
    }
    @Test
    void testNoEle() {
        assertEquals(-20000, PointModel.NO_ELE);
    }
    @Test
    void testNoPres() {
        assertEquals(0, PointModel.NO_PRES);
    }
    @Test
    void testTime() {
        assertEquals(0, PointModel.NO_TIME);
    }
}