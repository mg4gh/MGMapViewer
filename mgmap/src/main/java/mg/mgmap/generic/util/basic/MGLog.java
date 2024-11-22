package mg.mgmap.generic.util.basic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

@SuppressWarnings({"unused"})
public class MGLog {

    public enum Level { TEST1, TEST2, VERBOSE, DEBUG, INFO, WARN, ERROR, TEST3}

    public static Level levelByOrdinal(int ordinal){
        Level[] levels = Level.values();
        for (Level value : levels) {
            if (value.ordinal() == ordinal) return value;
        }
        return null;
    }

    private static boolean unittest = false;

    public static boolean isUnittest() {
        return unittest;
    }
    public static void setUnittest(boolean unittest) {
        MGLog.unittest = unittest;
    }

    public static TreeMap<String, Level> logConfig = new TreeMap<>();
    private static final ArrayList<MGLogObserver> logObservers = new ArrayList<>();

    public static void addLogObserver(MGLogObserver observer){
        logObservers.add(observer);
    }
    public static void removeLogObserver(MGLogObserver observer){
        logObservers.remove(observer);
    }

    public static void se(String msg, Throwable t){
        log(Level.ERROR, msg + getStackTraceString(t));
    }
    public static void se(String msg){
        log(Level.ERROR, msg);
    }
    public static void se(Object o){
        log(Level.ERROR, o==null?"":o.toString());
    }
    public static void se(MGLogCB logCB){
        log(Level.ERROR, logCB.logCB());
    }

    public static void sw(String msg){
        log(Level.WARN, msg);
    }
    public static void sw(Object o){
        log(Level.WARN, o==null?"":o.toString());
    }
    public static void sw(MGLogCB logCB){
        log(Level.WARN, logCB.logCB());
    }

    public static void si(String msg){
        log(Level.INFO, msg);
    }
    public static void si(Object o){
        log(Level.INFO, o==null?"":o.toString());
    }
    public static void si(MGLogCB logCB){
        log(Level.INFO, logCB.logCB());
    }

    public static void sd(String msg){
        log(Level.DEBUG, msg);
    }
    public static void sd(Object o){
        log(Level.DEBUG, o==null?"":o.toString());
    }
    public static void sd(MGLogCB logCB){
        log(Level.DEBUG, logCB.logCB());
    }

    public static void sv(String msg){
        log(Level.VERBOSE, msg);
    }
    public static void sv(Object o){
        log(Level.VERBOSE, o==null?"":o.toString());
    }
    public static void sv(MGLogCB logCB){
        log(Level.VERBOSE, logCB.logCB());
    }


    public static void log(Level level, String msg){
        StackTraceElement ste = new Throwable().getStackTrace()[2];
        String tag = ste.getClassName();
        msg = NameUtil.context(ste)+msg;
        println(level, tag, msg);
        for (MGLogObserver observer : logObservers){
            observer.processLog(level, tag, msg);
        }
    }
    public static void ctx(Level level, int depth){
        StackTraceElement[] stes = new Throwable().getStackTrace();
        StringBuilder msg = new StringBuilder(NameUtil.context(stes[1]));
        String tag = stes[1].getClassName();
        for (int i=2; (i<2+depth)&&(i<stes.length); i++){
            StackTraceElement ste = stes[i];
            msg.append("\n        ").append(NameUtil.context(ste));
        }
        println(level, tag, msg.toString());
    }


    public String tag;
    public Level level;

    public MGLog(String tag){
        this.tag = tag;
        evaluateLevel();
    }

    public void evaluateLevel(){
        String tag = this.tag;
        level = Level.INFO; // default
        while (!tag.isEmpty()){
            if (logConfig.containsKey(tag)){
                level = logConfig.get(tag);
                break;
            } else {
                int idx = Math.max( tag.lastIndexOf("."), 0);
                tag = tag.substring(0,idx);
            }
        }
        v(this.tag+" : "+level);
    }

    public void e(Throwable t){
        if (level.ordinal() <= Level.ERROR.ordinal()){
            log(Level.ERROR,  getStackTraceString(t));
        }
    }
    public void e(String msg, Throwable t){
        if (level.ordinal() <= Level.ERROR.ordinal()){
            log(Level.ERROR, msg + getStackTraceString(t));
        }
    }
    public void e(String msg){
        if (level.ordinal() <= Level.ERROR.ordinal()){
            log(Level.ERROR, msg);
        }
    }
    public void e(Object o){
        if (level.ordinal() <= Level.ERROR.ordinal()) {
            log(Level.ERROR, o == null ? "" : o.toString());
        }
    }
    public void e(MGLogCB logCB){
        if (level.ordinal() <= Level.ERROR.ordinal()){
            log(Level.ERROR, logCB.logCB());
        }
    }

    public void w(String msg, Throwable t){
        if (level.ordinal() <= Level.WARN.ordinal()){
            log(Level.WARN, msg + getStackTraceString(t));
        }
    }
    public void w(String msg){
        if (level.ordinal() <= Level.WARN.ordinal()){
            log(Level.WARN, msg);
        }
    }
    public void w(Object o){
        if (level.ordinal() <= Level.WARN.ordinal()) {
            log(Level.WARN, o == null ? "" : o.toString());
        }
    }
    public void w(MGLogCB logCB){
        if (level.ordinal() <= Level.WARN.ordinal()){
            log(Level.WARN, logCB.logCB());
        }
    }

    public void i(){
        if (level.ordinal() <= Level.INFO.ordinal()){
            log(Level.INFO, "");
        }
    }
    public void i(String msg){
        if (level.ordinal() <= Level.INFO.ordinal()){
            log(Level.INFO, msg);
        }
    }
    public void i(Object o){
        if (level.ordinal() <= Level.INFO.ordinal()) {
            log(Level.INFO, o == null ? "" : o.toString());
        }
    }
    public void i(MGLogCB logCB){
        if (level.ordinal() <= Level.INFO.ordinal()){
            log(Level.INFO, logCB.logCB());
        }
    }

    public void d(){
        if (level.ordinal() <= Level.DEBUG.ordinal()){
            log(Level.DEBUG, "");
        }
    }
    public void d(String msg){
        if (level.ordinal() <= Level.DEBUG.ordinal()){
            log(Level.DEBUG, msg);
        }
    }
    public void d(Object o){
        if (level.ordinal() <= Level.DEBUG.ordinal()) {
            log(Level.DEBUG, o == null ? "" : o.toString());
        }
    }
    public void d(MGLogCB logCB){
        if (level.ordinal() <= Level.DEBUG.ordinal()){
            log(Level.DEBUG, logCB.logCB());
        }
    }

    public void v(String msg){
        if (level.ordinal() <= Level.VERBOSE.ordinal()){
            log(Level.VERBOSE, msg);
        }
    }
    public void v(Object o){
        if (level.ordinal() <= Level.VERBOSE.ordinal()) {
            log(Level.VERBOSE, o == null ? "" : o.toString());
        }
    }
    public void v(MGLogCB logCB){
        if (level.ordinal() <= Level.VERBOSE.ordinal()){
            log(Level.VERBOSE, logCB.logCB());
        }
    }

    public void any(Level level, Object o){
        if (this.level.ordinal() <= level.ordinal()){
            log(level, o == null ? "" : o.toString());
        }
    }
    public void any(Level level, String msg){
        if (this.level.ordinal() <= level.ordinal()){
            log(level, msg);
        }
    }
    public void any(Level level, MGLogCB logCB){
        if (this.level.ordinal() <= level.ordinal()){
            log(level, logCB.logCB());
        }
    }
    public void any(Level level, int depth){
        if (this.level.ordinal() <= level.ordinal()){
            ctx(level, depth);
        }
    }

    private static void println(Level level, String tag, String msg){
        if (isUnittest()){
            System.out.printf("%s  %s: %s -- %s%n", Formatter.SDF_LOG.format(new Date(System.currentTimeMillis())), level, tag, msg);
        } else {
            android.util.Log.println(level.ordinal(), tag, msg);
        }
    }



    // copied from android.util.Log
    public static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return "";
        }

        // This is to reduce the amount of log spew that apps do in the non-error
        // condition of the network being unavailable.
        Throwable t = tr;
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return "";
            }
            t = t.getCause();
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        tr.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

}



