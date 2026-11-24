package mg.mgmap.application.util;

public interface HgtBuffer {

    boolean isSeaLevel();

    float get(int idx);  // index of short values (not byte offset)

    long getAccessNanos();

    default void cleanup(){}

    String getName();
}
