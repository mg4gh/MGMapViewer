package mg.mapviewer.features.routing;

import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogRefApproach;
import mg.mapviewer.model.TrackLogSegment;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PointModelUtil;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.view.ExtendedTextView;

public class MSRoutingHintService extends MGMicroService {

    private static final int THRESHOLD_FAR = 200;
    private static final int THRESHOLD_NEAR = 40;
    private static final int THRESHOLD_KURS = 100; // distance for kurs calculation
    private static final int TT_REFRESH_TIME = 150;

    private enum ServiceState { OFF , INIT, ON };
    private final MGPref<Boolean> prefRoutingHints = MGPref.get(R.string.MSRouting_qc_RoutingHint, false);
    private final MGPref<Boolean> prefRoutingHintsEnabled = MGPref.anonymous(false);
    private final MGPref<Boolean> prefGps = MGPref.get(R.string.MSPosition_prev_GpsOn, false);
    private final MGPref<Boolean> prefMtlVisibility = MGPref.get(R.string.MSMarker_pref_MTL_visibility, false);

    private int  mediumAwayCnt = 0;
    private PointModel lastHintPoint = null;
    private boolean lastHintClose = false;

    private ServiceState serviceState = ServiceState.OFF;
    private TextToSpeech tts = null;

    public MSRoutingHintService(MGMapActivity mmActivity){
        super(mmActivity);

        if (MGPref.get(R.string.MGMapApplication_pref_Restart, false).getValue()){
            prefRoutingHints.setValue(false);
        }
        prefRoutingHints.addObserver(new RoutingHintObserver());
        prefRoutingHints.onChange();
        Observer routingHintsEnabledObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                prefRoutingHintsEnabled.setValue( prefGps.getValue() && prefMtlVisibility.getValue());
                if (!prefRoutingHintsEnabled.getValue()){
                    prefRoutingHints.setValue(false);
                }
            }
        };
        prefGps.addObserver(routingHintsEnabledObserver);
        prefMtlVisibility.addObserver(routingHintsEnabledObserver);

    }

    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info) {
        etv.setData(prefRoutingHints,R.drawable.routing_hints2, R.drawable.routing_hints1);
        etv.setPrAction(prefRoutingHints);
        etv.setDisabledData(prefRoutingHintsEnabled, R.drawable.routing_hints_dis);
        etv.setHelp(r(R.string.MSRouting_qcRoutingHint_Help)).setHelp(r(R.string.MSRouting_qcRoutingHint_Help),r(R.string.MSRouting_qcRoutingHint_Help));
        return etv;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        tryStopTTSService();
    }

    public class RoutingHintObserver implements Observer {
        @Override
        public void update(Observable o, Object arg) {
            if (prefRoutingHints.getValue()){
                tryStartTTSService();
            } else {
                tryStopTTSService();
            }
        }
    }

    private void tryStartTTSService(){
        if (serviceState == ServiceState.OFF){
            serviceState = ServiceState.INIT;
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" TextToSpeech start");
            tts = new TextToSpeech(getApplication().getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    Log.i(MGMapApplication.LABEL, NameUtil.context()+" TextToSpeech started status="+status);
                    if(status != TextToSpeech.ERROR) {
                        tts.setLanguage(Locale.GERMAN);
                        serviceState = ServiceState.ON;
                    }
                }
            });
            getApplication().lastPositionsObservable.addObserver(refreshObserver);
        }
    }

    private void tryStopTTSService(){
        if (serviceState != ServiceState.OFF){
            serviceState  = ServiceState.OFF;
            getApplication().lastPositionsObservable.deleteObserver(refreshObserver);
            if (tts != null) {
                tts.stop();
                tts.shutdown();
                tts = null;
                Log.i(MGMapApplication.LABEL, NameUtil.context() + " TextToSpeech stoped");
            }
        }
    }


    @Override
    protected void doRefresh() {
        handleNewPoint();
    }


    protected void handleNewPoint() {
        try {
            MSRouting msRouting = getApplication().getMS(MSRouting.class);
            if (msRouting == null) return;
            TrackLog routeTrackLog = getApplication().routeTrackLogObservable.getTrackLog();
            if (routeTrackLog == null) return;

            if ((tts != null) && (serviceState == ServiceState.ON) && prefGps.getValue()){
                PointModel last1Gps = getApplication().lastPositionsObservable.lastGpsPoint;

                Log.i(MGMapApplication.LABEL, NameUtil.context()+" lastGps="+last1Gps);
                if (last1Gps != null) { // have a new position to handle
                    TrackLogRefApproach bestMatch = routeTrackLog.getBestDistance(last1Gps, THRESHOLD_FAR);
                    if ((bestMatch != null)){
                        if (bestMatch.getDistance() < THRESHOLD_NEAR){
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
