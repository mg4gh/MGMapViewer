package mg.mgmap.generic.graph.impl5;

public class Config {

    public static final double CONNECT_THRESHOLD_METER = 0.5; // means 0.5m
    static int MAX_NODE_CNT = 15000;
    static int MAX_NEIGHBOUR_CNT = 32000;
    static int MAX_DNODEREF_CNT = 65000; // don't use values above 65534 (max unsigned short)

    static final int LOW_MEMORY_THRESHOLD = 2;

    static int CACHE_LIMIT = 1000;
    static byte ZOOM_LEVEL = 14;

}
