package mg.mgmap.generic.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class ExtendedPointModelImplTest {

    @Test
    public void createExtendedPointModelImplTest1() {
        ExtendedPointModelImpl<Double> epmi = new ExtendedPointModelImpl<>(49.6084, 8.6051, 538.731f, 0, 555.5555);
        assertEquals(49608400, epmi.la);
        assertEquals( 8605100, epmi.lo);
        assertEquals( 538.731f, epmi.ele, 0);
        assertEquals( new Double(555.5555), epmi.getExtent());
    }
    @Test
    public void createExtendedPointModelImplTest2() {
        ExtendedPointModelImpl<String> epmi = new ExtendedPointModelImpl<>(49.6084, 8.6051, 538.731f, 0, "555.5555");
        assertEquals(49608400, epmi.la);
        assertEquals( 8605100, epmi.lo);
        assertEquals( 538.731f, epmi.ele,0);
        assertEquals( "555.5555", epmi.getExtent());
    }
    @Test
    public void createExtendedPointModelImplTest3() {
        PointModel pm = new PointModelImpl(49.6084, 8.6051, 538.731f, 0);
        ExtendedPointModelImpl<String> epmi = new ExtendedPointModelImpl<>(pm, "555.5556");
        assertEquals(49608400, epmi.la);
        assertEquals( 8605100, epmi.lo);
        assertEquals( 538.731f, epmi.ele,0);
        assertEquals( "555.5556", epmi.getExtent());
    }
}