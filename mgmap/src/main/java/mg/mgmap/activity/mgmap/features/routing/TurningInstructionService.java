/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.activity.mgmap.features.routing;

import android.content.Context;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.model.ExtendedPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogRefApproach;
import mg.mgmap.generic.model.TrackLogSegment;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.PrefCache;

public class TurningInstructionService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final int THRESHOLD_FAR = 200;
    private static final int THRESHOLD_NEAR = 40;
    private static final int THRESHOLD_KURS = 100; // distance for kurs calculation
    private static final int AWAY_REPETITION_THRESHOLD = 2;

    private enum ServiceState { OFF , INIT, ON }
    private final Pref<Boolean> prefRoutingHints;
    private final Pref<Boolean> prefGps;

    private int  mediumAwayCnt = 0;
    private double lastAwayDistance = 0;
    private PointModel lastHintPoint = null;
    private boolean lastHintClose = false;
    private TrackLogRefApproach lastHintApproach = null;

    private ServiceState serviceState = ServiceState.OFF;
    private TextToSpeech tts = null;

    final Context context;
    final PrefCache prefCache;
    final MGMapApplication application;

    public TurningInstructionService(MGMapApplication application, Context context, PrefCache prefCache){
        this.context = context;
        this.prefCache = prefCache;
        this.application = application;

        prefRoutingHints = prefCache.get(R.string.FSRouting_qc_RoutingHint, false);
        prefRoutingHints.addObserver((e) -> {
            if (prefRoutingHints.getValue()){
                tryStartTTSService();
            } else {
                tryStopTTSService();
            }
        });
        prefGps = prefCache.get(R.string.FSPosition_pref_GpsOn, false);
    }

    private void tryStartTTSService(){
        if (serviceState == ServiceState.OFF){
            serviceState = ServiceState.INIT;
            mgLog.i("TextToSpeech start");
            tts = new TextToSpeech(context, status -> {
                mgLog.i("TextToSpeech started status="+status);
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.GERMAN);
                    serviceState = ServiceState.ON;
                }
            });
        }
    }

    private void tryStopTTSService(){
        if (serviceState != ServiceState.OFF){
            serviceState  = ServiceState.OFF;
            if (tts != null) {
                tts.stop();
                tts.shutdown();
                tts = null;
                mgLog.i("TextToSpeech stoped");
            }
        }
    }


    public void handleNewPoint(PointModel pm) {
        try {
            TrackLog routeTrackLog = application.routeTrackLogObservable.getTrackLog();
            if (routeTrackLog == null) return;

            if ((tts != null) && (serviceState == ServiceState.ON) && prefGps.getValue()){
                mgLog.i("lastGps="+pm);
                if (pm != null) { // have a new position to handle
                    TrackLogRefApproach bestMatch = routeTrackLog.getBestDistance(pm, THRESHOLD_FAR);
                    if ((bestMatch != null)){
                        if (bestMatch.getDistance() < THRESHOLD_NEAR){
                            checkHints(mediumAwayCnt > AWAY_REPETITION_THRESHOLD, bestMatch);
                            mediumAwayCnt = 0;
                            lastAwayDistance = 0;
                        } else {
                            // not really close
                            if (bestMatch.getDistance() > lastAwayDistance+10){
                                mediumAwayCnt++;
                                lastAwayDistance = bestMatch.getDistance();
                                mgLog.i("away="+mediumAwayCnt);
                                if (mediumAwayCnt <= AWAY_REPETITION_THRESHOLD){ // don't repeat all the time
                                    StringBuilder text = new StringBuilder();
                                    for (int i=0;i<mediumAwayCnt;i++) text.append("Achtung! ");
                                    int abstand = (int)bestMatch.getDistance();
                                    text.append("Abstand ").append(abstand).append(" Meter");
                                    mgLog.i("away="+mediumAwayCnt+" text="+text);
                                    tts.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null, "ABCDEF");
                                } else {
                                    if (mediumAwayCnt == AWAY_REPETITION_THRESHOLD+1){
                                        String text = "Track verlassen";
                                        mgLog.i("away="+mediumAwayCnt+" text="+text);
                                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ABCDEF");
                                    }
                                }
                            }
                        }
                    } else {
                        mgLog.i("far away");
                        if (mediumAwayCnt <= AWAY_REPETITION_THRESHOLD ){
                            String text = "Großer Abstand, mehr als "+ (THRESHOLD_FAR)+" Meter, Track verlassen";
                            mgLog.i("away="+mediumAwayCnt+" text="+text);
                            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "ABCDEF");
                            mediumAwayCnt = AWAY_REPETITION_THRESHOLD+1;
                        }
                        // far away ==> else do nothing
                    }
                }
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
    }


    private void checkHints(boolean backOnTrack, TrackLogRefApproach bestMatch){
        boolean abortTTS = false;
        int abstand = (int)bestMatch.getDistance();
        StringBuilder text = new StringBuilder();
        mgLog.d("SegIdx="+bestMatch.getSegmentIdx()+" epIdx="+bestMatch.getEndPointIndex()+" HINT Abstand="+abstand);

        if (PointModelUtil.distance(bestMatch.getTrackLog().getPointList(bestMatch,null)) < THRESHOLD_NEAR) {
            text.append("Ziel erreicht.");
            abortTTS = true;
        } else {
            if (!prefCache.get(R.string.preferences_minimalTurningInstruction_key, false).getValue()){
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
                        mgLog.d("Kurs "+segment.get(bestMatch.getEndPointIndex()-1)+" "+bestMatch.getApproachPoint()+" "+pmx+" "+courseDegree+" "+courseClock);
                        if (((0 < courseDegree) && (courseDegree < 150)) || ((210 < courseDegree) && (courseDegree < 360))) {
                            if (text.length() != 0){ // add Kurs only, if there is a hint
                                text.append(" Kurs ").append(courseClock).append(" Uhr");
                            }
                        }
                        break;
                    }

                    routeDistance += newDistance;

                    RoutingHint hint = null;
                    if (pm instanceof ExtendedPointModel<?>) {
                        @SuppressWarnings("unchecked")
                        ExtendedPointModel<RoutingHint> epm = (ExtendedPointModel<RoutingHint>) pm;
                        hint = epm.getExtent();
                    }
                    if (hint != null){
                        mgLog.d("HINT d="+routeDistance+" w="+hint.numberOfPathes+" deg="+hint.directionDegree+" c="+PointModelUtil.clock4degree(hint.directionDegree)
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
                                text.append(bClose ? (numHints == 0 ? "Gleich " : "Danach ") : (" In " + (int) routeDistance + " Meter ")).append(clock).append("Uhr ");
                                if (cond2){ // next path left is less than 45degree beside direction degree
                                    text.append("Nicht links ").append(PointModelUtil.clock4degree(hint.nextLeftDegree)).append(" Uhr ");
                                }
                                if (cond3){ // next path right is less than 45degree beside direction degree
                                    text.append("Nicht rechts ").append(PointModelUtil.clock4degree(hint.nextRightDegree)).append(" Uhr ");
                                }

                                numHints++;

                            }
                        }
                    } else {
                        mgLog.d("HINT d="+routeDistance);
                    }
                    lastPm = pm;
                }
            }
            if (backOnTrack){
                text.insert(0,"on track ");
            }
            if (text.length() == 0){
                if (PointModelUtil.distance(bestMatch.getTrackLog().getPointList(lastHintApproach,bestMatch)) > 500){
                    text.append("on track");
                }
            }
        }

        if (text.length() != 0){
            mgLog.i("TTS: "+text);
            tts.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, null, "ABCDEF");
            lastHintApproach = bestMatch;
            if (abortTTS){
                SystemClock.sleep(2000);
                prefRoutingHints.setValue(false);
            }
        }
    }

}
