package mg.mapviewer.util;

import android.util.Log;

import mg.mapviewer.BgJobService;
import mg.mapviewer.MGMapApplication;

public class BgJob {

    public BgJobService service = null;
    public int notification_id = 0;
    long lastNotification = 0;

    public boolean started = false;
    public boolean finished = false;

    public void start(){
        try {
            notification_id = 100+(int)(Math.random()*1000000000);
            started = true;
            doJob();
        } catch (Exception e){
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        } finally {
            finished = true;
        }
    }

    protected void doJob() throws Exception{

    }

    protected void notify(String text){
        long now = System.currentTimeMillis();
        if (now - lastNotification > 2000){
            lastNotification = now;
            service.notifyUser(notification_id, text);
        }
    }
}
