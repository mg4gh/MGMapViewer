package mg.mgmap.generic.graph.test.agraph;

import java.lang.invoke.MethodHandles;

import mg.mgmap.generic.util.basic.MGLog;

public class ANodes {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());




    static final byte FLAG_FIX               = 0x01;
    static final byte FLAG_VISITED           = 0x02;
    static final byte FLAG_HEIGHT_RELEVANT   = 0x04;
    static final byte FLAG_INVALID           = 0x40;
    static final byte FLAG_ALL               = (byte)0xff;

    short nodesUsed = 0;
    int[] las = null;
    int[] los = null;
    float[] eles = null;
    byte[] borders = null;
    byte[] flags = null;
    short[] neighbours = null;

    public void init(int[] las, int[] los, float[] eles, byte[] borders, byte[] flags, short[] neighbours) {
        this.las = las;
        this.los = los;
        this.eles = eles;
        this.borders = borders;
        this.flags = flags;
        this.neighbours = neighbours;
    }



    short createNode(int la, int lo, float ele, byte border){
        short node = nodesUsed++;
//        mgLog.d(String.format(Locale.ENGLISH, "createNode idx=%d - lat=%.6f lon=%.6f ele=%.1f, border=%d",nodeIdx, LaLo.md2d(lat), LaLo.md2d(lon), ele, borderNodes));
        las[node] = la;
        los[node] = lo;
        eles[node] = ele;
        borders[node] = border;
        return node;
    }

    int getLatitude(short node){
        return las[node];
    }
    void setLatitude(short node, int la){
        las[node] = la;
    }

    int getLongitude(short node){
        return los[node];
    }
    void setLongitude(short node, int lo){
        los[node] = lo;
    }

    float getEle(short node){
        return eles[node];
    }
    void setEle(short node, float ele){
        eles[node] = ele;
    }

    short getNeighbour(short node){
        return neighbours[node];
    }
    void setNeighbour(short node, short neighbour){
        neighbours[node] = neighbour;
    }





    boolean isBorderPoint(short node){
        return (borders[node] != 0);
    }
    byte getBorder(short node){
        return borders[node];
    }
    int countBorderNodes(){
        int cnt = 0;
        for (short node=0; node<nodesUsed; node++){
            if (isBorderPoint(node)) cnt++;
        }
        return cnt;
    }





    boolean isFlag(short node, byte flag){
        return (flags[node] & flag) != 0;
    }

    void setFlag(short node, byte flag, boolean value){
        if (value){
            flags[node] |= flag;
        } else {
            flags[node] &= (byte)(flag ^ FLAG_ALL);
        }
    }
    void setFlags(short node, byte flag1, boolean value1, byte flag2, boolean value2, byte flag3, boolean value3){
        if (value1){
            flags[node] |= flag1;
        } else {
            flags[node] &= (byte)(flag1 ^ 0xFF);
        }
        if (value2){
            flags[node] |= flag2;
        } else {
            flags[node] &= (byte)(flag2 ^ 0xFF);
        }
        if (value3){
            flags[node] |= flag3;
        } else {
            flags[node] &= (byte)(flag3 ^ 0xFF);
        }
    }
}
