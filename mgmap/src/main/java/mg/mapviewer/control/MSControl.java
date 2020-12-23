package mg.mapviewer.control;

import android.content.Intent;
import android.view.ViewGroup;

import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMicroService;
import mg.mapviewer.R;
import mg.mapviewer.features.statistic.TrackStatisticActivity;
import mg.mapviewer.settings.DownloadPreferenceScreen;
import mg.mapviewer.settings.FurtherPreferenceScreen;
import mg.mapviewer.settings.MainPreferenceScreen;
import mg.mapviewer.settings.SettingsActivity;
import mg.mapviewer.util.FullscreenObserver;
import mg.mapviewer.util.HomeObserver;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.view.PrefTextView;

public class MSControl extends MGMicroService {

    private final MGPref<Boolean> prefFullscreen = MGPref.get(R.string.MSFullscreen_qc_On, true);
    private final MGPref<Boolean> prefQC2 = MGPref.get(R.string.MSControl_qc2_on, false);

    private final MGPref<Boolean> prefQC2Settings   = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);
    private final MGPref<Boolean> prefQC2FuSettings = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);
    private final MGPref<Boolean> prefQC2Statistic  = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);
    private final MGPref<Boolean> prefQC2Download   = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);
    public final MGPref<Boolean> prefQC2Home       = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);
    private final MGPref<Boolean> prefQC2Exit       = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);

    ViewGroup qcsParent = null;
    ViewGroup qcs = null;
    ViewGroup qcs2 = null;

    FullscreenObserver fullscreenObserver = new FullscreenObserver(getActivity());
    HomeObserver homeObserver = new HomeObserver(getActivity());

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
    Observer statisticObserver = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            prefQC2.toggle();
            MGMapActivity activity = getActivity();
            Intent intent = new Intent(activity, TrackStatisticActivity.class);
            activity.startActivity(intent);
        }
    };
    Observer exitObserver = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            getActivity().finishAndRemoveTask();
            System.exit(0);
        }
    };

    public MSControl(MGMapActivity activity){
        super(activity);
    }

    @Override
    public PrefTextView initQuickControl(PrefTextView ptv, String info){
        if ("fullscreen+".equals(info)) {
            ptv.setPrefData(new MGPref[]{prefFullscreen, prefQC2Home, prefQC2},
                    new int[]{R.drawable.fullscreen});
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
        }


        return ptv;
    }

    @Override
    protected void start() {
        super.start();

        qcsParent = (qcsParent==null)?getActivity().findViewById(R.id.base):qcsParent;
        qcs = (qcs==null)?getActivity().findViewById(R.id.tr_qc):qcs;
        qcs2 = (qcs2==null)?getActivity().findViewById(R.id.tr_qc2):qcs2;

        prefFullscreen.addObserver(fullscreenObserver);
        prefQC2Home.addObserver(homeObserver);
        prefQC2.addObserver(refreshObserver);
        prefQC2Settings.addObserver(settingsPrefObserver);
        prefQC2FuSettings.addObserver(settingsPrefObserver);
        prefQC2Download.addObserver(settingsPrefObserver);
        prefQC2Statistic.addObserver(statisticObserver);
        prefQC2Exit.addObserver(exitObserver);

        prefFullscreen.onChange();
        prefQC2.setValue(false);
        refreshObserver.onChange();
    }

    @Override
    protected void stop() {
        super.stop();

        prefFullscreen.deleteObserver(fullscreenObserver);
        prefQC2Home.deleteObserver(homeObserver);
        prefQC2.deleteObserver(refreshObserver);
        prefQC2Settings.deleteObserver(settingsPrefObserver);
        prefQC2FuSettings.deleteObserver(settingsPrefObserver);
        prefQC2Download.deleteObserver(settingsPrefObserver);
        prefQC2Statistic.deleteObserver(statisticObserver);
        prefQC2Exit.deleteObserver(exitObserver);
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

}
