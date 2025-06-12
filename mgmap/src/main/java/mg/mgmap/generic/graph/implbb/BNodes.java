package mg.mgmap.generic.graph.implbb;

import java.nio.ByteBuffer;

import mg.mgmap.generic.graph.WayAttributs;
import mg.mgmap.generic.model.PointModel;

public class BNodes {

    final static int POINT_SIZE =
            4   // latitude in md (int)
                    + 4 // longitude in md (int)
                    + 4 // elevation as float
                    + 2 // 1 byte (4) flags for border node  + 1 byte (3) flags for height smoothing
                    + 2; // first neighbour index
    final static int POINT_LATITUDE_OFFSET = 0;
    final static int POINT_LONGITUDE_OFFSET = 4;
    final static int POINT_ELEVATION_OFFSET = 8;
    final static int POINT_FLAG_BORDER_OFFSET = 12;
    final static int POINT_FLAG_SMOOTH_OFFSET = 13;
    final static int POINT_NEIGHBOUR_OFFSET = 14;

    static final byte BORDER_NODE_WEST  = 0x08;
    static final byte BORDER_NODE_NORTH = 0x04;
    static final byte BORDER_NODE_EAST  = 0x02;
    static final byte BORDER_NODE_SOUTH = 0x01;

    static final byte FLAG_FIX               = 0x01;
    static final byte FLAG_VISITED           = 0x02;
    static final byte FLAG_HEIGHT_RELEVANT   = 0x04;
    static final byte FLAG_INVALID           = 0x40;

    ByteBuffer bbPoints;
    short pointsUsed = 0;

    void init(ByteBuffer bbPoints){
        this.bbPoints = bbPoints;
    }

    short createNode(int lat, int lon){
        short nodeIdx = pointsUsed;
        bbPoints.position(nodeIdx* POINT_SIZE + POINT_LATITUDE_OFFSET);
        bbPoints.putInt(lat);
        bbPoints.putInt(lon);
        return nodeIdx;
    }

    short createNode(int lat, int lon, float ele, byte borderNodes){
        short nodeIdx = pointsUsed++;
        bbPoints.position(nodeIdx* POINT_SIZE + POINT_LATITUDE_OFFSET);
        bbPoints.putInt(lat);
        bbPoints.putInt(lon);
        bbPoints.putFloat(ele);
        bbPoints.put(borderNodes);
        bbPoints.put((byte)0); // POINT_FLAG_SMOOTH_OFFSET
        bbPoints.putShort((short)0); // POINT_NEIGHBOUR_OFFSET
        return nodeIdx;
    }

    int getLatitude(short nodeIdx){
        bbPoints.position(nodeIdx* POINT_SIZE + POINT_LATITUDE_OFFSET);
        return bbPoints.getInt();
    }
    void setLatitude(short nodeIdx, int lat){
        bbPoints.position(nodeIdx* POINT_SIZE + POINT_LATITUDE_OFFSET);
        bbPoints.putInt(lat);
    }

    int getLongitude(short nodeIdx){
        bbPoints.position(nodeIdx* POINT_SIZE + POINT_LONGITUDE_OFFSET);
        return bbPoints.getInt();
    }
    void setLongitude(short nodeIdx, int lon){
        bbPoints.position(nodeIdx* POINT_SIZE + POINT_LONGITUDE_OFFSET);
        bbPoints.putInt(lon);
    }

    float getEle(short nodeIdx){
        bbPoints.position(nodeIdx* POINT_SIZE + POINT_ELEVATION_OFFSET);
        return (bbPoints.getFloat());
    }
    void setEle(short nodeIdx, float ele){
        bbPoints.position(nodeIdx* POINT_SIZE + POINT_ELEVATION_OFFSET);
        bbPoints.putFloat(ele);
    }

    short getNeighbour(short nodeIdx){
        bbPoints.position(nodeIdx*POINT_SIZE + POINT_NEIGHBOUR_OFFSET);
        return bbPoints.getShort();
    }
    void setNeighbour(short nodeIdx, short nIdx){
        bbPoints.position(nodeIdx* POINT_SIZE + POINT_NEIGHBOUR_OFFSET);
        bbPoints.putShort(nIdx);
    }





    boolean isBorderPoint(short nodeIdx){
        bbPoints.position(nodeIdx* POINT_SIZE + POINT_FLAG_BORDER_OFFSET);
        return (bbPoints.get() != 0);
    }
    void setBorder(short nodeIdx, byte border){
        bbPoints.position(nodeIdx* POINT_SIZE + POINT_FLAG_BORDER_OFFSET);
        bbPoints.put(border);
    }

    public int countBorderNodes() {
        int cnt = 0;
        int numPoints = pointsUsed;
        for (short iIdx = 0; iIdx < numPoints; iIdx++) { // iIdx is first pointIdx
            bbPoints.position(iIdx* POINT_SIZE + POINT_LATITUDE_OFFSET);
            int iLat = bbPoints.getInt();
            int iLon = bbPoints.getInt();
            if ((iLat == PointModel.NO_LAT_LONG_MD) || (iLon == PointModel.NO_LAT_LONG_MD)) continue; // invalid point
            if (isBorderPoint(iIdx)) cnt++;
        }
        return cnt;
    }




    boolean isFlag(short pointIdx, byte flag){
        bbPoints.position(pointIdx*POINT_SIZE + POINT_FLAG_SMOOTH_OFFSET);
        byte flags = bbPoints.get();
        return (flags & flag) != 0;
    }

    void setFlag(short pointIdx, byte flag, boolean value){
        bbPoints.position(pointIdx*POINT_SIZE + POINT_FLAG_SMOOTH_OFFSET);
        byte flags = bbPoints.get();
        if (value){
            flags |= flag;
        } else {
            flags &= (byte)(flag ^ 0xFF);
        }
        bbPoints.position(pointIdx*POINT_SIZE + POINT_FLAG_SMOOTH_OFFSET);
        bbPoints.put(flags);
    }
    void setFlags(short pointIdx, byte flag1, boolean value1, byte flag2, boolean value2, byte flag3, boolean value3){
        bbPoints.position(pointIdx*POINT_SIZE + POINT_FLAG_SMOOTH_OFFSET);
        byte flags = bbPoints.get();
        if (value1){
            flags |= flag1;
        } else {
            flags &= (byte)(flag1 ^ 0xFF);
        }
        if (value2){
            flags |= flag2;
        } else {
            flags &= (byte)(flag2 ^ 0xFF);
        }
        if (value3){
            flags |= flag3;
        } else {
            flags &= (byte)(flag3 ^ 0xFF);
        }
        bbPoints.position(pointIdx*POINT_SIZE + POINT_FLAG_SMOOTH_OFFSET);
        bbPoints.put(flags);
    }
}
