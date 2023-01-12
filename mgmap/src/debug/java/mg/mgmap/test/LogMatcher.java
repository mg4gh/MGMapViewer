package mg.mgmap.test;

import android.util.Log;

import java.util.ArrayList;

import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.basic.MGLogObserver;

public class LogMatcher implements MGLogObserver {

    MGLog.Level level;
    int lineCnt = 0;

    public LogMatcher(MGLog.Level level){
        this.level = level;
    }


    @Override
    public void processLog(MGLog.Level level, String tag, String message) {
        if (matchRunning && (this.level.ordinal() <= level.ordinal()) && (regexs.size()>0)){
            boolean lineMatch = message.matches(regexs.get(0));
//                                if (!message.contains("          **** "))  Log.d("mg.mgmap","          **** "+lineMatch+" "+message);
            if (lineMatch){
                regexs.remove(0);
                matches.add(message);
                Log.i(LogMatcher.class.getName(),"  LogMatcher add line: "+matches.size()+" "+message.substring(0,Math.min(80,message.length()))+" ...");
            }
            lineCnt++;
        }
    }

    private ArrayList<String> regexs;
    private ArrayList<String> matches;
    boolean matchRunning = false;

    public void startMatch(ArrayList<String> regexs, ArrayList<String> matches){
        matchRunning = true;
        this.regexs = new ArrayList<>(regexs); // use internally a clone
        this.matches = matches;
        MGLog.addLogObserver(this);
    }
    public int stopMatch(){
        MGLog.removeLogObserver(this);
        matchRunning = false;
        return lineCnt;
    }

}
