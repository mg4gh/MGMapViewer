package mg.mgmap.test;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.util.basic.NameUtil;

public class LogMatcher {

    public static void cleanup(){
        try {
            Runtime.getRuntime().exec("logcat -c");
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), e);
        }
    }

    int level;
    int lineCnt = 0;

    public LogMatcher(int level){
        this.level = level;
    }

    private void match(){
        new Thread(){
            @Override
            public void run() {
                try {
                    String sLevel = "i";
                    switch (level){
                        case Log.WARN: sLevel = "w"; break;
                        case Log.ERROR: sLevel = "e"; break;
                        case Log.DEBUG: sLevel = "d"; break;
                        case Log.VERBOSE: sLevel = "v"; break;
                    }

                    String cmd = "logcat "+ MGMapApplication.LABEL+":"+sLevel+" "+ " *:I ";
                    Process pLogcat = Runtime.getRuntime().exec(cmd);
                    InputStream is = pLogcat.getInputStream();
                    BufferedReader in = new BufferedReader(new InputStreamReader(is));
                    String line;
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" LogMatcher is running - cmd: "+cmd);

                    while (matchRunning){
                        try {
                            Thread.sleep(100);

                            while ( matchRunning && (regexs!=null) && (regexs.size() > 0) && ((line = in.readLine()) != null)){
                                boolean lineMatch = line.matches(regexs.get(0));
                                if (lineMatch){
                                    regexs.remove(0);
                                    matches.add(line);
                                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" LogMatcher add line: "+matches.size()+" "+line.substring(0,20));
                                }
                                lineCnt++;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private ArrayList<String> regexs;
    private ArrayList<String> matches;
    boolean matchRunning = false;

    public void startMatch(ArrayList<String> regexs, ArrayList<String> matches){
        matchRunning = true;
        this.regexs = new ArrayList<>(regexs); // use internally a clone
        this.matches = matches;
        match();
    }
    public int stopMatch(){
        matchRunning = false;
        return lineCnt;
    }

}
