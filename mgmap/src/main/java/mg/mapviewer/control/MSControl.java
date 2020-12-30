package mg.mapviewer.control;

import android.content.Intent;
import android.view.ViewGroup;

import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

import mg.mapviewer.HeightProfileActivity;
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
import mg.mapviewer.view.ExtendedTextView;
import mg.mapviewer.view.PrefTextView;

public class MSControl extends MGMicroService {

    MGPref<Integer> prefQcs = MGPref.get(R.string.MSControl_qc_selector, 0);
    private final MGPref<Boolean> prefFullscreen = MGPref.get(R.string.MSFullscreen_qc_On, true);

    private final MGPref<Boolean> prefSettings = MGPref.anonymous(false);
    private final MGPref<Boolean> prefFuSettings = MGPref.anonymous(false);
    private final MGPref<Boolean> prefStatistic = MGPref.anonymous(false);
    private final MGPref<Boolean> prefHeightProfile = MGPref.anonymous(false);
    private final MGPref<Boolean> prefDownload = MGPref.anonymous(false);
    private final MGPref<Boolean> prefHome = MGPref.anonymous(false);
    private final MGPref<Boolean> prefExit = MGPref.anonymous(false);
    private final MGPref<Boolean> prefZoomIn = MGPref.anonymous(false);
    private final MGPref<Boolean> prefZoomOut = MGPref.anonymous(false);

    ViewGroup qcsParent = null;
    ViewGroup[] qcss = null;

    FullscreenObserver fullscreenObserver = new FullscreenObserver(getActivity());
    HomeObserver homeObserver = new HomeObserver(getActivity());

    Observer settingsPrefObserver = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            MGMapActivity activity = getActivity();
            Intent intent = new Intent(activity, SettingsActivity.class);
            String prefScreenClass = MainPreferenceScreen.class.getName();
            if (o == prefFuSettings) prefScreenClass = FurtherPreferenceScreen.class.getName();
            if (o == prefDownload) prefScreenClass = DownloadPreferenceScreen.class.getName();
            intent.putExtra("MSControl.info", prefScreenClass);
            activity.startActivity(intent);
        }
    };
    Observer statisticObserver = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            MGMapActivity activity = getActivity();
            Intent intent = new Intent(activity, TrackStatisticActivity.class);
            activity.startActivity(intent);
        }
    };
    Observer heightProfileObserver = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            MGMapActivity activity = getActivity();
            Intent intent = new Intent(activity, HeightProfileActivity.class);
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

        prefFullscreen.addObserver(fullscreenObserver);
        prefHome.addObserver(homeObserver);
        prefQcs.addObserver(refreshObserver);
        prefSettings.addObserver(settingsPrefObserver);
        prefFuSettings.addObserver(settingsPrefObserver);
        prefDownload.addObserver(settingsPrefObserver);
        prefStatistic.addObserver(statisticObserver);
        prefHeightProfile.addObserver(heightProfileObserver);
        prefExit.addObserver(exitObserver);
        prefZoomIn.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                getMapView().getModel().mapViewPosition.zoomIn();
                setupTTHideQCS();
            }
        });
        prefZoomOut.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                getMapView().getModel().mapViewPosition.zoomOut();
                setupTTHideQCS();
            }
        });
    }

    public void initQcss(ViewGroup[] qcss){
        this.qcss = qcss;
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info){
        if ("group_multi".equals(info)) {
            etv.setPrAction(MGPref.anonymous(false),prefHome);
            etv.setData(R.drawable.multi);
        } else if ("group_task".equals(info)) {
            etv.setPrAction(MGPref.anonymous(false));
            etv.setData(R.drawable.group_task);
        } else if ("fullscreen".equals(info)) {
            etv.setPrAction(prefFullscreen);
            etv.setData(R.drawable.fullscreen);
        } else if ("settings".equals(info)) {
            etv.setPrAction(prefSettings);
            etv.setData(R.drawable.settings);
        } else if ("fuSettings".equals(info)) {
            etv.setPrAction(prefFuSettings);
            etv.setData(R.drawable.settings_fu);
        } else if ("home".equals(info)) {
            etv.setPrAction(prefHome);
            etv.setData(R.drawable.home);
        } else if ("exit".equals(info)) {
            etv.setPrAction(prefExit);
            etv.setData(R.drawable.exit);
        }else if ("download".equals(info)) {
            etv.setPrAction(prefDownload);
            etv.setData(R.drawable.download);
        } else if ("statistic".equals(info)) {
            etv.setPrAction(prefStatistic);
            etv.setData(R.drawable.statistik);
        } else if ("heightProfile".equals(info)) {
            etv.setPrAction(prefHeightProfile);
            etv.setData(R.drawable.height_profile);
        } else if ("empty".equals(info)) {
            etv.setPrAction(MGPref.anonymous(false));
            etv.setData(R.drawable.empty);
        } else if ("zoom_in".equals(info)) {
            etv.setPrAction(prefZoomIn);
            etv.setData(R.drawable.zoom_in);
        } else if ("zoom_out".equals(info)) {
            etv.setPrAction(prefZoomOut);
            etv.setData(R.drawable.zoom_out);
        }


        return etv;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (qcsParent == null){
            qcsParent = getActivity().findViewById(R.id.base);
        }
//        qcsParent = (qcsParent==null)?getActivity().findViewById(R.id.base):qcsParent;
//        qcs = (qcs==null)?getActivity().findViewById(R.id.tr_qc):qcs;
//        qcs2 = (qcs2==null)?getActivity().findViewById(R.id.tr_qc2):qcs2;


        prefQcs.setValue(0);
        prefFullscreen.onChange();
//        prefQC2.setValue(false);
        refreshObserver.onChange();
    }

    @Override
    protected void onPause() {
        super.onPause();

//        prefFullscreen.deleteObserver(fullscreenObserver);
//        prefQC2Home.deleteObserver(homeObserver);
//        prefQcs.deleteObserver(refreshObserver);
//        prefQC2Settings.deleteObserver(settingsPrefObserver);
//        prefQC2FuSettings.deleteObserver(settingsPrefObserver);
//        prefQC2Download.deleteObserver(settingsPrefObserver);
//        prefQC2Statistic.deleteObserver(statisticObserver);
//        prefQC2Exit.deleteObserver(exitObserver);
    }

    @Override
    protected void doRefresh() {
        setQCVisibility();
    }

    private Runnable ttHideQCS = new Runnable() {
        @Override
        public void run() {
//            prefQC2.setValue(false);
            prefQcs.setValue(0);
        }
    };

    private void setupTTHideQCS(){
        getTimer().removeCallbacks(ttHideQCS);
        getTimer().postDelayed(ttHideQCS, 3000);
    }
    private void cancelTTHideQCS(){
        getTimer().removeCallbacks(ttHideQCS);
    }

    void setQCVisibility(){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                cancelTTHideQCS();
                for (int idx=0; idx<8; idx++){
                    ViewGroup qcs = qcss[idx];
                    if (prefQcs.getValue() == idx){ // should be visible
                        if (qcs.getParent() == null){ // but is not yet visible

                            qcsParent.addView(qcs);
                        }
                    } else { // should not be visible
                        if (qcs.getParent() != null){ // ... but is visible
                            qcsParent.removeView(qcs);
                        }

                    }
                }
                if (prefQcs.getValue() > 0){
                    setupTTHideQCS();
                } else {
                    cancelTTHideQCS();
                }

            }
        });
    }

}
