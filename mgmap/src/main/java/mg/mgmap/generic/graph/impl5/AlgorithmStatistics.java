package mg.mgmap.generic.graph.impl5;

import androidx.annotation.NonNull;

import java.util.Locale;

public class AlgorithmStatistics {

    long duration = 0;
    int cntTotal = 0;
    int cntRelaxed = 0;
    int cntSettled = 0;
    int resultPathLength = 0;
    double matchCost = 0;
    int removedNodeRefs = 0;
    int dNodeRefCnt = 0;
    int miDNodeRefCnt = 0;
    int prioQueueSize = 0;
    int maxPrioQueueSize = 0;


    long fixNanos = 0;
    long smoothNanos = 0;
    long setupNanos = 0; // includes fix and smooth

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "AlgorithmStatistics{  duration=%dms " +
                        "\n cntTotal=%d cntRelaxed=%d cntSettled=%d resultPathLength=%d matchCost=%.6f removedNodeRefs=%d fixMs=%.1fms smooth=%.1fms setupMs=%.1fms " +
                        "\n dNodeRefCnt=%d miDNodeRefCnt=%d prioQueueSize=%d maxPrioQueueSize=%d",
                duration, cntTotal, cntRelaxed, cntSettled, resultPathLength, matchCost, removedNodeRefs, fixNanos/1000000.0, smoothNanos/1000000.0, setupNanos/1000000.0,
                dNodeRefCnt,miDNodeRefCnt,prioQueueSize,maxPrioQueueSize);
    }
}
