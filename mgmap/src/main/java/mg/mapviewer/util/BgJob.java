package mg.mapviewer.util;

import android.util.Log;

import mg.mapviewer.BgJobService;
import mg.mapviewer.MGMapApplication;

public class BgJob {

    public BgJobService service = null;
    public int notification_id = 0;
    long lastNotification = 0;
    int max = 0;
    int progress = 0;
    String text = null;

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

    protected void notifyUser(){
        long now = System.currentTimeMillis();
        if (now - lastNotification > 1000){
            lastNotification = now;
            service.notifyUser(notification_id, text, max, progress, (max==0)||(progress==0));
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+text+" "+max+" "+progress);
        }
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
