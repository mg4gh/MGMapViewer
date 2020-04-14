package mg.mapviewer.util;

import android.util.Log;

import mg.mapviewer.MGMapApplication;

public class BgJob {

    public boolean started = false;
    public boolean finished = false;

    public void start(){
        try {
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
}
