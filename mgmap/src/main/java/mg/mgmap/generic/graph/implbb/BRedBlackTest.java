package mg.mgmap.generic.graph.implbb;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class BRedBlackTest extends BRedBlackEntries{


    final static int ENTRY_SIZE =
            2       // sort value short
            +1;     // empty



    ArrayList<ByteBuffer> buffers = new ArrayList<>();
    int usedEntries = 0;

    public BRedBlackTest(){
        super(ENTRY_SIZE, 10000);
    }


    public void addEntry(short eValue){
        int idx = getUsedEntries(); // next idx will be used
        ByteBuffer bb = prepareAddEntry();
        // put the entry content data
        bb.putShort(eValue);
        bb.put((byte)0);
        // insert entry in tree
        insert(idx);

//        System.out.println("after insert node="+idx+" value="+eValue);
//        for (int i=0; i<=idx; i++){
//            System.out.println(toString(i));
//        }
    }

    public short getValue(int node){
        ByteBuffer bb = getBuf4Idx(node);
        return bb.getShort();
    }

    @Override
    int compareTo(int idx1, int idx2) {
        ByteBuffer bb1 = getBuf4Idx(idx1);
        short v1 = bb1.getShort();
        ByteBuffer bb2 = getBuf4Idx(idx2);
        short v2 = bb2.getShort();
        int res = Short.compare(v1,v2);
        if (res == 0){
            res = Integer.compare(idx1, idx2);
        }
        return res;
    }



    @Override
    String toString(int node) {
        return super.toString(node)+" value="+getValue(node);
    }
}
