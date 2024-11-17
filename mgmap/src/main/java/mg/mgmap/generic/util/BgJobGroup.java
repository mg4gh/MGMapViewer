package mg.mgmap.generic.util;

import androidx.appcompat.app.AppCompatActivity;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.MGLog;

public class BgJobGroup {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final ArrayList<BgJob> bgJobs = new ArrayList<>();
    protected int successCounter = 0;
    protected int errorCounter = 0;
    protected int jobCounter = 0;
    boolean constructed = false;

    String title;
    String details;
    boolean offerRetries = false;

    final BgJobGroupCallback groupCallback;
    final MGMapApplication application;
    final AppCompatActivity activity;

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

    public void setTitle(String title){
        this.title = title;
    }

    public void setConstructed(String details){
        constructed = true;
        jobCounter = bgJobs.size();
        this.details = details;
        if (title != null){
            new BgJobUtil(activity, application).processConfirmDialog(this);
        } else {
            doit();
        }
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
        String message = "Title=\""+title+"\" successCounter="+successCounter+"  errorCounter="+errorCounter+"  jobCounter="+jobCounter;
        mgLog.d(message+ ((e==null)?"":" "+e.getMessage()));
        if (successCounter + errorCounter == jobCounter){
            offerRetries =  groupCallback.groupFinished(this, jobCounter, successCounter, errorCounter);
            if (title != null){
                activity.runOnUiThread(this::reportResult);
            } else {
                groupCallback.afterGroupFinished(this, jobCounter, successCounter, errorCounter);
            }
        }
    }

    void reportResult(){
        new BgJobUtil(activity, application).reportResult(this);
    }

    void onReportResultOk(){
        groupCallback.afterGroupFinished(this, jobCounter, successCounter, errorCounter);
    }


    public String getDetails(){
        return details;
    }
    public String getResultDetails(){
        return details+"\n\nNumber of jobs: "+jobCounter+"\nSuccessful finished: "+successCounter+"\nUnsuccessful finished:"+errorCounter;
    }


}
