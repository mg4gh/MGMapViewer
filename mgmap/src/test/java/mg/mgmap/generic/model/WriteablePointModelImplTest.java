package mg.mgmap.generic.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class WriteablePointModelImplTest {

    @Test
    public void createWriteablePointModelImplTest1() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051);
        assertEquals(49608400, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( PointModel.NO_ELE, wpmi.ele,0);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }
    @Test
    public void createWriteablePointModelImplTest2() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        assertEquals(49608400, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( 538.731f, wpmi.ele,0);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }
    @Test
    public void createWriteablePointModelImplTest3() {
        PointModel pm = new PointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(pm);
        assertEquals(49608400, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( 538.731f, wpmi.ele,0);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }
    @Test
    public void createWriteablePointModelImplTest4() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl();
        assertEquals(PointModel.NO_LAT_LONG*1000000, wpmi.la,0f);
        assertEquals( PointModel.NO_LAT_LONG*1000000, wpmi.lo,0f);
        assertEquals( PointModel.NO_ELE, wpmi.ele,0);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }

    @Test
    public void setLat() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        wpmi.setLat(50.54321);
        assertEquals(50543210, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( 538.731f, wpmi.ele,0);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }

    @Test
    public void setLon() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        wpmi.setLon(3.123456);
        assertEquals(49608400, wpmi.la);
        assertEquals( 3123456, wpmi.lo);
        assertEquals( 538.731f, wpmi.ele,0);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }

    @Test
    public void setEle() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        wpmi.setEle(123.456f);
        assertEquals(49608400, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( 123.456f, wpmi.ele,0);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }

    @Test
    public void setTimestamp() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        long aTime=54324567343567753L;
        wpmi.setTimestamp(aTime);
        assertEquals(49608400, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( 538.731f, wpmi.ele,0);
        assertEquals( aTime, wpmi.timestamp);
    }

    @Test
    public void getTimestamp() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        long aTime=54324567343567753L;
        wpmi.setTimestamp(aTime);
        assertEquals(49608400, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( 538.731f, wpmi.ele,0);
        assertEquals( aTime, wpmi.getTimestamp());
    }

}