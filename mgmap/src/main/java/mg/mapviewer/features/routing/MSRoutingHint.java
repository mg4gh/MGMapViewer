package mg.mapviewer.features.routing;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.model.TrackLogRefApproach;
import mg.mapviewer.model.TrackLogSegment;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.PointModelUtil;

public class MSRoutingHint extends MGMicroService {

    private static final int THRESHOLD_FAR = 200;

    private boolean running = false;
    private PointModel lastPos = null;
    private MSRouting msRouting = null;
    private int  mediumAwayCnt = 0;

    public MSRoutingHint(MGMapActivity mmActivity) {
        super(mmActivity);
        ttRefreshTime = 200;
    }


    public void startService(){
        msRouting = getActivity().getMS(MSRouting.class);
        startTTS(getApplication().getApplicationContext());
        getApplication().lastPositionsObservable.addObserver(refreshObserver);
    }

    public void stopService(){
        getApplication().lastPositionsObservable.deleteObserver(refreshObserver);
        stopTTS();
    }

    @Override
    protected void doRefresh() {
        try {
            if ((tts != null) && running && getApplication().gpsOn.getValue()&& (msRouting.routeTrackLog != null)){
                PointModel lastGps = getApplication().lastPositionsObservable.lastGpsPoint;
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" lastPos="+lastPos+" lastGps="+lastGps+" secondLastGpsPoint="+getApplication().lastPositionsObservable.secondLastGpsPoint);
                if ((lastGps != null) && ((lastPos == null) || (PointModelUtil.compareTo(lastGps,lastPos)!=0))) { // have a new position to handle
                    TrackLogRefApproach bestMatch = msRouting.routeTrackLog.getBestDistance(lastGps, THRESHOLD_FAR);
                    double speed = 4; // in m/s - estimate, if no info
                    PointModel secondLastGps = getApplication().lastPositionsObservable.secondLastGpsPoint;

                    if ((secondLastGps != null) && ( lastGps.getTimestamp() - secondLastGps.getTimestamp() <10000)){ // less than 10s time diff
                        speed = PointModelUtil.distance(lastGps,secondLastGps)*1000 /(lastGps.getTimestamp() - secondLastGps.getTimestamp());
                    }
                    double distSinceLastPos = (System.currentTimeMillis() - lastGps.getTimestamp()) * speed / 1000;


                    if ((bestMatch != null)){
                        if (bestMatch.getDistance() < PointModelUtil.getCloseThreshold()){
                            checkHints(bestMatch, distSinceLastPos);
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

                lastPos = lastGps;
            }
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL,NameUtil.context(),e);
        }
    }

    private TextToSpeech tts = null;

    private void startTTS(Context context){
        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" TextToSpeech started status="+status);
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.GERMAN);
                    running = true;
                    refreshObserver.onChange();
                }
            }
        });
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" TextToSpeech start");
    }

    private void stopTTS(){
        running = false;
        if (tts != null){
            tts.stop();
            tts.shutdown();
            Log.i(MGMapApplication.LABEL, NameUtil.context()+" TextToSpeech stoped");
            tts = null;
        }
    }


    private void checkHints(TrackLogRefApproach bestMatch, double distSinceLastPos){
        int abstand = (int)bestMatch.getDistance();
        String text = "";
        Log.i(MGMapApplication.LABEL, NameUtil.context()+"SegIdx="+bestMatch.getSegmentIdx()+" epIdx="+bestMatch.getEndPointIndex()+" HINT Abstand="+abstand+" distSinceLastPos="+distSinceLastPos);

        PointModel lastPm = bestMatch.getApproachPoint();
        double routeDistance = 0;
        int numHints = 0;
        TrackLogSegment segment = msRouting.routeTrackLog.getTrackLogSegment(bestMatch.getSegmentIdx());
        for (int pmIdx=bestMatch.getEndPointIndex(); pmIdx<segment.size(); pmIdx++){
            PointModel pm = segment.get(pmIdx);
            double newDistance = PointModelUtil.distance(lastPm,pm);
            if (routeDistance+newDistance > 100) { // some threshold
                PointModel pmx = PointModelUtil.interpolate(lastPm,pm,100-routeDistance);


                double kursDegree = PointModelUtil.calcDegree( msRouting.routeTrackLog.getTrackLogSegment(bestMatch.getSegmentIdx()).get(bestMatch.getEndPointIndex()-1), bestMatch.getApproachPoint() , pmx );
                int kursClock = PointModelUtil.clock4degree( kursDegree );
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" Kurs "+msRouting.routeTrackLog.getTrackLogSegment(bestMatch.getSegmentIdx()).get(bestMatch.getEndPointIndex()-1)+" "+bestMatch.getApproachPoint()+" "+pmx+" "+kursDegree+" "+kursClock);
                if (((0 < kursDegree) && (kursDegree < 150)) || ((210 < kursDegree) && (kursDegree < 360))) text += " Kurs "+kursClock+" Uhr";
                break;
            }

            routeDistance += newDistance;

            RoutingHint hint = msRouting.routePointMap2.get(pm).routingHints.get(pm);
            if (hint != null){
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" HINT d="+routeDistance+" w="+hint.numberOfPathes+" deg="+hint.directionDegree+" c="+PointModelUtil.clock4degree(hint.directionDegree)
                        +" l="+PointModelUtil.clock4degree(hint.nextLeftDegree)+" r="+PointModelUtil.clock4degree(hint.nextRightDegree));
                if (hint.numberOfPathes > 2){
                    int clock = PointModelUtil.clock4degree(hint.directionDegree);
                    boolean cond1 = ((hint.directionDegree < 150) || (hint.directionDegree > 210));
                    boolean cond2 = ((hint.nextLeftDegree > 0) && ((hint.directionDegree-hint.nextLeftDegree)<45));
                    boolean cond3 = ((hint.nextRightDegree < 360) && ((hint.nextRightDegree-hint.directionDegree)<45));

                    if ((cond1 || cond2 || cond3) && (numHints<2)){
                        text += ((routeDistance<30)?(numHints==0?"Gleich ":"Danach "):(" In "+(int)routeDistance+" Meter "))+clock+ "Uhr ";
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
