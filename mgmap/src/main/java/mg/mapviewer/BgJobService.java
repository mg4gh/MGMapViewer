/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mapviewer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Timer;
import java.util.TimerTask;

import mg.mapviewer.util.BgJob;
import mg.mapviewer.util.NameUtil;

/**
 * Android Service that coordinates gps and barometer access
 */

public class BgJobService extends Service {

    private MGMapApplication application = null;

    private boolean active = false;
    private Notification notification = null;
    private volatile int numWorkers = 0;
    private String CHANNEL_ID;
    private int max = 0; // max jobs at activation - used for progress handling
    private int lastNumBgJobs = 0;

    private Handler timer;
    private TimerTask ttNotify = new TimerTask() {
        @Override
        public void run() {
            int currentNumBgJobs = application.numBgJobs();
            if (lastNumBgJobs != currentNumBgJobs){
                notifyUser(1,"BgJobService: running", max, max-currentNumBgJobs, false);
                lastNumBgJobs = currentNumBgJobs;
            }
            timer.postDelayed(ttNotify, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        application = (MGMapApplication)getApplication();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CHANNEL_ID = "my_channel_02";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "MGMapViewer information channel 02",
                    NotificationManager.IMPORTANCE_LOW);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" importance: "+channel.getImportance());

            Intent intent = new Intent(getApplicationContext(), MGMapActivity.class);
            notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.mg2)
                    .setContentTitle("MGMapViewer")
                    .setContentText("BgJobService is running.")
                    .setContentIntent(    PendingIntent.getActivity(this.getApplicationContext(), 0, intent , PendingIntent.FLAG_UPDATE_CURRENT))
                    .setSound(null)
                    .build();
        }


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        application = (MGMapApplication)getApplication();
        checkActive();

        return START_STICKY;
    }

    private void checkActive(){
        boolean shouldBeActive = (application.numBgJobs() > 0);
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" acitve="+active+" shouldBeActive="+shouldBeActive);
        if (!active && shouldBeActive){
            active = true;
            activateService();
        }
        if (active && !shouldBeActive){
            active = false;
            deactivateService();
        }
    }

//    protected void updateNotification(){
//        notifyUser(1,"BgJobService is running. "+numWorkers+" running, "+application.numBgJobs()+" waiting");
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            notification = new NotificationCompat.Builder(this, CHANNEL_ID)
//                    .setSmallIcon(R.drawable.mg2)
//                    .setContentTitle("MGMapViewer")
//                    .setContentText("BgJobService is running. "+application.numBgJobs())
//                    .setSound(null)
//                    .build();
//            NotificationManagerCompat.from(application).notify(1,notification);
//        }
//    }

    protected void activateService(){
        try {
            max = application.numBgJobs() + getNumWorkers();
            lastNumBgJobs = max;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForeground(1, notification);
            }

            timer = new Handler();
            timer.postDelayed(ttNotify, 1000);

            for (int i=0; i<8; i++){
                new Thread(){
                    @Override
                    public void run() {
                        numWorkers++;
                        BgJob job = null;
                        while ((job = application.getBgJob()) != null){
                            job.service = BgJobService.this;
                            job.start();
                            NotificationManagerCompat.from(application).cancel(job.notification_id);
 //                           updateNotification();
                        }
                        numWorkers--;
                        synchronized (this){
                            if (numWorkers == 0){
                                checkActive();
                            }
                        }
                    }

                }.start();
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }

    }
    protected void deactivateService(){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true);
            }
            application.refresh();
            timer.removeCallbacks(ttNotify);
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
    }

    public int getNumWorkers(){
        return numWorkers;
    }

    public void notifyUser(int notificationId, String notificationText, int max, int progress, boolean indeterminate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.mg2)
                    .setContentTitle("MGMapViewer")
                    .setContentText(notificationText)
                    .setSound(null)
                    .setProgress(max, progress, indeterminate)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build();
            NotificationManagerCompat.from(application).notify(notificationId, notification);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
