package mg.mgmap.application.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.lang.invoke.MethodHandles;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.MGLog;

public class GpsSupervisorWorker extends Worker {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    public GpsSupervisorWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
    }
    @NonNull
    @Override
    public Result doWork() {
        mgLog.i("GpsSupervisorWorker is running.");
        MGMapApplication application = (MGMapApplication) getApplicationContext();

        application.triggerGpsSupervisionWorker();
        application.checkGpsStatus();
        return Result.success();
    }
}
