package mg.mgmap.generic.model;

import org.mapsforge.core.model.LatLong;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

import org.junit.Test;
import static org.junit.Assert.*;

public class PointModelImplTest {

    @Test
    public void test_createPointModelImpl1(){
        PointModelImpl pmi = new PointModelImpl(49.6084, 8.6051);
        assertEquals(49608400, pmi.la);
        assertEquals( 8605100, pmi.lo);
        assertEquals( PointModel.NO_ELE, pmi.ele,0);
    }

    @Test
    public void createPointModelImpl2(){
        PointModelImpl pmi = new PointModelImpl(49.6084136, 8.6051234);
        assertEquals(49608414, pmi.la);
        assertEquals( 8605123, pmi.lo);
        assertEquals( PointModel.NO_ELE, pmi.ele,0);
    }
    @Test
    public void createPointModelImpl3(){
        PointModelImpl pmi = new PointModelImpl(49.60841449, 8.60512351);
        assertEquals(49608414, pmi.la);
        assertEquals( 8605124, pmi.lo);
        assertEquals( PointModel.NO_ELE, pmi.ele,0);
    }
    @Test
    public void createPointModelImpl4(){
        PointModelImpl pmi = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        assertEquals(49608414, pmi.la);
        assertEquals( 8605124, pmi.lo);
        assertEquals( 17.8652f, pmi.ele,0);
        assertEquals( 3.1f, pmi.eleAcc,0);
    }


    @Test
    public void createPointModelImpl6(){
        PointModelImpl pmi = new PointModelImpl();
        assertEquals(PointModel.NO_LAT_LONG*1000000, pmi.la,0);
        assertEquals( PointModel.NO_LAT_LONG*1000000, pmi.lo,0);
        assertEquals( PointModel.NO_ELE, pmi.ele,0);
    }
    @Test
    public void createPointModelImpl7(){
        PointModelImpl pmi = new PointModelImpl(new LatLong(49.60841449, 8.60512351));
        assertEquals(49608414, pmi.la);
        assertEquals( 8605124, pmi.lo);
        assertEquals( PointModel.NO_ELE, pmi.ele,0);
    }


    @Test
    public void createFromLaLo1() {
        PointModelImpl pmi = PointModelImpl.createFromLaLo(49608414, 8605124);
        assertEquals(49608414, pmi.la);
        assertEquals( 8605124, pmi.lo);
        assertEquals( PointModel.NO_ELE, pmi.ele,0);
    }
    @Test
    public void createFromLaLo2() {
        PointModelImpl pmi = PointModelImpl.createFromLaLo((49608414L<<32)+8605124);
        assertEquals(49608414, pmi.la);
        assertEquals( 8605124, pmi.lo);
        assertEquals( PointModel.NO_ELE, pmi.ele,0);
    }


    @Test
    public void getLat() {
        PointModelImpl pmi = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        assertEquals(49.608414, pmi.getLat(),0);
    }

    @Test
    public void getLon() {
        PointModelImpl pmi = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        assertEquals(8.605124, pmi.getLon(),0);
    }

    @Test
    public void getEleA() {
        PointModelImpl pmi = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        assertEquals(17.8652f, pmi.getEleA(),0);
    }

    @Test
    public void getEleD() {
        PointModelImpl pmi = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        assertEquals(17.8652f, pmi.getEleD(),0);
    }

    @Test
    public void getTimestamp() {
        PointModelImpl pmi = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        assertEquals(PointModel.NO_TIME, pmi.getTimestamp());
    }

    @Test
    public void testToString1() {
        PointModelImpl pmi = new PointModelImpl(49.60841449, 8.60512351);
        assertEquals("Lat=49.608414, Lon=8.605124", pmi.toString());
    }
    @Test
    public void testToString2() {
        PointModelImpl pmi = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        assertEquals("Lat=49.608414, Lon=8.605124, Ele=17.9m", pmi.toString());
    }

    @Test
    public void toBuf() {
        PointModelImpl pmi2 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        ByteBuffer buf = ByteBuffer.allocate(12);
        pmi2.toBuf(buf);

        byte[] b = buf.array();
        ByteArrayInputStream bin = new ByteArrayInputStream(b);
        assertEquals((49608414>>24) & 0xFF, bin.read());
        assertEquals(((49608414<<8)>>24) & 0xFF, bin.read() );
        assertEquals(((49608414<<16)>>24) & 0xFF, bin.read() );
        assertEquals(((49608414<<24)>>24) & 0xFF, bin.read() );
        assertEquals((8605124>>24) & 0xFF, bin.read());
        assertEquals(((8605124<<8)>>24) & 0xFF, bin.read() );
        assertEquals(((8605124<<16)>>24) & 0xFF, bin.read() );
        assertEquals(((8605124<<24)>>24) & 0xFF, bin.read() );
        assertEquals((65) & 0xFF, bin.read() );
        assertEquals((142) & 0xFF, bin.read() );
        assertEquals((235) & 0xFF, bin.read() );
        assertEquals((238) & 0xFF, bin.read() );
    }

    @Test
    public void fromBuf() {
        PointModelImpl pmi2 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        ByteBuffer buf = ByteBuffer.allocate(12);
        pmi2.toBuf(buf); // is already tested, so we can use it
        buf.rewind();
        PointModelImpl pmi = new PointModelImpl();
        pmi.fromBuf(buf);
        assertEquals(49608414, pmi.la);
        assertEquals( 8605124, pmi.lo);
        assertEquals( 17.8652f, pmi.ele,0);
    }

    @Test
    public void compareTo1() {
        PointModelImpl pmi1 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        PointModelImpl pmi2 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        assertEquals(0, pmi1.compareTo(pmi2));
    }
    @Test
    public void compareTo2() {
        PointModelImpl pmi1 = new PointModelImpl(47.60841449, 8.60512351, 17.8652f, 3.1f);
        PointModelImpl pmi2 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        assertEquals(-1, pmi1.compareTo(pmi2));
    }
    @Test
    public void compareTo3() {
        PointModelImpl pmi1 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        PointModelImpl pmi2 = new PointModelImpl(49.60841449, 8.60512351, 18.8652f, 3.1f);
        assertEquals(0, pmi1.compareTo(pmi2));
    }

    @Test
    public void testEquals1() {
        PointModelImpl pmi1 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        PointModelImpl pmi2 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        assertTrue(pmi1.equals(pmi2));
    }
    @Test
    public void testEquals2() {
        PointModelImpl pmi1 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        PointModelImpl pmi2 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        assertFalse(pmi1.equals(null));
    }
    @Test
    public void testEquals3() {
        PointModelImpl pmi1 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        PointModelImpl pmi2 = new PointModelImpl(49.60841449, 8.60512351, 17.8653f, 3.1f);
        assertFalse(pmi1.equals(pmi2));
    }

    @Test
    public void laMdDiff1() {
        PointModelImpl pmi1 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        PointModelImpl pmi2 = new PointModelImpl(49.60841449, 8.60512351, 17.8653f, 3.1f);
        assertEquals(0, pmi1.laMdDiff(pmi2));
    }
    @Test
    public void laMdDiff2() {
        PointModelImpl pmi1 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        PointModelImpl pmi2 = new PointModelImpl(49.60841749, 8.60512351, 17.8653f, 3.1f);
        assertEquals(3, pmi1.laMdDiff(pmi2));
    }

    @Test
    public void loMdDiff1() {
        PointModelImpl pmi1 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        PointModelImpl pmi2 = new PointModelImpl(49.60841449, 8.60512351, 17.8653f, 3.1f);
        assertEquals(0, pmi1.loMdDiff(pmi2));
    }
    @Test
    public void loMdDiff2() {
        PointModelImpl pmi1 = new PointModelImpl(49.60841449, 8.60512351, 17.8652f, 3.1f);
        PointModelImpl pmi2 = new PointModelImpl(49.60841449, 8.60512851, 17.8653f, 3.1f);
        assertEquals(5, pmi1.loMdDiff(pmi2));
    }

    @Test
    public void getLaLo() {
        long along = ((49608414L<<32)+8605124);
        PointModelImpl pmi = PointModelImpl.createFromLaLo(along);
        assertEquals(along, pmi.getLaLo());
    }
}