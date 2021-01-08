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
package mg.mapviewer.features.control;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Observer;

import mg.mapviewer.HeightProfileActivity;
import mg.mapviewer.MGMapActivity;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.FeatureService;
import mg.mapviewer.R;
import mg.mapviewer.ThemeSettings;
import mg.mapviewer.features.statistic.TrackStatisticActivity;
import mg.mapviewer.settings.DownloadPreferenceScreen;
import mg.mapviewer.settings.FurtherPreferenceScreen;
import mg.mapviewer.settings.MainPreferenceScreen;
import mg.mapviewer.settings.SettingsActivity;
import mg.mapviewer.util.FullscreenObserver;
import mg.mapviewer.util.HomeObserver;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.view.ExtendedTextView;

public class FSControl extends FeatureService {

    MGPref<Integer> prefQcs = MGPref.get(R.string.FSControl_qc_selector, 0);
    private final MGPref<Boolean> prefFullscreen = MGPref.get(R.string.FSFullscreen_qc_On, true);

    private final MGPref<Boolean> prefSettings = MGPref.anonymous(false);
    private final MGPref<Boolean> prefFuSettings = MGPref.anonymous(false);
    private final MGPref<Boolean> prefStatistic = MGPref.anonymous(false);
    private final MGPref<Boolean> prefHeightProfile = MGPref.anonymous(false);
    private final MGPref<Boolean> prefDownload = MGPref.anonymous(false);
    private final MGPref<Boolean> prefHome = MGPref.anonymous(false);
    private final MGPref<Boolean> prefExit = MGPref.anonymous(false);
    private final MGPref<Boolean> prefZoomIn = MGPref.anonymous(false);
    private final MGPref<Boolean> prefZoomOut = MGPref.anonymous(false);
    private final MGPref<Boolean> prefThemes = MGPref.anonymous(false);
    private final MGPref<Boolean> prefHelp = MGPref.anonymous(false);

    ViewGroup qcsParent = null; // quick controls parent
    ViewGroup[] qcss = null; // quick controls groups (index 0 is menu control group and index 1..7 are seven sub action menus)
    ArrayList<TextView> helpTexts = new ArrayList<>(); // TextView Instances, which are used to show the help Info

    FullscreenObserver fullscreenObserver = new FullscreenObserver(getActivity());
    HomeObserver homeObserver = new HomeObserver(getActivity());

    Observer settingsPrefObserver = (o, arg) -> {
        MGMapActivity activity = getActivity();
        Intent intent = new Intent(activity, SettingsActivity.class);
        String prefScreenClass = MainPreferenceScreen.class.getName();
        if (o == prefFuSettings) prefScreenClass = FurtherPreferenceScreen.class.getName();
        if (o == prefDownload) prefScreenClass = DownloadPreferenceScreen.class.getName();
        intent.putExtra("FSControl.info", prefScreenClass);
        activity.startActivity(intent);
    };
    Observer statisticObserver = (o, arg) -> {
        MGMapActivity activity = getActivity();
        Intent intent = new Intent(activity, TrackStatisticActivity.class);
        activity.startActivity(intent);
    };
    Observer heightProfileObserver = (o, arg) -> {
        MGMapActivity activity = getActivity();
        Intent intent = new Intent(activity, HeightProfileActivity.class);
        activity.startActivity(intent);
    };
    Observer exitObserver = (o, arg) -> {
        getActivity().finishAndRemoveTask();
        System.exit(0);
    };
    Observer themesObserver = (o, arg) -> {
        MGMapActivity activity = getActivity();
        Intent intent = new Intent(activity, ThemeSettings.class);
        if (activity.getRenderThemeStyleMenu() != null) {
            intent.putExtra(activity.getResources().getString(R.string.my_rendertheme_menu_key), activity.getRenderThemeStyleMenu());
        }
        activity.startActivity(intent);
    };

    public FSControl(MGMapActivity activity){
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
        prefZoomIn.addObserver((o, arg) -> {
            getMapView().getModel().mapViewPosition.zoomIn();
            setupTTHideQCS();
        });
        prefZoomOut.addObserver((o, arg) -> {
            getMapView().getModel().mapViewPosition.zoomOut();
            setupTTHideQCS();
        });
        prefThemes.addObserver(themesObserver);
        prefHelp.addObserver((o, arg) -> {
            int iVis = (prefHelp.getValue())?View.VISIBLE:View.INVISIBLE;
            if (prefHelp.getValue()){
                ViewGroup qcs = qcss[prefQcs.getValue()];
                for (int i=0; i<qcs.getChildCount(); i++){
                    if (qcs.getChildAt(i) instanceof ExtendedTextView) {
                        try {
                            ExtendedTextView etv = (ExtendedTextView) qcs.getChildAt(i);
                            TextView tv = helpTexts.get(i);
                            tv.setText(etv.getHelp());
                        } catch (Exception e) {
                            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
                        }
                    }
                }
                cancelTTHideQCS();
            } else {
                setupTTHideQCS();
            }
            getActivity().findViewById(R.id.help).setVisibility(iVis);
            Log.d(MGMapApplication.LABEL, NameUtil.context()+" change Visibility to "+ prefHelp.getValue());
        });
    }

    public void initQcss(ViewGroup[] qcss){
        this.qcss = qcss;
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info){
        super.initQuickControl(etv,info);
        if ("group_multi".equals(info)) {
            etv.setPrAction(MGPref.anonymous(false),prefHome);
            etv.setData(R.drawable.multi);
        } else if ("group_task".equals(info)) {
            etv.setPrAction(MGPref.anonymous(false));
            etv.setData(R.drawable.group_task);
        } else if ("fullscreen".equals(info)) {
            etv.setPrAction(prefFullscreen);
            etv.setData(R.drawable.fullscreen);
            etv.setHelp(r(R.string.FSControl_qcFullscreen_help));
        } else if ("settings".equals(info)) {
            etv.setPrAction(prefSettings);
            etv.setData(R.drawable.settings);
            etv.setHelp(r(R.string.FSControl_qcSettings_help));
        } else if ("fuSettings".equals(info)) {
            etv.setPrAction(prefFuSettings);
            etv.setData(R.drawable.settings_fu);
            etv.setHelp(r(R.string.FSControl_qcFuSettings_help));
        } else if ("home".equals(info)) {
            etv.setPrAction(prefHome);
            etv.setData(R.drawable.home);
            etv.setHelp(r(R.string.FSControl_qcHome_help));
        } else if ("exit".equals(info)) {
            etv.setPrAction(prefExit);
            etv.setData(R.drawable.exit);
            etv.setHelp(r(R.string.FSControl_qcExit_help));
        }else if ("download".equals(info)) {
            etv.setPrAction(prefDownload);
            etv.setData(R.drawable.download);
            etv.setHelp(r(R.string.FSControl_qcDownload_help));
        } else if ("statistic".equals(info)) {
            etv.setPrAction(prefStatistic);
            etv.setData(R.drawable.statistik);
            etv.setHelp(r(R.string.FSControl_qcStatistic_help));
        } else if ("heightProfile".equals(info)) {
            etv.setPrAction(prefHeightProfile);
            etv.setData(R.drawable.height_profile);
            etv.setHelp(r(R.string.FSControl_qcHeightProfile_help));
        } else if ("empty".equals(info)) {
            etv.setPrAction(MGPref.anonymous(false));
            etv.setData(R.drawable.empty);
        } else if ("zoom_in".equals(info)) {
            etv.setPrAction(prefZoomIn);
            etv.setData(R.drawable.zoom_in);
            etv.setHelp(r(R.string.FSControl_qcZoomIn_help));
        } else if ("zoom_out".equals(info)) {
            etv.setPrAction(prefZoomOut);
            etv.setData(R.drawable.zoom_out);
            etv.setHelp(r(R.string.FSControl_qcZoomOut_help));
        } else if ("themes".equals(info)) {
            etv.setPrAction(prefThemes);
            etv.setData(R.drawable.themes);
            etv.setHelp(r(R.string.FSControl_qcThemes_help));
        } else if ("help".equals(info)) {
            etv.setPrAction(prefHelp);
            etv.setData(R.drawable.help);
            etv.setHelp(r(R.string.FSControl_qcHelp_help));
        }
        return etv;
    }

    public TextView initHelpControl(TextView helpView, String info){
        if ("help1".equals(info)) {
            helpView.setOnClickListener(v -> {
                prefHelp.setValue(false);
                setupTTHideQCS();
            });
        } else if ("help2".equals(info)) {
            helpTexts.add(helpView);
        }
        return helpView;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (qcsParent == null){
            qcsParent = getActivity().findViewById(R.id.base);
        }
        prefQcs.setValue(0);
        prefFullscreen.onChange();
        refreshObserver.onChange();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void doRefreshResumedUI() {
        setQCVisibility();
    }

    private final Runnable ttEnableMenuQCS = () -> setEnableMenu(true);
    private final Runnable ttHideSubQCS = () -> {
        prefQcs.setValue(0);
        // if we go back to the main menu, then disable menu buttons for 500ms (the click might be for the disappeared submenu, su don't execute it for another context)
        getTimer().postDelayed(ttEnableMenuQCS, 500);
        setEnableMenu(false);
    };

    private void setEnableMenu(boolean enable){
        Log.d(MGMapApplication.LABEL, NameUtil.context()+" enable="+enable);
        if (qcss[0]==null) return; // should never happen
        for (int cIdx=0; cIdx<qcss[0].getChildCount();cIdx++){
            qcss[0].getChildAt(cIdx).setEnabled(enable);
        }
    }

    private void setupTTHideQCS(){
        getTimer().removeCallbacks(ttHideSubQCS);
        getTimer().postDelayed(ttHideSubQCS, 3000);
        Log.v(MGMapApplication.LABEL, NameUtil.context()+" Help Timer 3000 ");
    }
    private void cancelTTHideQCS(){
        Log.v(MGMapApplication.LABEL, NameUtil.context()+" cancel Help Timer ");
        getTimer().removeCallbacks(ttHideSubQCS);
    }

    void setQCVisibility(){
//        cancelTTHideQCS();
        prefHelp.setValue(false);
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
        if ((prefQcs.getValue() > 0) && (!prefHelp.getValue())){
            setupTTHideQCS();
        } else {
            cancelTTHideQCS();
        }
    }

}
