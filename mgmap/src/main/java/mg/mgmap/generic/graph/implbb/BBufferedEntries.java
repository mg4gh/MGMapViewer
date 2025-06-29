package mg.mgmap.generic.graph.implbb;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class BBufferedEntries {


    final int fullEntrySize;
    final int entriesInBuffer;
    final int bufferSize;

    ArrayList<ByteBuffer> buffers = new ArrayList<>();
    int usedEntries = 0;

    public BBufferedEntries(int fullEntrySize, int entriesInBuffer) {
        this.fullEntrySize = fullEntrySize;
        this.entriesInBuffer = entriesInBuffer;
        this.bufferSize = fullEntrySize * entriesInBuffer;
    }

    public ByteBuffer prepareAddEntry(){
        if (buffers.size() * entriesInBuffer <= usedEntries){ // allocate a new buffer
            byte[] buf = new byte[bufferSize];
            buffers.add(ByteBuffer.wrap(buf));
        }
        ByteBuffer bb = buffers.get(buffers.size()-1);
        bb.position((usedEntries % entriesInBuffer) * fullEntrySize);
        usedEntries++;
        return bb;
    }

    public int getUsedEntries() {
        return usedEntries;
    }

    ByteBuffer getBuf4Idx(int idx){
        ByteBuffer bb = buffers.get( idx / entriesInBuffer);
        bb.position( (idx % entriesInBuffer) * fullEntrySize );
        return bb;
    }



}
