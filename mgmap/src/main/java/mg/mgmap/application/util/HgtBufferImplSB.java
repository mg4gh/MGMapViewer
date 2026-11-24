package mg.mgmap.application.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import mg.mgmap.generic.model.PointModel;

public class HgtBufferImplSB implements HgtBuffer{

    String name;
    NativeBuffer nb;
    ByteBuffer bb;
    ShortBuffer sb;
    long accessNanos = System.nanoTime();

    public HgtBufferImplSB(String name, InputStream is) throws IOException {
        this.name = name;
//        bb = ByteBuffer.allocateDirect(is.available());
        nb = new NativeBuffer(name, is.available());
        bb = nb.getBuffer();
        try (ReadableByteChannel channel = Channels.newChannel(is)) {
            while (bb.hasRemaining()) {
                if (channel.read(bb) == -1) {
                    break;
                }
            }
        }
        bb.flip();
        sb = bb.asShortBuffer();
    }

    @Override
    public boolean isSeaLevel() {
        return false;
    }

    @Override
    public float get(int idx) {
        accessNanos = System.nanoTime();
        if (sb != null){
            return sb.get(idx);
        }
        return PointModel.NO_ELE;
    }

    @Override
    public long getAccessNanos() {
        return accessNanos;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void cleanup() {
        bb = null;
        sb = null;
        nb.free();
        nb = null;
    }
}
