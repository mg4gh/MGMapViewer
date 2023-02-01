package mg.mgmap.generic.model;

import org.junit.Test;
import static org.junit.Assert.*;

public class MetaDataTest {

    @Test
    public void testConstants(){
        assertEquals(4096, MetaData.BUF_SIZE);
        assertEquals(340, MetaData.POINTS_PER_BUF);
    }

    @Test
    public void createMetaData(){
        MetaData metaData = new MetaData();
        assertNotEquals(null, metaData.bBox);
        assertEquals(0, metaData.numPoints);
        assertEquals(null, metaData.buf);
    }
}