package mg.mgmap.application.util;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Locale;

import mg.mgmap.generic.util.basic.MGLog;

public class NativeBuffer {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    static {
        System.loadLibrary("mgmap-native");
    }

    String name;
    long size;
    private ByteBuffer byteBuffer;

    public NativeBuffer(String name, long size) {
        this.name = name;
        this.size = size;
        mgLog.v(()->String.format(Locale.ENGLISH,"NaBu allocate: %s - %d",name,size));
        byteBuffer = allocateDirectBuffer(size);
    }

    public ByteBuffer getBuffer() {
        return byteBuffer;
    }

    public void free() {
        if (byteBuffer != null) {
            mgLog.v(()->String.format(Locale.ENGLISH,"NaBu free: %s - %d",name,size));
            freeDirectBuffer(byteBuffer);
            byteBuffer = null;
        }
    }

    private native ByteBuffer allocateDirectBuffer(long size);
    private native void freeDirectBuffer(ByteBuffer buffer);
}
