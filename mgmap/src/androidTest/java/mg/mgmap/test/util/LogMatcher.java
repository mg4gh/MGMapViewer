package mg.mgmap.test.util;

import android.util.Log;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.basic.MGLogObserver;

public class LogMatcher implements MGLogObserver {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    MGLog.Level level;
    int lineCnt = 0;
    int pos = 0;

    public LogMatcher(MGLog.Level level){
        this.level = level;
    }


    @Override
    public void processLog(MGLog.Level level, String tag, String message) {
        if (matchRunning && (this.level.ordinal() <= level.ordinal()) && (pos < regexs.size())){
            boolean lineMatch = message.matches(regexs.get(pos));
//                                if (!message.contains("          **** "))  Log.d("mg.mgmap","          **** "+lineMatch+" "+message);
            if (lineMatch){
                pos++;
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
        this.regexs = regexs;
        this.matches = matches;
        MGLog.addLogObserver(this);
    }
    public int stopMatch(){
        MGLog.removeLogObserver(this);
        matchRunning = false;
        return lineCnt;
    }

    public boolean getResult(){
        boolean success = (matches.size() == regexs.size());
        mgLog.i ( (success?"passed":"failed")+" (lineCnt: "+lineCnt+")" );
        int cnt = 0;
        for (String s : regexs){
            mgLog.any(success?MGLog.Level.VERBOSE:MGLog.Level.INFO, " R "+(++cnt)+" "+s);
        }
        cnt = 0;
        for (String s : matches){
            mgLog.any(success?MGLog.Level.VERBOSE:MGLog.Level.INFO, " M "+(++cnt)+" "+s);
        }
        return success;
    }

}
