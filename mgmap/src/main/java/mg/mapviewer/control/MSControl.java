package mg.mapviewer.control;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.util.Log;
import android.view.ViewGroup;

import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.features.statistic.TrackStatisticActivity;
import mg.mapviewer.settings.DownloadPreferenceScreen;
import mg.mapviewer.settings.FurtherPreferenceScreen;
import mg.mapviewer.settings.MainPreferenceScreen;
import mg.mapviewer.settings.SettingsActivity;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.pref.MGPref;
import mg.mapviewer.view.PrefTextView;

public class MSControl extends MGMicroService {

    private final MGPref<Boolean> prefQC2 = MGPref.get(R.string.MSControl_qc2_on, false);

    private final MGPref<Boolean> prefQC2Settings   = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);
    private final MGPref<Boolean> prefQC2FuSettings = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);
    private final MGPref<Boolean> prefQC2Statistic  = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);
    private final MGPref<Boolean> prefQC2Download   = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);
    private final MGPref<Boolean> prefQC2Home       = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);
    private final MGPref<Boolean> prefQC2Exit       = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);

    ViewGroup qcsParent = null;
    ViewGroup qcs = null;
    ViewGroup qcs2 = null;

    public MSControl(MGMapActivity activity){
        super(activity);
    }

    @Override
    public PrefTextView initQuickControl(PrefTextView ptv, String info){
        if ("qc2".equals(info)) {
            ptv.appendPrefData(new MGPref[]{prefQC2},
                    new int[]{});
        } else if ("settings".equals(info)) {
            ptv.setPrefData(new MGPref[]{prefQC2Settings},
                    new int[]{R.drawable.settings});
        } else if ("fuSettings".equals(info)) {
            ptv.setPrefData(new MGPref[]{prefQC2FuSettings},
                    new int[]{R.drawable.settings_fu});
        } else if ("home".equals(info)) {
            ptv.setPrefData(new MGPref[]{prefQC2Home},
                    new int[]{R.drawable.home});
        } else if ("exit".equals(info)) {
            ptv.setPrefData(new MGPref[]{prefQC2Exit},
                    new int[]{R.drawable.exit});
        }else if ("download".equals(info)) {
            ptv.setPrefData(new MGPref[]{prefQC2Download},
                    new int[]{R.drawable.download});
        } else if ("statistic".equals(info)) {
            ptv.setPrefData(new MGPref[]{prefQC2Statistic},
                    new int[]{R.drawable.statistik});
        } else if ("home2".equals(info)) {
            ptv.appendPrefData(new MGPref[]{prefQC2Home},
                    new int[]{});
        }


        return ptv;
    }

    @Override
    protected void start() {
        super.start();

        qcsParent = (qcsParent==null)?getActivity().findViewById(R.id.base):qcsParent;
        qcs = (qcs==null)?getActivity().findViewById(R.id.tr_qc):qcs;
        qcs2 = (qcs2==null)?getActivity().findViewById(R.id.tr_qc2):qcs2;

        prefQC2.addObserver(refreshObserver);
        prefQC2.setValue(false);
        refreshObserver.onChange();

        Observer settingsPrefObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                prefQC2.toggle();
                MGMapActivity activity = getActivity();
                Intent intent = new Intent(activity, SettingsActivity.class);
                String prefScreenClass = MainPreferenceScreen.class.getName();
                if (o == prefQC2FuSettings) prefScreenClass = FurtherPreferenceScreen.class.getName();
                if (o == prefQC2Download) prefScreenClass = DownloadPreferenceScreen.class.getName();
                intent.putExtra("MSControl.info", prefScreenClass);
                activity.startActivity(intent);
            }
        };
        prefQC2Settings.addObserver(settingsPrefObserver);
        prefQC2FuSettings.addObserver(settingsPrefObserver);
        prefQC2Download.addObserver(settingsPrefObserver);

        prefQC2Statistic.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                prefQC2.toggle();
                MGMapActivity activity = getActivity();
                Intent intent = new Intent(activity, TrackStatisticActivity.class);
                activity.startActivity(intent);
            }
        });

        prefQC2Home.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                launchHomeScreen(getActivity());
            }
        });

        prefQC2Exit.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                getActivity().finishAndRemoveTask();
            }
        });

    }

    @Override
    protected void stop() {
        super.stop();
        prefQC2.deleteObservers();
        prefQC2Settings.deleteObservers();
        prefQC2FuSettings.deleteObservers();
        prefQC2Download.deleteObservers();
        prefQC2Statistic.deleteObservers();
        prefQC2Home.deleteObservers();
        prefQC2Exit.deleteObservers();
    }

    @Override
    protected void doRefresh() {
        setQCVisibility();
    }

    private Runnable ttHideQC2 = new Runnable() {
        @Override
        public void run() {
            prefQC2.setValue(false);
        }
    };

    private void setupTTHideQC2(){
        getTimer().removeCallbacks(ttHideQC2);
        getTimer().postDelayed(ttHideQC2, 3000);
    }
    private void cancelTTHideQC2(){
        getTimer().removeCallbacks(ttHideQC2);


    }

    void setQCVisibility(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cancelTTHideQC2();
                if (prefQC2.getValue()){
                    if (qcs.getParent() != null){
                        qcsParent.removeView(qcs);
                    }
                    if (qcs2.getParent() == null){
                        qcsParent.addView(qcs2);
                        setupTTHideQC2();
                    }
                } else {
                    if (qcs.getParent() == null){
                        qcsParent.addView(qcs);
                    }
                    if (qcs2.getParent() != null){
                        cancelTTHideQC2();
                        qcsParent.removeView(qcs2);
                    }
                }
            }
        });
    }

    public void launchHomeScreen(Activity activity) {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN, null);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        activity.startActivity(homeIntent);
    }
}
