package mg.mgmap.generic.util.basic;

import java.lang.invoke.MethodHandles;

public class MemoryUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public static boolean checkLowMemory(int threshold){
        long maxMem = Runtime.getRuntime().maxMemory();
        long totalMem = Runtime.getRuntime().totalMemory();
        long freeMem = Runtime.getRuntime().freeMemory();
        long totalFreeMem = maxMem - totalMem + freeMem;
        long freeMemPercent =  (totalFreeMem*100) /maxMem;
        mgLog.d("freeMemPercent="+freeMemPercent+ " maxMem="+maxMem);
        if (freeMemPercent < 5) {
            mgLog.i("trigger System.gc()");
            System.gc();
        }
        return freeMemPercent < threshold;
    }

}
