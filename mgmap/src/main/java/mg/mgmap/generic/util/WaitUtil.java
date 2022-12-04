package mg.mgmap.generic.util;

import android.util.Log;

public class WaitUtil {

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void doWait(Object object, long millis, String tag){

        try {
            synchronized (object){
                object.wait(millis);
            }
        } catch (InterruptedException e) {
            Log.w(tag, e.getMessage() );
        }
    }
}
