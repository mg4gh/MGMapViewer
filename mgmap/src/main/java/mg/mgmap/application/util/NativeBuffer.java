package mg.mgmap.application.util;

import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.util.Locale;

import mg.mgmap.generic.util.basic.MGLog;

public class NativeBuffer {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    private static boolean nativeLoaded;

    static {
        try {
            System.loadLibrary("mgmap-native");
            nativeLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            // Running on host JVM (local unit test) where Android .so library is not present
            nativeLoaded = false;
        }
    }

    private ByteBuffer byteBuffer;
    private final String name;
    private final long size;

    public NativeBuffer(String name, long size) {
        this.name = name;
        this.size = size;
        mgLog.v(() -> String.format(Locale.ENGLISH, "NaBu allocate: %s - %d", name, size));
        if (nativeLoaded) {
            byteBuffer = allocateDirectBuffer(size);
        } else {
            // Fallback for local unit tests running on host JVM
            byteBuffer = ByteBuffer.allocateDirect((int) size);
        }
    }

    public ByteBuffer getBuffer() {
        return byteBuffer;
    }

    public void free() {
        if (byteBuffer != null) {
            mgLog.v(() -> String.format(Locale.ENGLISH, "NaBu free: %s - %d", name, size));
            if (nativeLoaded) {
                freeDirectBuffer(byteBuffer);
            }
            byteBuffer = null;
        }
    }

    private native ByteBuffer allocateDirectBuffer(long size);
    private native void freeDirectBuffer(ByteBuffer buffer);
}
