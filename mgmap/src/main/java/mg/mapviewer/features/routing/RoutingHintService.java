package mg.mapviewer.features.routing;

import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogRefApproach;
import mg.mapviewer.model.TrackLogSegment;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PointModelUtil;
import mg.mapviewer.util.pref.MGPref;
import mg.mapviewer.view.PrefTextView;

public class RoutingHintService {

    private static final int THRESHOLD_FAR = 200;
    private static final int THRESHOLD_KURS = 100;
    private static final int TT_REFRESH_TIME = 150;

    private static RoutingHintService instance;
    public static RoutingHintService getInstance(){
        return instance;
    }

    private enum ServiceState { OFF , INIT, ON };
    private final MGPref<Boolean> prefRoutingHints = MGPref.get(R.string.MSRouting_qc_RoutingHint, false);
    private final MGPref<Boolean> prefGps = MGPref.get(R.string.MSPosition_prev_GpsOn, false);

    public void initQuickControl(PrefTextView ptv, String info) {
        ptv.appendPrefData(new MGPref[]{prefRoutingHints},
                new int[]{R.drawable.mtlr3, R.drawable.mtlr4});
    }

    private int  mediumAwayCnt = 0;
    private PointModel lastHintPoint = null;
    private boolean lastHintClose = false;

    private final MGMapApplication application;
    private final Handler timer;
    private ServiceState serviceState = ServiceState.OFF;
    private TextToSpeech tts = null;

    public RoutingHintService(MGMapApplication application){
        if (instance == null){
            instance = this;
        } else {
            String error = "Only one Instance allowed";
            Log.e(MGMapApplication.LABEL, NameUtil.context()+ error);
            throw new RuntimeException(error);
        }
        this.application = application;
        this.timer = new Handler();
        prefRoutingHints.setValue(false);
        prefRoutingHints.addObserver(new RoutingHintObserver());
    }

    public class RoutingHintObserver implements Observer {

        @Override
        public void update(Observable o, Object arg) {

            if (prefRoutingHints.getValue()){
                if (serviceState == ServiceState.OFF){
                    serviceState = ServiceState.INIT;
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" TextToSpeech start");
                    tts = new TextToSpeech(application.getApplicationContext(), new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" TextToSpeech started status="+status);
                            if(status != TextToSpeech.ERROR) {
                                tts.setLanguage(Locale.GERMAN);
                                serviceState = ServiceState.ON;
                            }
                        }
                    });
                    application.lastPositionsObservable.addObserver(locationObserver);
                }
            } else {
                if (serviceState != ServiceState.OFF){
                    serviceState  = ServiceState.OFF;
                    application.lastPositionsObservable.deleteObserver(locationObserver);
                    if (tts != null) {
                        tts.stop();
                        tts.shutdown();
                        tts = null;
                        Log.i(MGMapApplication.LABEL, NameUtil.context() + " TextToSpeech stoped");
                    }
                }
            }


        }
    }


    public Observer locationObserver = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            timer.removeCallbacks(ttRefresh);
            timer.postDelayed(ttRefresh, TT_REFRESH_TIME);
        }
    };

    private Runnable ttRefresh = new Runnable() {
        @Override
        public void run() {
            handleNewPoint();
        }
    };





    protected void handleNewPoint() {
        try {
            MSRouting msRouting = application.getMS(MSRouting.class);
            if (msRouting == null) return;
            TrackLog routeTrackLog = application.routeTrackLogObservable.getTrackLog();
            if (routeTrackLog == null) return;

            if ((tts != null) && (serviceState == ServiceState.ON) && prefGps.getValue()){
                PointModel last1Gps = application.lastPositionsObservable.lastGpsPoint;

                Log.i(MGMapApplication.LABEL, NameUtil.context()+" lastGps="+last1Gps);
                if (last1Gps != null) { // have a new position to handle
                    TrackLogRefApproach bestMatch = routeTrackLog.getBestDistance(last1Gps, THRESHOLD_FAR);
                    if ((bestMatch != null)){
                        if (bestMatch.getDistance() < PointModelUtil.getCloseThreshold()){
                            checkHints(msRouting, bestMatch);
                            mediumAwayCnt = 0;
                        } else {
                            // not really close
                            mediumAwayCnt++;
                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" away="+mediumAwayCnt);
                            if (mediumAwayCnt <=3){ // don't repeat all the time
                                String text = "";
                                for (int i=0;i<mediumAwayCnt;i++) text += "Achtung! ";
                                int abstand = (int)bestMatch.getDistance();
                                text += "Abstand "+ (abstand)+" Meter";
                                Log.i(MGMapApplication.LABEL, NameUtil.context()+" away="+mediumAwayCnt+" text="+text);
                                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ABCDEF");
                            }
                        }
                    } else {
                        Log.i(MGMapApplication.LABEL, NameUtil.context()+" far away");
                        if (mediumAwayCnt <= 3 ){
                            String text = "Großer Abstand mehr als "+ (THRESHOLD_FAR)+" Meter";
                            Log.i(MGMapApplication.LABEL, NameUtil.context()+" away="+mediumAwayCnt+" text="+text);
                            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ABCDEF");
                            mediumAwayCnt = 4;
                        }
                        // far away ==> else do nothing
                    }
                }
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL,NameUtil.context(),e);
        }
    }


    private void checkHints(MSRouting msRouting, TrackLogRefApproach bestMatch){
        int abstand = (int)bestMatch.getDistance();
        String text = "";
        Log.i(MGMapApplication.LABEL, NameUtil.context()+"SegIdx="+bestMatch.getSegmentIdx()+" epIdx="+bestMatch.getEndPointIndex()+" HINT Abstand="+abstand);

        PointModel lastPm = bestMatch.getApproachPoint();
        double routeDistance = 0;
        int numHints = 0;
        TrackLogSegment segment = bestMatch.getSegment();
        for (int pmIdx=bestMatch.getEndPointIndex(); pmIdx<segment.size(); pmIdx++){
            PointModel pm = segment.get(pmIdx);
            double newDistance = PointModelUtil.distance(lastPm,pm);
            if (routeDistance+newDistance > THRESHOLD_KURS) { // some threshold
                PointModel pmx = PointModelUtil.interpolate(lastPm,pm,THRESHOLD_KURS-routeDistance);

                double courseDegree = PointModelUtil.calcDegree( segment.get(bestMatch.getEndPointIndex()-1), bestMatch.getApproachPoint() , pmx );
                int courseClock = PointModelUtil.clock4degree( courseDegree );
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" Kurs "+segment.get(bestMatch.getEndPointIndex()-1)+" "+bestMatch.getApproachPoint()+" "+pmx+" "+courseDegree+" "+courseClock);
                if (((0 < courseDegree) && (courseDegree < 150)) || ((210 < courseDegree) && (courseDegree < 360))) {
                    if (text.length() > 0 ){ // add Kurs only, if there is a hint
                        text += " Kurs "+courseClock+" Uhr";
                    }
                }
                break;
            }

            routeDistance += newDistance;

            RoutingHint hint = msRouting.routePointMap2.get(pm).routingHints.get(pm);
            if (hint != null){
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" HINT d="+routeDistance+" w="+hint.numberOfPathes+" deg="+hint.directionDegree+" c="+PointModelUtil.clock4degree(hint.directionDegree)
                        +" l="+PointModelUtil.clock4degree(hint.nextLeftDegree)+" r="+PointModelUtil.clock4degree(hint.nextRightDegree));
                int clock = PointModelUtil.clock4degree(hint.directionDegree);
                if ((hint.numberOfPathes > 2) && (clock >= 0)){
                    boolean cond1 = ((hint.directionDegree < 150) || (hint.directionDegree > 210));
                    boolean cond2 = ((hint.nextLeftDegree > 0) && ((hint.directionDegree-hint.nextLeftDegree)<45));
                    boolean cond3 = ((hint.nextRightDegree < 360) && ((hint.nextRightDegree-hint.directionDegree)<45));

                    if ((cond1 || cond2 || cond3) && (numHints<2)){
                        boolean bClose = (routeDistance < 40);
                        if (numHints == 0){
                            if ((pm == lastHintPoint) && (lastHintClose==bClose)){
                                break; // no tts for this point
                            } else {
                                lastHintPoint = pm;
                                lastHintClose = bClose;
                            }
                        }
                        text += (bClose?(numHints==0?"Gleich ":"Danach "):(" In "+(int)routeDistance+" Meter "))+clock+ "Uhr ";
                        if (cond2){ // next path left is less than 45degree beside direction degree
                            text += "Nicht links "+PointModelUtil.clock4degree(hint.nextLeftDegree)+ " Uhr ";
                        }
                        if (cond3){ // next path right is less than 45degree beside direction degree
                            text += "Nicht rechts "+PointModelUtil.clock4degree(hint.nextRightDegree)+ " Uhr ";
                        }
                        
                        numHints++;

                    }
                }
            } else {
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" HINT d="+routeDistance);
            }
            lastPm = pm;
        }

        if (text.length() > 0){
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+text);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ABCDEF");
        }
    }

}
