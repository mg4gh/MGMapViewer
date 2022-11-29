package mg.mgmap.application.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;

public class NotificationUtil {

    private NotificationManager notificationManager = null;
    private Notification notification = null;
    private final String CHANNEL_ID;
    private final Context context;
    private final Handler timer = new Handler();

    public NotificationUtil(Context context){
        this.context = context;
        CHANNEL_ID = "MGMapViewer_PowerSaving_Notification";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "PowerSaving notification channel",
                NotificationManager.IMPORTANCE_HIGH);

        notificationManager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        notificationManager.createNotificationChannel(channel);
//            channel.setSound(PreferenceManager..getNotificationRingTone(context), null);
        Log.i(MGMapApplication.LABEL, NameUtil.context() + "create \""+ channel.getName()+"\" - importance: " + channel.getImportance());
    }

    public void notifyAlarm() {

        Intent intent = new Intent(context, MGMapActivity.class);
        notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.mg2)
                .setContentTitle("MGMapViewer")
                .setContentText("Power saving problem detected")
                .setContentIntent(PendingIntent.getActivity(context.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .build();
        Log.i(MGMapApplication.LABEL, NameUtil.context() + "send alarm notification");
        notificationManager.notify(77,notification);
        timer.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.i(MGMapApplication.LABEL, NameUtil.context() + "cancel alarm notification");
                notificationManager.cancel(77);
            }
        }, 60*1000);
    }
}
