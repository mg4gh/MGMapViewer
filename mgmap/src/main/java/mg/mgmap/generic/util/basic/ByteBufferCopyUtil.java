package mg.mgmap.generic.util.basic;

import android.os.Build;

import java.nio.ByteBuffer;

public class ByteBufferCopyUtil {

    public static ByteBuffer cloneArea(ByteBuffer src, int srcOffset, int length) {
        ByteBuffer dest = (src.isDirect())?ByteBuffer.allocateDirect(length):ByteBuffer.allocate(length);
        src.position(srcOffset);
        src.limit(srcOffset + length);
        dest.put( src.slice() );
        return dest;
    }

}
