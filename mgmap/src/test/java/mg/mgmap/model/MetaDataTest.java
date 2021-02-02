package mg.mgmap.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetaDataTest {

    @Test
    void testConstants(){
        assertEquals(4096, MetaData.BUF_SIZE);
        assertEquals(340, MetaData.POINTS_PER_BUF);
    }

    @Test
    void createMetaData(){
        MetaData metaData = new MetaData();
        assertNotEquals(null, metaData.bBox);
        assertEquals(0, metaData.numPoints);
        assertEquals(null, metaData.buf);
    }
}