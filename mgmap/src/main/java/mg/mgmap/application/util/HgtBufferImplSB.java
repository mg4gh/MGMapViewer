package mg.mgmap.application.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class HgtBufferImplSB implements HgtBuffer{

    ByteBuffer bb;
    ShortBuffer sb;

    public HgtBufferImplSB(InputStream is) throws IOException {
        bb = ByteBuffer.allocateDirect(is.available());
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
        return sb.get(idx);
    }
}
