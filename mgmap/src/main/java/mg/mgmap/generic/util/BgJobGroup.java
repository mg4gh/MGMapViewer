package mg.mgmap.generic.util;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;

public class BgJobGroup {

    ArrayList<BgJob> bgJobs = new ArrayList<>();
    int successCounter = 0;
    int errorCounter = 0;
    int jobCounter = 0;
    boolean constructed = false;

    String title;
    String details;
    boolean offerRetries = false;

    final BgJobGroupCallback groupCallback;
    MGMapApplication application;
    AppCompatActivity activity;

    public BgJobGroup(MGMapApplication application, AppCompatActivity activity, String title, BgJobGroupCallback groupCallback) {
        this.application = application;
        this.activity = activity;
        this.title = title;
        this.groupCallback = groupCallback;
    }

    public void addJob(BgJob bgJob){
        if (!constructed){
            bgJob.group = this;
            bgJobs.add(bgJob);
        }
    }

    public void setConstructed(String details){
        constructed = true;
        jobCounter = bgJobs.size();
        this.details = details;
        new BgJobUtil(activity, application).processConfirmDialog(this);
    }

    public int size(){
        return bgJobs.size();
    }

    public void doit(){
        application.addBgJobs(bgJobs);
    }
    public void retry(){
        successCounter = 0;
        errorCounter = 0;
        groupCallback.retry(this);
    }

    synchronized void jobFinished(boolean success, Exception e){
        if (success){
            successCounter++;
        } else {
            errorCounter++;
        }
        String message = "successCounter="+successCounter+"  errorCounter="+errorCounter+"  jobCounter="+jobCounter;
        Log.d(MGMapApplication.LABEL, NameUtil.context() +"  "+message+ ((e==null)?"":" "+e.getMessage()));
        if (successCounter + errorCounter == jobCounter){
            if (groupCallback.groupFinished(jobCounter, successCounter, errorCounter)){
                offerRetries = true;
            }
            new Thread(() -> activity.runOnUiThread(this::reportResult)).start();
        }
    }

    void reportResult(){
        new BgJobUtil(activity, application).reportResult(this);
    }




}
