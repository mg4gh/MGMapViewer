package mg.mgmap.test;

import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.MapPosition;
import org.mapsforge.map.model.IMapViewPosition;
import org.mapsforge.map.view.MapView;

import java.util.ArrayList;

import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.basic.NameUtil;

public class AbstractTestCase {

    protected MGMapApplication mgMapApplication;
    protected TestControl testControl;

    protected String name;
    protected ArrayList<String> regexs = new ArrayList<>();
    protected ArrayList<String> matches = new ArrayList<>();
    protected long durationLimit = 15000;

    LogMatcher lm;
    protected boolean running = false;
    int level = Log.DEBUG;
    int lineCnt;


    public AbstractTestCase(MGMapApplication mgMapApplication) {
        this.mgMapApplication = mgMapApplication;
        this.testControl = mgMapApplication.getTestControl();
        this.name = this.getClass().getSimpleName();
    }

    public void start(){
        running = true;
        lm = new LogMatcher(level);
        addRegexs();
        lm.startMatch(regexs, matches);
        setCursorVisibility(true);
    }

    public void run(){}

    public synchronized void stop(){
        if (isRunning()){
            lineCnt = lm.stopMatch();
            setCursorVisibility(false);
            running = false;
        }
    }

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



    protected <T> T getActivity(Class<T> clazz){
        return testControl.getActivity(clazz);
    }

    protected MapView getMapView(){
        return getActivity(MGMapActivity.class).getMapsforgeMapView();
    }

    public MapPosition getMapViewPosition(MapView mapView) {
        return (MapPosition) mapView.getModel().mapViewPosition.getMapPosition();
    }

    public void setMapViewPosition(MapView mapView, MapPosition mapPosition) {
        if (running){
            getActivity(MGMapActivity.class).runOnUiThread(() -> mapView.getModel().mapViewPosition.setMapPosition(mapPosition));
        }
    }

    public Point getPoint4PointModel(PointModel pm) {
        return getActivity(MGMapActivity.class).getMapViewUtility().getPoint4PointModel(pm);
    }
    public PointModel getPointModel4Point(Point p) {
        return getActivity(MGMapActivity.class).getMapViewUtility().getPointModel4Point(p);
    }


    public Point getCenterPosition() {
        return testControl.getCenterPos();
    }
    public void setCursorPosition(Point p) {
        if (running){
            testControl.setCurrentCursorPos(p);
        }
    }
    public void setCursorVisibility(boolean visibility) {
        if (running){
            testControl.setCursorVisibility(visibility);
        }
    }

    protected void doClick() {
        if (running){
            testControl.doClick();
        }
    }
    public void animateTo(Point newPosition, int duration) {
        if (running){
            testControl.animateTo(newPosition, duration);
        }
    }
    public void swipeTo(Point newPosition, int duration) {
        if (running){
            testControl.swipeTo(newPosition, duration);
        }
    }


    public String getResult(){
        boolean success = (matches.size() == regexs.size());
        String result = (success?"passed":"failed")+" (lineCnt="+lineCnt+")";
        for (String s : regexs){
            Log.println(success?Log.VERBOSE:Log.INFO, MGMapApplication.LABEL, NameUtil.context()+ " R "+s);
        }
        for (String s : matches){
            Log.println(success?Log.VERBOSE:Log.INFO, MGMapApplication.LABEL, NameUtil.context()+ " M "+s);
        }
        return result;
    }


    public long getDurationLimit() {
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" ATC " + durationLimit);
        return durationLimit;
    }

    public boolean isRunning() {
        return running;
    }

    public String getName() {
        return name;
    }
}
