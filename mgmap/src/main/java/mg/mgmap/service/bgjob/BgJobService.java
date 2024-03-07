/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.service.bgjob;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.R;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.BgJob;
import mg.mgmap.generic.util.basic.MGLog;

/**
 * Android Service that coordinates gps and barometer access
 */

public class BgJobService extends Service {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final int MAX_WORKER = 8;
    private MGMapApplication application = null;

    private static boolean active = false;
    private NotificationCompat.Builder baseNotiBuilder = null;
    private final AtomicInteger numWorkers = new AtomicInteger(0);
    private String CHANNEL_ID;
    private int maxJobs = 0; // max jobs at activation - used for progress handling
    private int lastNumBgJobs = 0;

    private Handler timer;
    private final Runnable ttNotify = new Runnable() { //check for progress update each second
        @Override
        public void run() {
            int currentNumBgJobs = application.numBgJobs();
            if (lastNumBgJobs != currentNumBgJobs){
                notifyUserProgress(baseNotiBuilder, 1, maxJobs, maxJobs -currentNumBgJobs, false);
                lastNumBgJobs = currentNumBgJobs;
            }
            timer.postDelayed(ttNotify, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        application = (MGMapApplication)getApplication();
        timer = new Handler();

        CHANNEL_ID = "MGMapViewer_BgJobService_Notification";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "BgJobService notification channel",
                NotificationManager.IMPORTANCE_LOW);

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        mgLog.i("importance: "+channel.getImportance());
        baseNotiBuilder = createNotificationBuilder("BgJobService: running");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        application = (MGMapApplication)getApplication();
        checkActivateService();
        if (active){
            checkActivateWorker();
        }
        return START_STICKY;
    }

    synchronized protected void checkActivateService() {
        try {
            if ((!active) && (application.numBgJobs() > 0)) {
                active = true;
                maxJobs = application.numBgJobs() + numWorkers.get();
                lastNumBgJobs = maxJobs;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(1, baseNotiBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                } else {
                    startForeground(1, baseNotiBuilder.build());
                }
                mgLog.i("startForeground() for BgJobService triggered.  ");

                timer.postDelayed(ttNotify, 1000);
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
    }

    synchronized protected void checkActivateWorker(){
        try {
            while ((numWorkers.get() < MAX_WORKER) && (application.numBgJobs() > 0)){
                numWorkers.incrementAndGet();
                new Thread(() -> {
                    BgJob job;
                    while ((job = application.getBgJob()) != null){
                        job.service = BgJobService.this;
                        application.addActiveJob(job);
                        job.start();
                        application.removeActiveJob(job);
                    }
                    numWorkers.decrementAndGet();
                    checkDeactivateService();
                }).start();
            }
        } catch (Exception e) {
            mgLog.e(e);
        }

    }
    synchronized protected void checkDeactivateService(){
        try {
            if (isActive() && (application.numBgJobs() == 0) && (numWorkers.get() == 0)){
                active = false;
                stopForeground(STOP_FOREGROUND_REMOVE);
                mgLog.i("stopForeground() for BgJobService triggered.  ");
                timer.removeCallbacks(ttNotify);
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
    }

    public NotificationCompat.Builder createNotificationBuilder(String notificationText){
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.mg2)
                .setContentTitle("MGMap")
                .setContentText(notificationText)
                .setSound(null)
                .setProgress(0, 0, true)
                .setOngoing(true)
                .setOnlyAlertOnce(true);
    }

    public void notifyUserProgress(NotificationCompat.Builder builder, int notificationId, int max, int progress, boolean indeterminate) {
        builder.setProgress(max, progress, indeterminate);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(application).notify(notificationId, builder.build());
            mgLog.v("NOTI: id="+notificationId+" "+" max="+max+" progress="+progress);
        }
    }

    public void notifyUserFinish(int notificationId) {
        NotificationManagerCompat.from(application).cancel(notificationId);
        mgLog.v("NOTI: id="+notificationId+" "+" cancel");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static boolean isActive(){
        return active;
    }
}
