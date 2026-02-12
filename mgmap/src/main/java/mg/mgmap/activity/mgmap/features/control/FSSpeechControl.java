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
package mg.mgmap.activity.mgmap.features.control;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.TextView;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import mg.mgmap.R;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.util.EnlargeControl;
import mg.mgmap.activity.mgmap.util.MapViewUtility;
import mg.mgmap.activity.mgmap.util.Permissions;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;

public class FSSpeechControl extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private SpeechRecognizer speechRecognizer = null;
    private final Map<String, String> wordFixes = new HashMap<>();

    private final Pref<Boolean> prefRouteDuration = getPref(R.string.FSRouting_pref_RouteDuration, true);
    private final Pref<Boolean> prefEnlargeTimeTrigger = getPref(R.string.FSTime_enlargeTimeTrigger, true);
    private final Pref<Boolean> prefEnlargeBatTrigger = getPref(R.string.FSTime_enlargeBatTrigger, true);
    private final Pref<String> prefSearchText = getPref(R.string.FSSearch_pref_SearchText, "");
    private final Pref<Boolean> prefSearchOn = getPref(R.string.FSSearch_qc_searchOn, false);
    private final Pref<Boolean> prefSearchTrigger = getPref(R.string.FSSearch_pref_SearchTrigger, false);
    private final TextView tv_enlarge;


    public FSSpeechControl(MGMapActivity activity){
        super(activity);
        initWordFixes();
        tv_enlarge = getActivity().findViewById(R.id.enlarge);
    }


    @Override
    protected void onResume() {
        super.onResume();
        refreshObserver.onChange();
    }

    @Override
    protected void onPause() {
        if (speechRecognizer != null){
            speechRecognizer.stopListening();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (speechRecognizer != null){
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }


    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    mgLog.d("KeyEvent.KEYCODE_VOLUME_UP KeyEvent.ACTION_DOWN");
                    tryVoiceControl();
                }
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    mgLog.d("KeyEvent.KEYCODE_VOLUME_DOWN KeyEvent.ACTION_DOWN");
                    if (speechRecognizer != null){
                        speechRecognizer.stopListening();
                    }
                }
                break;
        }
        return true;
    }

    public void tryVoiceControl() {
        if (!(Permissions.check(getActivity(), Manifest.permission.RECORD_AUDIO))) {
            Permissions.request(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, MGMapActivity.RECORD_AUDIO_CODE);
        } else {
            voiceControl();
        }
    }

    public void voiceControl() {

        try {
            if (speechRecognizer == null){
                mgLog.d("create SpeechRecognizer");
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getActivity());
                speechRecognizer.setRecognitionListener(new RecognitionListener(){

                    @Override
                    public void onBeginningOfSpeech() {
                        mgLog.d("onBeginningOfSpeech");
                    }

                    @Override
                    public void onBufferReceived(byte[] bytes) {
                        mgLog.d("onBufferReceived "+bytes.length);
                    }

                    @Override
                    public void onEndOfSpeech() {
                        mgLog.d("onEndOfSpeech");
                    }

                    @Override
                    public void onError(int i) {
                        mgLog.d("onError "+i);
                    }

                    @Override
                    public void onEvent(int i, Bundle bundle) {
                        mgLog.d("onEvent");
                    }

                    @Override
                    public void onPartialResults(Bundle bundle) {
                        mgLog.d("onPartialResults "+bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION));
                    }

                    @Override
                    public void onReadyForSpeech(Bundle bundle) {
                        mgLog.d("onReadyForSpeech");
                    }

                    @Override
                    public void onResults(Bundle results) {
                        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        mgLog.d("onResults "+matches);
                        if (matches != null && !matches.isEmpty()) {
                            evaluateVoice(matches);
                        }
                    }

                    @Override
                    public void onRmsChanged(float v) {
//                        mgLog.d("onRmsChanged "+v);
                    }
                });
            }


            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, Objects.requireNonNull(getClass().getPackage()).getName());
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 10);
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-US");

            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            mgLog.e(e.getMessage(),e);
        }
    }

    private void initWordFixes(){
        wordFixes.put("dreck", "track");
        wordFixes.put("gute", "route");


        wordFixes.put("eins", "1");
        wordFixes.put("zwei", "2");
        wordFixes.put("drei", "3");
        wordFixes.put("vier", "4");
        wordFixes.put("fünf", "5");
        wordFixes.put("sechs", "6");
        wordFixes.put("sieben", "7");
        wordFixes.put("acht", "8");
        wordFixes.put("neun", "9");
        wordFixes.put("zehn", "10");
        wordFixes.put("elf", "11");
        wordFixes.put("zwölf", "12");
        wordFixes.put("dreizehn", "13");
        wordFixes.put("vierzehn", "14");
        wordFixes.put("fünfzehn", "15");
        wordFixes.put("sechszehn", "16");
        wordFixes.put("siebzehn", "17");
        wordFixes.put("achtzehn", "18");
        wordFixes.put("neunzehn", "19");
        wordFixes.put("zwanzig", "20");
        wordFixes.put("einundzwanzig", "21");
        wordFixes.put("zweiundzwanzig", "22");
        wordFixes.put("dreiundzwanzig", "23");
        wordFixes.put("vierundzwanzig", "24");
    }


    private void evaluateVoice(ArrayList<String> suggestions){
        suggestions.sort((o1, o2) -> {
            if (o1.length() != o2.length()) {
                return o2.length() - o1.length();
            } else {
                return o1.compareTo(o2);
            }
        });
        mgLog.d("suggestions: "+suggestions);

        boolean done = false;
        for (String suggestion : suggestions){
            String[] words = suggestion.toLowerCase().split("\\s");
            for (int i=0; i<words.length; i++){
                words[i] = wordFixes.getOrDefault(words[i], words[i]);
            }
            if (words.length == 0) continue;
            mgLog.d("voice command was checked: "+Arrays.toString(words));

            switch (Objects.requireNonNull(words[0])){
                case "zoom":
                    // accepted commands are:
                    //   zoom in [steps]
                    //   zoom out [steps]
                    //   zoom [level]
                    if (words.length == 1) continue; // just "zoom" is no command
                    if ("in".equals(words[1])){
                        if (words.length == 2){
                            done = zoom(1);
                        } else if (words.length == 3){
                            int steps = intFromWord(words[2]);
                            if (steps > 0){
                                done = zoom(steps);
                            }
                        }
                    } else if ("out".equals(words[1])){
                        if (words.length == 2){
                            done = zoom(-1);
                        } else if (words.length == 3){
                            int steps = intFromWord(words[2]);
                            if (steps > 0){
                                done = zoom(-steps);
                            }
                        }
                    } else {
                        int level = intFromWord(words[1]);
                        if (level > 0){
                            done = setZoom(level);
                        }
                    }
                    break;
                case "lauter":
                    // accepted commands are:
                    //   lauter max
                    //   lauter [steps]
                    if (words.length == 1){
                        done = volume(true, 1);
                    } else if (words.length == 2){
                        if ("max".equals(words[1])){
                            done = setVolume(100);
                        } else {
                            int steps = intFromWord(words[1]);
                            if (steps > 0){
                                done = volume(true, steps);
                            }
                        }
                    }
                    break;
                case "leiser":
                    // accepted commands are:
                    //   leiser max
                    //   leiser [steps]
                    if (words.length == 1){
                        done = volume(false, 1);
                    } else if (words.length == 2){
                        if ("max".equals(words[1])){
                            done = setVolume(0);
                        } else {
                            int steps = intFromWord(words[1]);
                            if (steps > 0){
                                done = volume(false, steps);
                            }
                        }
                    }
                    break;
                case "lautlos":
                    // accepted commands are:
                    //   lautlos
                    if (words.length == 1){
                        done = setVolume(0);
                    }
                    break;
                case "track":
                    // accepted commands are:
                    //   track recording (on|off)
                    //   track aufzeichnung (an|aus)
                    if (words.length == 3){
                        if (("recording".equals(words[1]) && "on".equals(words[2])) || ("aufzeichnung".equals(words[1]) && "an".equals(words[2]))){
                            done = trackRecord(true);
                        }
                        if (("recording".equals(words[1]) && "off".equals(words[2])) || ("aufzeichnung".equals(words[1]) && "aus".equals(words[2]))){
                            done = trackRecord(false);
                        }
                    }
                    break;
                case "route":
                    // accepted commands are:
                    //   route kilometer
                    //   route (aufwärts|höhenmeter)
                    //   route abwärts
                    //   route zeit
                    //   route ankunftszeit
                    //   route dauer
                    if (words.length == 2) {
                        ViewGroup dashboard = getDashboard("route");
                        if ("kilometer".equals(words[1])){
                            done = enlarge(dashboard,1);
                        } else if ("aufwärts".equals(words[1]) || "höhenmeter".equals(words[1])){
                            done = enlarge(dashboard,2);
                        } else if ("abwärts".equals(words[1])){
                            done = enlarge(dashboard,3);
                        } else if ("zeit".equals(words[1])){
                            done = enlarge(dashboard,4);
                        } else if ("ankunftszeit".equals(words[1])){
                            done = setDurationMode(false);
                        } else if ("dauer".equals(words[1])){
                            done = setDurationMode(true);
                        }
                    }
                    break;
                case "aufzeichnung":
                    // accepted commands are:
                    //   aufzeichnung kilometer
                    //   aufzeichnung (aufwärts|höhenmeter)
                    //   aufzeichnung abwärts
                    //   aufzeichnung zeit
                    if (words.length == 2) {
                        ViewGroup dashboard = getDashboard("rtl");
                        if ("kilometer".equals(words[1])){
                            done = enlarge(dashboard,1);
                        } else if ("aufwärts".equals(words[1]) || "höhenmeter".equals(words[1])){
                            done = enlarge(dashboard,2);
                        } else if ("abwärts".equals(words[1])){
                            done = enlarge(dashboard,3);
                        } else if ("zeit".equals(words[1])){
                            done = enlarge(dashboard,4);
                        }
                    }
                    break;
                case "uhrzeit":
                    if (words.length == 1) {
                        prefEnlargeTimeTrigger.toggle();
                        done = true;
                    }
                    break;
                case "battery":
                case "akku":
                    if (words.length == 1) {
                        prefEnlargeBatTrigger.toggle();
                        done = true;
                    }
                    break;
                case "suche":
                    prefSearchOn.setValue(true);
                    prefSearchText.setValue(suggestion.replaceFirst("[Ss]uche ",""));
                    prefSearchTrigger.setValue(true);
                    done = true;
                    break;
                case "richtung":
                    if (words.length >= 2){
                        done = switch (words[1]) {
                            case "ost" -> checkMove(words, 2, 1, 0);
                            case "süd" -> checkMove(words, 2, 0, 1);
                            case "west" -> checkMove(words, 2, -1, 0);
                            case "nord" -> checkMove(words, 2, 0, -1);
                            case "südost" -> checkMove(words, 2, 1, 1);
                            case "südwest" -> checkMove(words, 2, -1, 1);
                            case "nordost" -> checkMove(words, 2, 1, -1);
                            case "nordwest" -> checkMove(words, 2, -1, -1);
                            default -> false;
                        };
                    }

                    break;
                case "rechts":
                    done = checkMove(words,1,1,0);
                    break;
                case "links":
                    done = checkMove(words,1,-1,0);
                    break;
            }
            if (done) {
                mgLog.i("voice command was executed: "+Arrays.toString(words));
                break; // leave the for loop
            }
        }
    }

    private boolean checkMove(String[] words, int nextIdx, int eastSteps, int southSteps){
        if (words.length > nextIdx+1) return false;
        if (words.length > nextIdx){
            int cnt = intFromWord(words[nextIdx]);
            if (cnt <= 0) return false;
            eastSteps *= cnt;
            southSteps *= cnt;
        }
        move(eastSteps, southSteps);
        return true;
    }

    private int intFromWord(String word){
        try {
            return Integer.parseInt(word);
        } catch (NumberFormatException e) {
            return -1;
        }
    }



    /* ******************************************************************************************** */
    /* *************         Controls that are used to implement speech commands         ********** */
    /* ******************************************************************************************** */

    /** each step is a quarter of screen height/width  */
    private void move(int eastSteps, int southSteps){
        MapViewUtility mvu = getMapViewUtility();
        PointModel center = mvu.getCenter();
        Point p = mvu.getPoint4PointModel(center);
        int stepX = p.x/2;
        int stepY = p.y/2;
        Point p2 = new Point(p.x + stepX*eastSteps , p.y + stepY*southSteps);
        mvu.animateToCenter(mvu.getPointModel4Point(p2));
    }

    private boolean zoom(int delta){
        getMapView().getModel().mapViewPosition.zoom((byte)(delta));
        return true;
    }
    private boolean setZoom(int level){
        getMapView().getModel().mapViewPosition.setZoomLevel((byte)(level));
        return true;
    }

    private boolean volume(boolean raise, int delta){
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        for (int i=0; i<delta; i++){
            audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, raise?AudioManager.ADJUST_RAISE:AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
        }
        return true;
    }
    private boolean setVolume(int level){
        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, level, AudioManager.FLAG_SHOW_UI);
        return true;
    }

    private boolean trackRecord(boolean on){
        boolean trackRecordIsOn = (application.recordingTrackLogObservable.getTrackLog() != null);
        if ((!trackRecordIsOn && on) || (trackRecordIsOn && !on)){
            getPref(R.string.FSRecording_pref_toggleTrackRecord, false).toggle();
        }
        return true;
    }

    private boolean setDurationMode(boolean durationMode){
        prefRouteDuration.setValue(durationMode);
        return true;
    }
    private boolean enlarge(ViewGroup dashboard, int childAt){
        if (dashboard.getParent() != null){
            new EnlargeControl(tv_enlarge).onClick(dashboard.getChildAt(childAt));
        }
        return true;
    }




}
