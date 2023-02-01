package mg.mgmap.generic.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class PointModelTest {

    @Test
    public void testNoLatLong() {
        assertEquals(200, PointModel.NO_LAT_LONG,0);
    }
    @Test
    public void testNoEle() {
        assertEquals(-20000, PointModel.NO_ELE,0);
    }
    @Test
    public void testNoPres() {
        assertEquals(0, PointModel.NO_PRES,0);
    }
    @Test
    public void testTime() {
        assertEquals(946771200000L, PointModel.NO_TIME);
    }
}