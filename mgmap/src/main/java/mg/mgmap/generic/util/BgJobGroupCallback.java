package mg.mgmap.generic.util;

public interface BgJobGroupCallback {

    public boolean groupFinished(int total, int success, int fail); // should return true, if retry shall be offered

    public default void retry(BgJobGroup jobGroup) {
        jobGroup.doit();
    };
}
