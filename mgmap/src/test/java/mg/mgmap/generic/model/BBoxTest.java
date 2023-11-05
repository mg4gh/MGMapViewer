package mg.mgmap.generic.model;

import org.junit.Test;
import static org.junit.Assert.*;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;

import java.nio.ByteBuffer;
import java.util.ArrayList;


public class BBoxTest {


    @Test
    public void createBBox() {
        BBox bBox = new BBox();
        assertEquals(PointModel.NO_LAT_LONG, bBox.minLatitude,0);
        assertEquals(PointModel.NO_LAT_LONG,bBox.minLongitude,0);
        assertEquals(-PointModel.NO_LAT_LONG,bBox.maxLatitude,0);
        assertEquals(-PointModel.NO_LAT_LONG,bBox.maxLongitude,0);
    }

    @Test
    public void clear() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,3));
        bBox.clear();
        assertEquals(PointModel.NO_LAT_LONG, bBox.minLatitude,0);
        assertEquals(PointModel.NO_LAT_LONG,bBox.minLongitude,0);
        assertEquals(-PointModel.NO_LAT_LONG,bBox.maxLatitude,0);
        assertEquals(-PointModel.NO_LAT_LONG,bBox.maxLongitude,0);
    }

    @Test
    public void isInitial() {
        BBox bBox = new BBox();
        assertTrue(bBox.isInitial());
        bBox.extend(new PointModelImpl(2,3));
        assertFalse(bBox.isInitial());
        bBox.clear();
        assertTrue(bBox.isInitial());
    }

    @Test
    public void extend1() {
        BBox bBox = new BBox().extend(new PointModelImpl(2,3));
        assertEquals(2,bBox.minLatitude,0);
        assertEquals(3,bBox.minLongitude,0);
        assertEquals(2,bBox.maxLatitude,0);
        assertEquals(3,bBox.maxLongitude,0);
    }
    @Test
    public void extend2() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,3))
                .extend(new PointModelImpl(4,1));
        assertEquals(2,bBox.minLatitude,0);
        assertEquals(1,bBox.minLongitude,0);
        assertEquals(4,bBox.maxLatitude,0);
        assertEquals(3,bBox.maxLongitude,0);
    }


    @Test
    public void extend3() {
        BBox bBox = new BBox();
        bBox.extend(new PointModelImpl(49,8));
        bBox.extend(100); //meter

        assertEquals(48.999102,bBox.minLatitude,0);
        assertEquals(7.998631,bBox.minLongitude,0);
        assertEquals(49.000898,bBox.maxLatitude,0);
        assertEquals(8.001369,bBox.maxLongitude,0);
    }

    @Test
    public void extend4() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,3))
                .extend(new LatLong(4,1));
        assertEquals(2,bBox.minLatitude,0);
        assertEquals(1,bBox.minLongitude,0);
        assertEquals(4,bBox.maxLatitude,0);
        assertEquals(3,bBox.maxLongitude,0);
    }

    @Test
    public void extend5() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(4,1))
                .extend(new BBox().extend(new PointModelImpl(2,3)));
        assertEquals(2,bBox.minLatitude,0);
        assertEquals(1,bBox.minLongitude,0);
        assertEquals(4,bBox.maxLatitude,0);
        assertEquals(3,bBox.maxLongitude,0);
    }

    @Test
    public void extend6() {
        ArrayList<PointModel> pms = new ArrayList<>();
        pms.add( new PointModelImpl(4,5) );
        pms.add( new PointModelImpl(-3,-1) );
        BBox bBox = new BBox().extend(pms);
        assertEquals(-3,bBox.minLatitude,0);
        assertEquals(-1,bBox.minLongitude,0);
        assertEquals(4,bBox.maxLatitude,0);
        assertEquals(5,bBox.maxLongitude,0);
    }


    @Test
    public void intersects1() {
        BBox bBox1 = new BBox()
                .extend(new PointModelImpl(2,3))
                .extend(new PointModelImpl(4,1));
        BBox bBox2 = new BBox()
                .extend(new PointModelImpl(3,4))
                .extend(new PointModelImpl(5,2));
        assertTrue(bBox1.intersects(bBox2));
    }

    @Test
    public void intersects2() {
        BBox bBox1 = new BBox()
                .extend(new PointModelImpl(2,3))
                .extend(new PointModelImpl(4,1));
        BBox bBox2 = new BBox()
                .extend(new PointModelImpl(4,4))
                .extend(new PointModelImpl(5,3));
        assertTrue(bBox1.intersects(bBox2));
    }

    @Test
    public void intersects3() {
        BBox bBox1 = new BBox()
                .extend(new PointModelImpl(2,3))
                .extend(new PointModelImpl(4,1));
        BBox bBox2 = new BBox()
                .extend(new PointModelImpl(4.01,4))
                .extend(new PointModelImpl(5,3.01));
        assertFalse(bBox1.intersects(bBox2));
    }

    @Test
    public void intersects4() {
        BBox bBox1 = new BBox()
                .extend(new PointModelImpl(2,3))
                .extend(new PointModelImpl(4,1));
        BoundingBox boundingBox1 = new BoundingBox(4.01, 3, 5 , 4 );
        BoundingBox boundingBox2 = new BoundingBox(4.0, 3, 5 , 4 );
        assertFalse(bBox1.intersects(boundingBox1));
        assertTrue(bBox1.intersects(boundingBox2));
    }

    @Test
    public void contains() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,3))
                .extend(new PointModelImpl(4,1));

        assertTrue(bBox.contains(3,2));
    }
    @Test
    public void contains1() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,3))
                .extend(new PointModelImpl(4,1));
        assertFalse(bBox.contains(3,3.01));
        assertFalse(bBox.contains(3,0.99));
        assertFalse(bBox.contains(1.99,3));
        assertFalse(bBox.contains(4.01,3));
        assertTrue(bBox.contains(4,3));
        assertTrue(bBox.contains( new PointModelImpl(4,1)));
        assertTrue(bBox.contains(2,1));
        assertTrue(bBox.contains(2,3));
        assertTrue(bBox.contains(3,2));
    }
    @Test
    public void contains2() {
        BBox bBox1 = new BBox()
                .extend(new PointModelImpl(2,3))
                .extend(new PointModelImpl(4,1));
        BBox bBox2 = new BBox()
                .extend(new PointModelImpl(4,4))
                .extend(new PointModelImpl(5,3));
        assertFalse(bBox1.contains(bBox2));
    }
    @Test
    public void contains3() {
        BBox bBox1 = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        BBox bBox2 = new BBox()
                .extend(new PointModelImpl(3,2))
                .extend(new PointModelImpl(5,4));
        assertTrue(bBox1.contains(bBox2));
    }
    @Test
    public void contains4() {
        BBox bBox1 = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        BBox bBox2 = new BBox()
                .extend(new PointModelImpl(3,2))
                .extend(new PointModelImpl(5,5));
        assertTrue(bBox1.contains(bBox2));
    }
    @Test
    public void contains5() {
        BBox bBox1 = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        BBox bBox2 = new BBox()
                .extend(new PointModelImpl(3,2))
                .extend(new PointModelImpl(5,5.01));
        assertFalse(bBox1.contains(bBox2));
    }

    @Test
    public void contains6() {
        BBox bBox1 = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        BoundingBox boundingBox1 = new BoundingBox(3, 2, 5 , 5 );
        BoundingBox boundingBox2 = new BoundingBox(3, 2, 5 , 5.01 );
        assertTrue(bBox1.contains(boundingBox1));
        assertFalse(bBox1.contains(boundingBox2));
    }



    @Test
    public void fromBoundingBox() {
        BoundingBox boundingBox1 = new BoundingBox(3, 2, 5 , 6 );
        BBox bBox = BBox.fromBoundingBox(boundingBox1);
        assertEquals(3,bBox.minLatitude,0);
        assertEquals(2,bBox.minLongitude,0);
        assertEquals(5,bBox.maxLatitude,0);
        assertEquals(6,bBox.maxLongitude,0);
    }

    @Test
    public void getCenter() {
        BBox bBox1 = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        PointModel center = bBox1.getCenter();
        assertEquals(4,center.getLat(), 0);
        assertEquals(3,center.getLon(), 0);
    }

    @Test
    public void clip1() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        WriteablePointModel res = new WriteablePointModelImpl();
        bBox.clip(4,3,7,9, res);
        assertEquals(5,res.getLat(),0);
        assertEquals(5,res.getLon(),0);
    }
    @Test
    public void clip2() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        WriteablePointModel res = new WriteablePointModelImpl();
        bBox.clip(4,3,12,7, res);
        assertEquals(6,res.getLat(),0);
        assertEquals(4,res.getLon(),0);
    }
    @Test
    public void clip3() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        WriteablePointModel res = new WriteablePointModelImpl();
        bBox.clip(4,3,-4,7, res);
        assertEquals(2,res.getLat(),0);
        assertEquals(4,res.getLon(),0);
    }
    @Test
    public void clip4() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        WriteablePointModel res = new WriteablePointModelImpl();
        bBox.clip(4,3,-4,-1, res);
        assertEquals(2,res.getLat(),0);
        assertEquals(2,res.getLon(),0);
    }
    @Test
    public void clip5() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        WriteablePointModel res = new WriteablePointModelImpl();
        bBox.clip(4,3,-4,-13, res);
        assertEquals(3,res.getLat(),0);
        assertEquals(1,res.getLon(),0);
    }
    @Test
    public void clip6() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        WriteablePointModel res = new WriteablePointModelImpl();
        bBox.clip(4,3,12,-13, res);
        assertEquals(5,res.getLat(),0);
        assertEquals(1,res.getLon(),0);
    }
    @Test
    public void clip7() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        WriteablePointModel res = new WriteablePointModelImpl();
        bBox.clip(4,3,12,-1, res);
        assertEquals(6,res.getLat(),0);
        assertEquals(2,res.getLon(),0);
    }

    @SuppressWarnings({"IntegerMultiplicationImplicitCastToLong", "PointlessArithmeticExpression"})
    @Test
    public void toByteBuffer() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        ByteBuffer buf = ByteBuffer.allocate(16);
        bBox.toByteBuffer(buf);
        assertEquals(16,buf.position());

        byte[] b = buf.array();
        assertEquals(2*1000000, ((b[0] & 0xFF)<<24)+((b[1] & 0xFF)<<16)+((b[2] & 0xFF)<<8)+((b[3] & 0xFF)) );
        assertEquals(6*1000000, ((b[4] & 0xFF)<<24)+((b[5] & 0xFF)<<16)+((b[6] & 0xFF)<<8)+((b[7] & 0xFF)) );
        assertEquals(1*1000000, ((b[8] & 0xFF)<<24)+((b[9] & 0xFF)<<16)+((b[10] & 0xFF)<<8)+((b[11] & 0xFF)) );
        assertEquals(5*1000000, ((b[12] & 0xFF)<<24)+((b[13] & 0xFF)<<16)+((b[14] & 0xFF)<<8)+((b[15] & 0xFF)) );
    }

    @Test
    public void fromByteBuffer() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        ByteBuffer buf = ByteBuffer.allocate(16);
        bBox.toByteBuffer(buf);
        assertEquals(16,buf.position());

        buf.rewind();
        BBox bBox2 = new BBox();
        bBox2.fromByteBuffer(buf);
        assertEquals(bBox.minLatitude, bBox2.minLatitude,0);
        assertEquals(bBox.maxLatitude, bBox2.maxLatitude,0);
        assertEquals(bBox.minLongitude, bBox2.minLongitude,0);
        assertEquals(bBox.maxLongitude, bBox2.maxLongitude,0);
    }

    @Test
    public void testToString() {
        BBox bBox = new BBox()
                .extend(new PointModelImpl(2,5))
                .extend(new PointModelImpl(6,1));
        assertEquals("minLat=2.000000, minLon=1.000000, maxLat=6.000000, maxLon=5.000000",bBox.toString());
    }
}