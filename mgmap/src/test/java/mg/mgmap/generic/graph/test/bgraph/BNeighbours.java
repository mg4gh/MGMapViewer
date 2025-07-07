package mg.mgmap.generic.graph.test.bgraph;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;

import mg.mgmap.generic.util.basic.MGLog;

public class BNeighbours {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final static int NEIGHBOUR_SIZE =
            2   // neighbourPointIndex (as short)
                    + 1 // 4 bit neighbour tile selector (1 bytes)
                    + 1 // reverse flag (1 bytes)
                    + 2 // next neighbour index
                    + 2 // way attributes index
                    + 4 // distance as float
                    + 4; // cost as a float
    final static int NEIGHBOUR_POINT_OFFSET = 0;
    final static int NEIGHBOUR_FLAG_TILE_SELECTOR_OFFSET = 2;
    final static int NEIGHBOUR_FLAG_PRIMARY_OFFSET = 3;
    final static int NEIGHBOUR_NEXT_NEIGHBOUR_OFFSET = 4;
    final static int NEIGHBOUR_WAY_ATTRIBUTES_OFFSET = 6;
    final static int NEIGHBOUR_DISTANCE_OFFSET = 8;
    final static int NEIGHBOUR_COST_OFFSET = 12;

    final static byte PRIMARY_NA = 0; // not applicable
    final static byte PRIMARY_YES = 1;
    final static byte PRIMARY_NO = -1;


    ByteBuffer bbNeighbours;
    short neighboursUsed = 1;

    void init(ByteBuffer bbNeighbours){
        this.bbNeighbours = bbNeighbours;
    }


    public short createNeighbour(short wayAttributesIdx, short neighbourPointIdx, float distance, byte tileSelector, byte primary){
        short nIdx = neighboursUsed++;
        bbNeighbours.position(NEIGHBOUR_SIZE *nIdx);
        bbNeighbours.putShort(neighbourPointIdx);
        bbNeighbours.put(tileSelector); // NEIGHBOUR_FLAG_TILE_SELECTOR_OFFSET
        bbNeighbours.put(primary); // NEIGHBOUR_FLAG_PRIMARY_OFFSET
        bbNeighbours.putShort((short)0); // next neighbour index
        bbNeighbours.putShort(wayAttributesIdx);
        bbNeighbours.putFloat(distance);
        bbNeighbours.putFloat(-1); // cost
//        mgLog.d(String.format(Locale.ENGLISH, "createNeighbour neighbourIdx=%d to neighbourPointIdx=%d ",nIdx,neighbourPointIdx ));
        return nIdx;
    }


    public short getNeighbourPoint(short nIdx){
        bbNeighbours.position(nIdx* NEIGHBOUR_SIZE + NEIGHBOUR_POINT_OFFSET);
        return bbNeighbours.getShort();
    }
    public byte getTileSelector(short nIdx){
        bbNeighbours.position(nIdx* NEIGHBOUR_SIZE + NEIGHBOUR_FLAG_TILE_SELECTOR_OFFSET);
        return bbNeighbours.get();
    }
    public boolean isPrimary(short nIdx){
        bbNeighbours.position(nIdx* NEIGHBOUR_SIZE + NEIGHBOUR_FLAG_PRIMARY_OFFSET);
        byte primary = bbNeighbours.get() ;
        assert (primary != PRIMARY_NA);
        return (primary == PRIMARY_YES);
    }
    public short getReverse(short nIdx){
        bbNeighbours.position(nIdx* NEIGHBOUR_SIZE + NEIGHBOUR_FLAG_PRIMARY_OFFSET);
        byte reverse = bbNeighbours.get();
        assert (reverse != 0);
        return ((short)(nIdx + reverse));
    }
    public short getNextNeighbour(short nIdx){
        assert((nIdx > 0) && (nIdx*NEIGHBOUR_SIZE < bbNeighbours.array().length)):"unexpected nIdx="+nIdx;
        bbNeighbours.position(nIdx* NEIGHBOUR_SIZE + NEIGHBOUR_NEXT_NEIGHBOUR_OFFSET);
        short nextNeighbour = bbNeighbours.getShort();
        assert ((0 <= nextNeighbour) && (nextNeighbour < neighboursUsed));
        return nextNeighbour;
    }
    public void setNextNeighbour(short nIdx, short nnIdx){
        bbNeighbours.position(nIdx* NEIGHBOUR_SIZE + NEIGHBOUR_NEXT_NEIGHBOUR_OFFSET);
        bbNeighbours.putShort(nnIdx);
    }

    public short getWayAttributes(short nIdx){
        bbNeighbours.position(nIdx* NEIGHBOUR_SIZE + NEIGHBOUR_WAY_ATTRIBUTES_OFFSET);
        return bbNeighbours.getShort();
    }
    public float getDistance(short nIdx){
        bbNeighbours.position(nIdx* NEIGHBOUR_SIZE + NEIGHBOUR_DISTANCE_OFFSET);
        return bbNeighbours.getFloat();
    }
    public float getCost(short nIdx){
        bbNeighbours.position(nIdx* NEIGHBOUR_SIZE + NEIGHBOUR_COST_OFFSET);
        return bbNeighbours.getFloat();
    }
    public void setCost(short nIdx, float cost){
        bbNeighbours.position(nIdx* NEIGHBOUR_SIZE + NEIGHBOUR_COST_OFFSET);
        bbNeighbours.putFloat(cost);
    }


}
