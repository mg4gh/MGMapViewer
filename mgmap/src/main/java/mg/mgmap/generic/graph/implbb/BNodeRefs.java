package mg.mgmap.generic.graph.implbb;

import java.nio.ByteBuffer;

public class BNodeRefs extends BRedBlackEntries{

    final static int NODE_REF_SIZE =
            2 + 2       // node: tile idx (in GraphMulti) + Node idx (in GraphTile)
            + 4         // cost (float)
            + 4         // heuristic (float)
            + 2 + 2     // predecessor: tile idx (in GraphMulti) + Node idx (in GraphTile)
            + 2         // neighbour (neighbour idx of node to predecessor)
            + 1;         // FLAGS:

                    // settled: Bit 0 - not settled,  1 - settled
                    // reverse: Bit 1 - not reverse,  1 - reverse


    final static int TILE_IDX_IN_MULTI_OFFSET = 0;
    final static int NODE_IDX_OFFSET = 2;
    final static int COST_OFFSET = 4;
    final static int HEURISTIC_OFFSET = 8;
    final static int PREV_TILE_IDX_IN_MULTI_OFFSET = 12;
    final static int PREV_NODE_IDX_OFFSET = 14;
    final static int NEIGHBOUR_OFFSET = 16;
    final static int FLAGS_OFFSET = 18;

    final static byte FLAGS_SETTLED = 0x01;
    final static byte FLAGS_REVERSED = 0x02;

    final BGraphMulti bGraphMulti;

    public BNodeRefs(BGraphMulti bGraphMulti){
        super(NODE_REF_SIZE, 10000);
        this.bGraphMulti = bGraphMulti;
    }

    public int createNodeRef(short tileIdxInMulti, short nodeIdx, float cost, float heuristic, short prevTileIdxInMulti, short prevNodeIdx, short neighbour, byte flags ){
        int idx = getUsedEntries(); // next idx will be used
        ByteBuffer bb = prepareAddEntry();
        // put the entry content data
        bb.putShort(tileIdxInMulti);
        bb.putShort(nodeIdx);
        bb.putFloat(cost);
        bb.putFloat(heuristic);
        bb.putShort(prevTileIdxInMulti);
        bb.putShort(prevNodeIdx);
        bb.putShort(neighbour);
        bb.put(flags);
        // insert entry in tree
        insert(idx);
        return idx;
    }

    public short getNodeTileIdx(int node){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+TILE_IDX_IN_MULTI_OFFSET);
        return bb.getShort();
    }
    public short getNodeIdx(int node){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+NODE_IDX_OFFSET);
        return bb.getShort();
    }
    public short getPrevNodeTileIdx(int node){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+PREV_TILE_IDX_IN_MULTI_OFFSET);
        return bb.getShort();
    }
    public short getPrevNodeIdx(int node){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+PREV_NODE_IDX_OFFSET);
        return bb.getShort();
    }
    public float getCost(int node){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+COST_OFFSET);
        return bb.getFloat();
    }
    public float getHeuristic(int node){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+HEURISTIC_OFFSET);
        return bb.getFloat();
    }
    public short getNeighbour(int node){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+NEIGHBOUR_OFFSET);
        return bb.getShort();
    }
    public float getHeuristicCost(int node){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+COST_OFFSET);
        return bb.getFloat()+bb.getFloat(); // cost + heuristic
    }

    boolean isFlag(int node, byte flag){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+FLAGS_OFFSET);
        return ((bb.get() & flag) != 0);
    }
    void setFlag(int node, byte flag, boolean value){
        ByteBuffer bb = getBuf4Idx(node);
        bb.position(bb.position()+FLAGS_OFFSET);
        byte flags = bb.get();
        if (value){
            flags |= flag;
        } else {
            flags &= (byte)(flag ^ 0xFF);
        }
        bb.position(bb.position()-1);
        bb.put( flags );
    }









    @Override
    int compareTo(int idx1, int idx2) {
        ByteBuffer bb1 = getBuf4Idx(idx1);
        int tileAndNodeIdx1 = bb1.getInt();
//        short tileIdxInMulti1 = bb1.getShort(); // read tileIdxInMulti
//        short nodeIdx1 = bb1.getShort(); // read nodeIdx
        float heuristicCost1 = bb1.getFloat(); // read cost
        heuristicCost1 += bb1.getFloat();  // read heuristic
        int prevTileAndNodeIdx1 = bb1.getInt();

        ByteBuffer bb2 = getBuf4Idx(idx2);
        int tileAndNodeIdx2 = bb1.getInt();
//        short tileIdxInMulti2 = bb2.getShort(); // read tileIdxInMulti
//        short nodeIdx2 = bb2.getShort(); // read nodeIdx
        float heuristicCost2 = bb2.getFloat(); // read cost
        heuristicCost2 += bb2.getFloat();  // read heuristic
        int prevTileAndNodeIdx2 = bb1.getInt();

        int res = Float.compare(heuristicCost1, heuristicCost2);
        if (res == 0){
            res = Integer.compare(tileAndNodeIdx1, tileAndNodeIdx2);
            if (res == 0){
                res = Integer.compare(prevTileAndNodeIdx1, prevTileAndNodeIdx2);
            }
        }
        return res;
    }

    String toString(BGraphMulti bGraphMulti, int ref){
        ByteBuffer bb = getBuf4Idx(ref);
        BGraphTile bGraphTile = bGraphMulti.getTile( bb.getShort() );
        short node = bb.getShort();
        float cost = bb.getFloat();
        float heuristic = bb.getFloat();
        BGraphTile bPrevGraphTile = bGraphMulti.getTile( bb.getShort() );
        short prevNode = bb.getShort();
        short neighbour = bb.getShort();
        byte flags = bb.get();

        return "GNodeRef{" +
                "node=" + new BNode(bGraphTile, node) +
                ", predecessor=" +  new BNode(bPrevGraphTile, prevNode) +
                ", neighbour=" + neighbour +
                ", cost=" + cost +
                ", heuristic=" + heuristic +
//                ", settled=" + settled +
                ", reverse=" + ((flags & FLAGS_REVERSED) > 0) +
                '}';
    }


}
