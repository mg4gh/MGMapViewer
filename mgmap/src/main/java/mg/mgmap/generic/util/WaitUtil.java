package mg.mgmap.generic.util;

import android.util.Log;

import java.lang.invoke.MethodHandles;

import mg.mgmap.generic.util.basic.MGLog;

public class WaitUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void doWait(Object object, long millis){

        try {
            synchronized (object){
                object.wait(millis);
            }
        } catch (InterruptedException e) {
            mgLog.w(e.getMessage() );
        }
    }
}
