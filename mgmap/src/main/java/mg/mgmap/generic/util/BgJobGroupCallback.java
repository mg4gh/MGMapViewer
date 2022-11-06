package mg.mgmap.generic.util;

public interface BgJobGroupCallback {

    default boolean groupFinished(BgJobGroup jobGroup, int total, int success, int fail){ // return true, if retry shall be offered
        return false;
    }

    default void afterGroupFinished(BgJobGroup jobGroup, int total, int success, int fail){}

    default void retry(BgJobGroup jobGroup) {
        jobGroup.doit();
    }
}
