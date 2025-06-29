package mg.mgmap.generic.graph.implbb;

import java.nio.ByteBuffer;

import mg.mgmap.generic.model.PointModel;

public class BNodes {

    final static int NODE_SIZE =
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


    static final byte FLAG_FIX               = 0x01;
    static final byte FLAG_VISITED           = 0x02;
    static final byte FLAG_HEIGHT_RELEVANT   = 0x04;
    static final byte FLAG_INVALID           = 0x40;

    ByteBuffer bbNodes;
    short nodesUsed = 0;

    void init(ByteBuffer bbNodes){
        this.bbNodes = bbNodes;
    }

    short createNode(int lat, int lon){
        short nodeIdx = nodesUsed;
        bbNodes.position(nodeIdx* NODE_SIZE + POINT_LATITUDE_OFFSET);
        bbNodes.putInt(lat);
        bbNodes.putInt(lon);
        return nodeIdx;
    }

    short createNode(int lat, int lon, float ele, byte borderNodes){
        short nodeIdx = nodesUsed++;
        bbNodes.position(nodeIdx* NODE_SIZE + POINT_LATITUDE_OFFSET);
        bbNodes.putInt(lat);
        bbNodes.putInt(lon);
        bbNodes.putFloat(ele);
        bbNodes.put(borderNodes);
        bbNodes.put((byte)0); // POINT_FLAG_SMOOTH_OFFSET
        bbNodes.putShort((short)0); // POINT_NEIGHBOUR_OFFSET
        return nodeIdx;
    }

    int getLatitude(short nodeIdx){
        bbNodes.position(nodeIdx* NODE_SIZE + POINT_LATITUDE_OFFSET);
        return bbNodes.getInt();
    }
    void setLatitude(short nodeIdx, int lat){
        bbNodes.position(nodeIdx* NODE_SIZE + POINT_LATITUDE_OFFSET);
        bbNodes.putInt(lat);
    }

    int getLongitude(short nodeIdx){
        bbNodes.position(nodeIdx* NODE_SIZE + POINT_LONGITUDE_OFFSET);
        return bbNodes.getInt();
    }
    void setLongitude(short nodeIdx, int lon){
        bbNodes.position(nodeIdx* NODE_SIZE + POINT_LONGITUDE_OFFSET);
        bbNodes.putInt(lon);
    }

    float getEle(short nodeIdx){
        bbNodes.position(nodeIdx* NODE_SIZE + POINT_ELEVATION_OFFSET);
        return (bbNodes.getFloat());
    }
    void setEle(short nodeIdx, float ele){
        bbNodes.position(nodeIdx* NODE_SIZE + POINT_ELEVATION_OFFSET);
        bbNodes.putFloat(ele);
    }

    short getNeighbour(short nodeIdx){
        bbNodes.position(nodeIdx* NODE_SIZE + POINT_NEIGHBOUR_OFFSET);
        return bbNodes.getShort();
    }
    void setNeighbour(short nodeIdx, short nIdx){
        bbNodes.position(nodeIdx* NODE_SIZE + POINT_NEIGHBOUR_OFFSET);
        bbNodes.putShort(nIdx);
    }





    boolean isBorderPoint(short nodeIdx){
        bbNodes.position(nodeIdx* NODE_SIZE + POINT_FLAG_BORDER_OFFSET);
        return (bbNodes.get() != 0);
    }
    byte getBorder(short nodeIdx){
        bbNodes.position(nodeIdx* NODE_SIZE + POINT_FLAG_BORDER_OFFSET);
        return bbNodes.get();
    }
//    void setBorder(short nodeIdx, byte border){
//        bbNodes.position(nodeIdx* NODE_SIZE + POINT_FLAG_BORDER_OFFSET);
//        bbNodes.put(border);
//    }

    public int countBorderNodes() {
        int cnt = 0;
        int numPoints = nodesUsed;
        for (short iIdx = 0; iIdx < numPoints; iIdx++) { // iIdx is first pointIdx
            bbNodes.position(iIdx* NODE_SIZE + POINT_LATITUDE_OFFSET);
            int iLat = bbNodes.getInt();
            int iLon = bbNodes.getInt();
            if ((iLat == PointModel.NO_LAT_LONG_MD) || (iLon == PointModel.NO_LAT_LONG_MD)) continue; // invalid point
            if (isBorderPoint(iIdx)) cnt++;
        }
        return cnt;
    }




    boolean isFlag(short pointIdx, byte flag){
        bbNodes.position(pointIdx* NODE_SIZE + POINT_FLAG_SMOOTH_OFFSET);
        byte flags = bbNodes.get();
        return (flags & flag) != 0;
    }

    void setFlag(short pointIdx, byte flag, boolean value){
        bbNodes.position(pointIdx* NODE_SIZE + POINT_FLAG_SMOOTH_OFFSET);
        byte flags = bbNodes.get();
        if (value){
            flags |= flag;
        } else {
            flags &= (byte)(flag ^ 0xFF);
        }
        bbNodes.position(pointIdx* NODE_SIZE + POINT_FLAG_SMOOTH_OFFSET);
        bbNodes.put(flags);
    }
    void setFlags(short pointIdx, byte flag1, boolean value1, byte flag2, boolean value2, byte flag3, boolean value3){
        bbNodes.position(pointIdx* NODE_SIZE + POINT_FLAG_SMOOTH_OFFSET);
        byte flags = bbNodes.get();
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
        bbNodes.position(pointIdx* NODE_SIZE + POINT_FLAG_SMOOTH_OFFSET);
        bbNodes.put(flags);
    }
}
