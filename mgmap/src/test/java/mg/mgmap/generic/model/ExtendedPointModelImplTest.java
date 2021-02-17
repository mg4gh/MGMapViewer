package mg.mgmap.generic.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExtendedPointModelImplTest {

    @Test
    void createExtendedPointModelImplTest1() {
        ExtendedPointModelImpl<Double> epmi = new ExtendedPointModelImpl<>(49.6084, 8.6051, 538.731f, 555.5555);
        assertEquals(49608400, epmi.la);
        assertEquals( 8605100, epmi.lo);
        assertEquals( 538.731f, epmi.ele);
        assertEquals( new Double(555.5555), epmi.getExtent());
    }
    @Test
    void createExtendedPointModelImplTest2() {
        ExtendedPointModelImpl<String> epmi = new ExtendedPointModelImpl<>(49.6084, 8.6051, 538.731f, "555.5555");
        assertEquals(49608400, epmi.la);
        assertEquals( 8605100, epmi.lo);
        assertEquals( 538.731f, epmi.ele);
        assertEquals( "555.5555", epmi.getExtent());
    }
    @Test
    void createExtendedPointModelImplTest3() {
        PointModel pm = new PointModelImpl(49.6084, 8.6051, 538.731f);
        ExtendedPointModelImpl<String> epmi = new ExtendedPointModelImpl<>(pm, "555.5556");
        assertEquals(49608400, epmi.la);
        assertEquals( 8605100, epmi.lo);
        assertEquals( 538.731f, epmi.ele);
        assertEquals( "555.5556", epmi.getExtent());
    }
}