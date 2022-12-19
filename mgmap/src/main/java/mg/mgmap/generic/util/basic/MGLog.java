package mg.mgmap.generic.util.basic;

import android.util.Log;

import java.util.ArrayList;
import java.util.TreeMap;

@SuppressWarnings({"unused"})
public class MGLog {

    public static TreeMap<String, Integer> logConfig = new TreeMap<>();
    private static final ArrayList<MGLogObserver> logObservers = new ArrayList<>();

    public static void addLogObserver(MGLogObserver observer){
        logObservers.add(observer);
    }
    public static void removeLogObserver(MGLogObserver observer){
        logObservers.remove(observer);
    }

    public static void se(String msg, Throwable t){
        log(Log.ERROR, msg + Log.getStackTraceString(t));
    }
    public static void se(String msg){
        log(Log.ERROR, msg);
    }
    public static void se(Object o){
        log(Log.ERROR, o==null?"":o.toString());
    }
    public static void se(LogCB logCB){
        log(Log.ERROR, logCB.logCB());
    }

    public static void sw(String msg){
        log(Log.WARN, msg);
    }
    public static void sw(Object o){
        log(Log.WARN, o==null?"":o.toString());
    }
    public static void sw(LogCB logCB){
        log(Log.WARN, logCB.logCB());
    }

    public static void si(String msg){
        log(Log.INFO, msg);
    }
    public static void si(Object o){
        log(Log.INFO, o==null?"":o.toString());
    }
    public static void si(LogCB logCB){
        log(Log.INFO, logCB.logCB());
    }

    public static void sd(String msg){
        log(Log.DEBUG, msg);
    }
    public static void sd(Object o){
        log(Log.DEBUG, o==null?"":o.toString());
    }
    public static void sd(LogCB logCB){
        log(Log.DEBUG, logCB.logCB());
    }

    public static void sv(String msg){
        log(Log.VERBOSE, msg);
    }
    public static void sv(Object o){
        log(Log.VERBOSE, o==null?"":o.toString());
    }
    public static void sv(LogCB logCB){
        log(Log.VERBOSE, logCB.logCB());
    }


    public static void log(int level, String msg){
        StackTraceElement ste = new Throwable().getStackTrace()[2];
        String tag = ste.getClassName();
        msg = NameUtil.context(ste)+msg;
        android.util.Log.println(level, tag, msg);
        for (MGLogObserver observer : logObservers){
            observer.processLog(level, tag, msg);
        }
    }
    public static void ctx(int level, int depth){
        StackTraceElement[] stes = new Throwable().getStackTrace();
        for (int i=2; (i<2+depth)&&(i<stes.length); i++){
            StackTraceElement ste = stes[i];
            String tag = ste.getClassName();
            String msg = "        "+NameUtil.context(ste);
            android.util.Log.println(level, tag, msg);
        }
    }


    public String tag;
    public int level;

    public MGLog(String tag){
        this.tag = tag;
        evaluateLevel();
    }

    @SuppressWarnings("ConstantConditions")
    public void evaluateLevel(){
        String tag = this.tag;
        level = Log.INFO; // default
        while (tag.length() > 0){
            if (logConfig.containsKey(tag)){
                level = logConfig.get(tag);
                break;
            } else {
                int idx = Math.max( tag.lastIndexOf("."), 0);
                tag = tag.substring(0,idx);
            }
        }
        i(this.tag+" : "+level);
    }

    public void e(Throwable t){
        if (level <= Log.ERROR){
            log(Log.ERROR, Log.getStackTraceString(t));
        }
    }
    public void e(String msg, Throwable t){
        if (level <= Log.ERROR){
            log(Log.ERROR, msg + Log.getStackTraceString(t));
        }
    }
    public void e(String msg){
        if (level <= Log.ERROR){
            log(Log.ERROR, msg);
        }
    }
    public void e(Object o){
        if (level <= Log.ERROR) {
            log(Log.ERROR, o == null ? "" : o.toString());
        }
    }
    public void e(LogCB logCB){
        if (level <= Log.ERROR){
            log(Log.ERROR, logCB.logCB());
        }
    }

    public void w(String msg, Throwable t){
        if (level <= Log.WARN){
            log(Log.WARN, msg + Log.getStackTraceString(t));
        }
    }
    public void w(String msg){
        if (level <= Log.WARN){
            log(Log.WARN, msg);
        }
    }
    public void w(Object o){
        if (level <= Log.WARN) {
            log(Log.WARN, o == null ? "" : o.toString());
        }
    }
    public void w(LogCB logCB){
        if (level <= Log.WARN){
            log(Log.WARN, logCB.logCB());
        }
    }

    public void i(){
        if (level <= Log.INFO){
            log(Log.INFO, "");
        }
    }
    public void i(String msg){
        if (level <= Log.INFO){
            log(Log.INFO, msg);
        }
    }
    public void i(Object o){
        if (level <= Log.INFO) {
            log(Log.INFO, o == null ? "" : o.toString());
        }
    }
    public void i(LogCB logCB){
        if (level <= Log.INFO){
            log(Log.INFO, logCB.logCB());
        }
    }

    public void d(){
        if (level <= Log.DEBUG){
            log(Log.DEBUG, "");
        }
    }
    public void d(String msg){
        if (level <= Log.DEBUG){
            log(Log.DEBUG, msg);
        }
    }
    public void d(Object o){
        if (level <= Log.DEBUG) {
            log(Log.DEBUG, o == null ? "" : o.toString());
        }
    }
    public void d(LogCB logCB){
        if (level <= Log.DEBUG){
            log(Log.DEBUG, logCB.logCB());
        }
    }

    public void v(String msg){
        if (level <= Log.VERBOSE){
            log(Log.VERBOSE, msg);
        }
    }
    public void v(Object o){
        if (level <= Log.VERBOSE) {
            log(Log.VERBOSE, o == null ? "" : o.toString());
        }
    }
    public void v(LogCB logCB){
        if (level <= Log.VERBOSE){
            log(Log.VERBOSE, logCB.logCB());
        }
    }

    public void any(int level, Object o){
        if (this.level <= level){
            log(level, o == null ? "" : o.toString());
        }
    }
    public void any(int level, String msg){
        if (this.level <= level){
            log(level, msg);
        }
    }
    public void any(int level, LogCB logCB){
        if (this.level <= level){
            log(level, logCB.logCB());
        }
    }
    public void any(int level, int depth){
        if (this.level <= level){
            ctx(level, depth);
        }
    }

}
