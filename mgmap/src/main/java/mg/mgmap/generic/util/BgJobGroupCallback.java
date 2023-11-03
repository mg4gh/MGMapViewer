package mg.mgmap.generic.util;

import android.view.View;

public interface BgJobGroupCallback {

    default boolean groupFinished(BgJobGroup jobGroup, int total, int success, int fail){ // return true, if retry shall be offered
        return false;
    }

    default void afterGroupFinished(BgJobGroup jobGroup, int total, int success, int fail){}

    default void retry(BgJobGroup jobGroup) {
        jobGroup.doit();
    }

    default View getContentView(){
        return null;
    }
}
