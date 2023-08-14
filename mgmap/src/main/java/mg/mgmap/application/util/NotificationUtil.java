package mg.mgmap.application.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import java.lang.invoke.MethodHandles;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.MGLog;

public class NotificationUtil {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private final NotificationManager notificationManager;
    private Notification notification = null;
    private final String CHANNEL_ID;
    private final MGMapApplication application;
    private final Context context;
    private final Handler timer = new Handler(Looper.getMainLooper());
    private final Uri soundUri;
    private Ringtone ringtone = null;

    public NotificationUtil(MGMapApplication application){
        this.application = application;
        this.context = application;
        soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.alarm);
        CHANNEL_ID = "MGMapViewer_PowerSaving_Notification";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                "PowerSaving notification channel",
                NotificationManager.IMPORTANCE_HIGH);
        notificationManager = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE));
        notificationManager.createNotificationChannel(channel);
        mgLog.i("create \""+ channel.getName()+"\" - importance: " + channel.getImportance()+" soundUri="+soundUri);
    }

    public void notifyAlarm() {
        Intent intent = new Intent(context, MGMapActivity.class);
        notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.mg2)
                .setContentTitle("MGMapViewer")
                .setContentText("Power saving problem detected")
                .setContentIntent(PendingIntent.getActivity(context.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE))
                .build();
        mgLog.i("send alarm notification");
        notificationManager.notify(77,notification);
        timer.postDelayed(() -> {
            mgLog.i("cancel alarm notification");
            notificationManager.cancel(77);
        }, 60*1000);

        try {
            if (application.getPrefCache().get(R.string.preferences_alarm_ps_key, false).getValue()){
                ringtone = RingtoneManager.getRingtone(context, soundUri);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.setVolume(1.0f);
                }
                ringtone.play();
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
    }

    public void finishAlarm(){
        try {
            mgLog.i();
            Ringtone r = ringtone;
            if (r != null){
                mgLog.i();
                r.stop();
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
    }
}
