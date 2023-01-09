package mg.mgmap.generic.util.basic;

public interface MGLogObserver {

    void processLog(MGLog.Level level, String tag, String message);
}
