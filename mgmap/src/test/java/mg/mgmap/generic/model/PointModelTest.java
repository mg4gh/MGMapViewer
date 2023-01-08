package mg.mgmap.generic.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PointModelTest {

    @Test
    public void testNoLatLong() {
        assertEquals(200, PointModel.NO_LAT_LONG);
    }
    @Test
    public void testNoEle() {
        assertEquals(-20000, PointModel.NO_ELE);
    }
    @Test
    public void testNoPres() {
        assertEquals(0, PointModel.NO_PRES);
    }
    @Test
    public void testTime() {
        assertEquals(946771200000L, PointModel.NO_TIME);
    }
}