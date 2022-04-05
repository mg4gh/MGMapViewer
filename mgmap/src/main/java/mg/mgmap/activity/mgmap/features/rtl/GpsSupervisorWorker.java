package mg.mgmap.activity.mgmap.features.rtl;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;

public class GpsSupervisorWorker extends Worker {

    public GpsSupervisorWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" doWork!!!!");
        MGMapApplication application = (MGMapApplication) getApplicationContext();

        application.startTrackLoggerService();
        return Result.success();
    }
}
