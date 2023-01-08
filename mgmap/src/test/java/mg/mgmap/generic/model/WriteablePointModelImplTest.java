package mg.mgmap.generic.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WriteablePointModelImplTest {

    @Test
    void createWriteablePointModelImplTest1() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051);
        assertEquals(49608400, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( PointModel.NO_ELE, wpmi.ele);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }
    @Test
    void createWriteablePointModelImplTest2() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        assertEquals(49608400, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( 538.731f, wpmi.ele);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }
    @Test
    void createWriteablePointModelImplTest3() {
        PointModel pm = new PointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(pm);
        assertEquals(49608400, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( 538.731f, wpmi.ele);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }
    @Test
    void createWriteablePointModelImplTest4() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl();
        assertEquals(PointModel.NO_LAT_LONG*1000000, wpmi.la);
        assertEquals( PointModel.NO_LAT_LONG*1000000, wpmi.lo);
        assertEquals( PointModel.NO_ELE, wpmi.ele);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }

    @Test
    void setLat() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        wpmi.setLat(50.54321);
        assertEquals(50543210, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( 538.731f, wpmi.ele);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }

    @Test
    void setLon() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        wpmi.setLon(3.123456);
        assertEquals(49608400, wpmi.la);
        assertEquals( 3123456, wpmi.lo);
        assertEquals( 538.731f, wpmi.ele);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }

    @Test
    void setEle() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        wpmi.setEle(123.456f);
        assertEquals(49608400, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( 123.456f, wpmi.ele);
        assertEquals( PointModel.NO_TIME, wpmi.timestamp);
    }

    @Test
    void setTimestamp() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        long aTime=54324567343567753L;
        wpmi.setTimestamp(aTime);
        assertEquals(49608400, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( 538.731f, wpmi.ele);
        assertEquals( aTime, wpmi.timestamp);
    }

    @Test
    void getTimestamp() {
        WriteablePointModelImpl wpmi = new WriteablePointModelImpl(49.6084, 8.6051, 538.731f, 3.1f);
        long aTime=54324567343567753L;
        wpmi.setTimestamp(aTime);
        assertEquals(49608400, wpmi.la);
        assertEquals( 8605100, wpmi.lo);
        assertEquals( 538.731f, wpmi.ele);
        assertEquals( aTime, wpmi.getTimestamp());
    }

}