package mg.mgmap.test;

import android.graphics.PointF;
import android.util.Log;

import java.util.ArrayList;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;

public class Testcase {

    protected long timeout = 15000;
    protected TestControl tc;
    protected String name;
    protected ArrayList<String> regexs = new ArrayList<>();
    protected ArrayList<String> matches = new ArrayList<>();

    LogMatcher lm;
    boolean running = false;
    int level;
    int lineCnt;

    public Testcase(TestControl tc) {
        this(tc, Log.INFO);
    }

    public Testcase(TestControl tc, int level){
        this.tc = tc;
        this.name = this.getClass().getSimpleName();
        this.level = level;
    }

    public void start(){
        Log.i(MGMapApplication.LABEL, NameUtil.context()+"  "+name);
        running = true;
        lm = new LogMatcher(level);
        addRegexs();
        lm.startMatch(regexs, matches);
        tc.setPos( new PointF(50,50));
        setup();
        tc.setCursorVisibility(true);
    }
    public void stop(){
        if (running){
            running = false;
        }
    }

    public long getTimeout() {
        return timeout;
    }

    public boolean isRunning() {
        return running;
    }

    protected void setup(){}

    protected void addRegexs(){}

    protected void addRegex(String regex){
        if (!regex.startsWith(".*")){
            regex = ".*"+regex;
        }
        if (!regex.endsWith(".*")){
            regex = regex+".*";
        }
        regexs.add(regex);
    }

    protected Runnable rFinal = new Runnable() {
        @Override
        public void run() {
            if (tc.currentTestcase == Testcase.this){
                lineCnt = lm.stopMatch();
                tc.setCursorVisibility(false);
                running = false;
            }
        }
    };

    public void logResult(StringBuilder sbSummary){
        boolean success = (matches.size() == regexs.size());
        String result = (success?"passed":"failed")+" "+name  +" (lineCnt="+lineCnt+")";
        sbSummary.append(System.lineSeparator()).append(result);
        Log.println(success?Log.INFO:Log.ERROR, MGMapApplication.LABEL, NameUtil.context()+ " TESTCASE Result: "+result);
        for (String s : regexs){
            Log.println(success?Log.VERBOSE:Log.INFO, MGMapApplication.LABEL, NameUtil.context()+ " R "+s);
        }
        for (String s : matches){
            Log.println(success?Log.VERBOSE:Log.INFO, MGMapApplication.LABEL, NameUtil.context()+ " M "+s);
        }
    }

    protected TestView getTestView(){
        return tc.testView;
    }
}
