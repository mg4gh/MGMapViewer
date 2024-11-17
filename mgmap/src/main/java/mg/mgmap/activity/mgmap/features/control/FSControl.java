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

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.transition.AutoTransition;
import android.transition.Transition;
import android.transition.TransitionListenerAdapter;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.filemgr.FileManagerActivity;
import mg.mgmap.activity.height_profile.HeightProfileActivity;
import mg.mgmap.activity.mgmap.MGMapActivity;
import mg.mgmap.activity.mgmap.FeatureService;
import mg.mgmap.R;
import mg.mgmap.activity.theme.ThemeSettings;
import mg.mgmap.activity.statistic.TrackStatisticActivity;
import mg.mgmap.activity.settings.DownloadPreferenceScreen;
import mg.mgmap.activity.settings.MainPreferenceScreen;
import mg.mgmap.activity.settings.SettingsActivity;
import mg.mgmap.application.BaseConfig;
import mg.mgmap.generic.util.FullscreenUtil;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.hints.AbstractHint;
import mg.mgmap.generic.util.hints.HintInitialMapDownload;
import mg.mgmap.generic.util.hints.HintVersion;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.generic.view.VUtil;

public class FSControl extends FeatureService {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());
    final static int hMargin  = VUtil.hMargin;
    final static int vMargin = VUtil.vMargin;
    private static final int ANIMATION_TIMEOUT_DEFAULT = 250;


    private final Pref<Integer> prefQcs = getPref(R.string.FSControl_qc_selector, 0);
    private final Pref<Boolean> prefFullscreen = getPref(R.string.FSControl_qcFullscreenOn, true);
    private final Pref<Boolean> prefMenuOneLine;
    private final Pref<String> prefMenuAnimationTimeout;
    private int totalWidth;
    private final Drawable qcBackground;
    private final Drawable qcLightBackground;

    private final Pref<Boolean> triggerSettings = new Pref<>(false);
    private final Pref<Boolean> triggerStatistic = new Pref<>(false);
    private final Pref<Boolean> triggerHeightProfile = new Pref<>(false);
    private final Pref<Boolean> triggerDownload = new Pref<>(false);
    private final Pref<Boolean> triggerHome = new Pref<>(false);
    private final Pref<Boolean> triggerExit = new Pref<>(false);
    private final Pref<Boolean> triggerZoomIn = new Pref<>(false);
    private final Pref<Boolean> triggerZoomOut = new Pref<>(false);
    private final Pref<Boolean> triggerThemes = new Pref<>(false);
    private final Pref<Boolean> triggerFileMgr = new Pref<>(false);
    private final Pref<Boolean> prefHelp = new Pref<>(false);

    private ViewGroup[] qcss = null; // quick controls groups (index 0 is menu control group and index 1..7 are seven sub action menus)


    final Observer homeObserver = (e) -> {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN, null);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        activity.startActivity(homeIntent);
    };
    final Observer settingsPrefObserver = (e) -> {
        MGMapActivity activity = getActivity();
        Intent intent = new Intent(activity, SettingsActivity.class);
        String prefScreenClass = MainPreferenceScreen.class.getName();
        if (e.getSource() == triggerDownload) prefScreenClass = DownloadPreferenceScreen.class.getName();
        intent.putExtra("FSControl.info", prefScreenClass);
        activity.startActivity(intent);
    };
    final Observer statisticObserver = (e) -> {
        MGMapActivity activity = getActivity();
        Intent intent = new Intent(activity, TrackStatisticActivity.class);
        activity.startActivity(intent);
    };
    final Observer heightProfileObserver = (e) -> {
        MGMapActivity activity = getActivity();
        Intent intent = new Intent(activity, HeightProfileActivity.class);
        activity.startActivity(intent);
    };
    final Observer exitObserver = (e) -> {
        getPref(R.string.FSPosition_pref_GpsOn, false).setValue(false);
        getApplication().startTrackLoggerService(getActivity(),false);
        getTimer().postDelayed(()->{
            getActivity().finishAndRemoveTask();
            if (getApplication().baseConfig.mode() == BaseConfig.Mode.NORMAL){
                System.exit(0);
            }
        },500);
    };
    final Observer themesObserver = (e) -> {
        MGMapActivity activity = getActivity();
        Intent intent = new Intent(activity, ThemeSettings.class);
        if (activity.getRenderThemeStyleMenu() != null) {
            intent.putExtra(activity.getResources().getString(R.string.my_rendertheme_menu_key), activity.getRenderThemeStyleMenu());
        }
        activity.startActivity(intent);
    };

    final AbstractHint hintInitialMapDownload;
    final AbstractHint hintVersion;

    public FSControl(MGMapActivity activity){
        super(activity);

        qcBackground = getDrawable(R.drawable.shape);
        qcLightBackground = getDrawable(R.drawable.shape_mu);

        ttRefreshTime = 10;
        prefFullscreen.addObserver((e) -> {
            FullscreenUtil.enforceState(getActivity(), prefFullscreen.getValue());
            getControlView().setVerticalOffset( );
        });
        prefMenuOneLine = getPref(R.string.FSControl_pref_menu_one_line_key, false);
        RelativeLayout base2 = activity.findViewById(R.id.base2);
        prefMenuOneLine.addObserver(evt -> base2.getLayoutParams().height = (prefMenuOneLine.getValue()?1:2) * VUtil.QC_HEIGHT );
        prefMenuOneLine.changed();
        prefMenuAnimationTimeout = getPref(R.string.FSControl_pref_menu_animation_timeout_key, ""+ANIMATION_TIMEOUT_DEFAULT);


        triggerHome.addObserver(homeObserver);
        prefQcs.addObserver(refreshObserver);
        triggerSettings.addObserver(settingsPrefObserver);
        triggerDownload.addObserver(settingsPrefObserver);
        triggerStatistic.addObserver(statisticObserver);
        triggerHeightProfile.addObserver(heightProfileObserver);
        triggerExit.addObserver(exitObserver);
        triggerZoomIn.addObserver((e) -> {
            getMapView().getModel().mapViewPosition.zoomIn();
            setupTTHideQCS();
        });
        triggerZoomOut.addObserver((e) -> {
            getMapView().getModel().mapViewPosition.zoomOut();
            setupTTHideQCS();
        });
        triggerThemes.addObserver(themesObserver);
        prefHelp.addObserver((ev) -> {
            int iVis = (prefHelp.getValue())?View.VISIBLE:View.INVISIBLE;
            if (prefHelp.getValue()){
                ViewGroup qcs = qcss[prefQcs.getValue()];
                for (int i=0; i<qcs.getChildCount(); i++){
                    if (qcs.getChildAt(i) instanceof ExtendedTextView) {
                        try {
                            ExtendedTextView etv = (ExtendedTextView) qcs.getChildAt(i);
                            @SuppressLint("DiscouragedApi") int id = activity.getResources().getIdentifier("help2_text"+i, "id", activity.getPackageName());
                            TextView tv = getActivity().findViewById(id);
                            tv.setText(etv.getHelp());
                        } catch (Exception e) {
                            mgLog.e(e);
                        }
                    }
                }
                cancelTTHideQCS();
            } else {
                setupTTHideQCS();
            }
            getActivity().findViewById(R.id.help).setVisibility(iVis);
            mgLog.d("change help Visibility to "+ prefHelp.getValue());
        });
        triggerFileMgr.addObserver((Observer) evt -> {
            Intent intent = new Intent(activity, FileManagerActivity.class);
            activity.startActivity(intent);
        });
        hintInitialMapDownload = new HintInitialMapDownload(getActivity());
        Runnable rSetVersion = () -> getPref(R.string.FSControl_pref_version_key, "").setValue(HintVersion.currentVersion());
        if (hintInitialMapDownload.checkHintCondition()){
            rSetVersion.run();
        }
        hintVersion = new HintVersion(getActivity()).addGotItAction(rSetVersion);
    }

    public void initQcss(ViewGroup[] qcss){
        this.qcss = qcss;
    }

    @Override
    public ExtendedTextView initQuickControl(ExtendedTextView etv, String info){
        super.initQuickControl(etv,info);
        if ("group_multi".equals(info)) {
            etv.setPrAction(new Pref<>(false), triggerHome);
            etv.setData(R.drawable.group_multi);
        } else if ("group_task".equals(info)) {
            etv.setPrAction(new Pref<>(false));
            etv.setData(R.drawable.group_task);
        } else if ("fullscreen".equals(info)) {
            etv.setPrAction(prefFullscreen);
            etv.setData(R.drawable.fullscreen);
            etv.setHelp(r(R.string.FSControl_qcFullscreen_help));
        } else if ("settings".equals(info)) {
            etv.setPrAction(triggerSettings);
            etv.setData(R.drawable.settings);
            etv.setHelp(r(R.string.FSControl_qcSettings_help));
        } else if ("home".equals(info)) {
            etv.setPrAction(triggerHome);
            etv.setData(R.drawable.home);
            etv.setHelp(r(R.string.FSControl_qcHome_help));
        } else if ("exit".equals(info)) {
            etv.setPrAction(triggerExit);
            etv.setData(R.drawable.exit);
            etv.setHelp(r(R.string.FSControl_qcExit_help));
        }else if ("download".equals(info)) {
            etv.setPrAction(triggerDownload);
            etv.setData(R.drawable.download);
            etv.setHelp(r(R.string.FSControl_qcDownload_help));
        } else if ("statistic".equals(info)) {
            etv.setPrAction(triggerStatistic);
            etv.setData(R.drawable.statistik);
            etv.setHelp(r(R.string.FSControl_qcStatistic_help));
        } else if ("heightProfile".equals(info)) {
            etv.setPrAction(triggerHeightProfile);
            etv.setData(R.drawable.height_profile);
            etv.setHelp(r(R.string.FSControl_qcHeightProfile_help));
        } else if ("empty".equals(info)) {
            etv.setPrAction(new Pref<>(false));
            etv.setData(R.drawable.empty);
        } else if ("zoom_in".equals(info)) {
            etv.setPrAction(triggerZoomIn);
            etv.setData(R.drawable.zoom_in);
            etv.setHelp(r(R.string.FSControl_qcZoomIn_help));
        } else if ("zoom_out".equals(info)) {
            etv.setPrAction(triggerZoomOut);
            etv.setData(R.drawable.zoom_out);
            etv.setHelp(r(R.string.FSControl_qcZoomOut_help));
        } else if ("themes".equals(info)) {
            etv.setPrAction(triggerThemes);
            etv.setData(R.drawable.themes);
            etv.setHelp(r(R.string.FSControl_qcThemes_help));
        } else if ("help".equals(info)) {
            etv.setPrAction(prefHelp);
            etv.setData(R.drawable.help);
            etv.setHelp(r(R.string.FSControl_qcHelp_help));
        } else if ("fileMgr".equals(info)) {
            etv.setPrAction(triggerFileMgr);
            etv.setData(R.drawable.file_mgr);
            etv.setHelp(r(R.string.FSControl_qcFileMgr_help));
        }
        return etv;
    }

    public TextView initHelpControl(TextView helpView, String info){
        if ("help1".equals(info)) {
            helpView.setOnClickListener(v -> {
                mgLog.d("onClick "+getResources().getResourceName(helpView.getId()).replaceFirst(".*/",""));
                prefHelp.setValue(false);
            });
        }
        return helpView;
    }

    @Override
    protected void onResume() {
        super.onResume();
        prefQcs.setValue(0);
        prefFullscreen.onChange();
        refreshObserver.onChange();

        // move menu items to deflated position
        for (int i=1; i<8; i++){
            new MenuDeflater(i).start();
        }
        getTimer().postDelayed(hintInitialMapDownload, 300);
        getTimer().postDelayed(hintVersion, 500);
        setEnableMenu(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getTimer().removeCallbacks(hintInitialMapDownload);
        getTimer().removeCallbacks(hintVersion);
        getTimer().removeCallbacks(ttHideSubQCS);
        getTimer().removeCallbacks(ttEnableMenuQCS);
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
        mgLog.d("enable="+enable);
        if (qcss[0]==null) return; // should never happen
        for (int cIdx=0; cIdx<qcss[0].getChildCount();cIdx++){
            qcss[0].getChildAt(cIdx).setEnabled(enable);
        }
    }

    private void setupTTHideQCS(){
        getTimer().removeCallbacks(ttHideSubQCS);
        getTimer().postDelayed(ttHideSubQCS, 3000);
        mgLog.v("Help Timer 3000 ");
    }
    private void cancelTTHideQCS(){
        mgLog.v("cancel Help Timer ");
        getTimer().removeCallbacks(ttHideSubQCS);
    }

    void setQCVisibility(){
        totalWidth = getActivity().getResources().getDisplayMetrics().widthPixels;
        for (int idx=0; idx<qcss.length; idx++){
            ViewGroup qcs = qcss[idx];
            if (prefQcs.getValue() == idx){ // should be visible
                if (qcs.getVisibility() == View.INVISIBLE){ // but is not yet visible
                    new MenuInflater(idx).start();
                }
            } else { // should not be visible
                if (qcs.getVisibility() == View.VISIBLE){ // but is visible
                    if (idx > 0){ // menu row is kept visible always
                        new MenuDeflater(idx).start();
                        prefHelp.setValue(false);
                        cancelTTHideQCS();
                    }
                }
            }
        }
        if ((prefQcs.getValue() > 0) && (!prefHelp.getValue())){
            setupTTHideQCS();
        }
        if (prefQcs.getValue() == 0){
            getTimer().postDelayed(ttEnableMenuQCS, 500);
        }
    }

    private int getMenuAnimationTimeout(){
        int res = ANIMATION_TIMEOUT_DEFAULT;
        try {
            res = Integer.parseInt(prefMenuAnimationTimeout.getValue());
        } catch (NumberFormatException e) {
            mgLog.e(e.getMessage());
            prefMenuAnimationTimeout.setValue(""+ANIMATION_TIMEOUT_DEFAULT);
        }
        res = Math.min(res, ANIMATION_TIMEOUT_DEFAULT*2); //limit to 2*prefMenuAnimationTimeout
        return res;
    }


    private class MenuInflater{
        private final int idx;
        private final ViewGroup qcs;
        public MenuInflater(int idx){
            this.idx = idx;
            qcs = qcss[idx];
        }
        public void start(){
            // first set menu item group visible
            qcs.setVisibility(View.VISIBLE);

            // animate menu item inflation
            AutoTransition at = new AutoTransition();
            at.setDuration(getMenuAnimationTimeout());
            TransitionManager.beginDelayedTransition(qcs,at);
            int max = qcs.getChildCount();
            for (int j=0; j<max; j++){
                View view = qcs.getChildAt(j);
                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(VUtil.getX4QC(totalWidth,1,max)-2*hMargin, VUtil.QC_HEIGHT-2*vMargin);
                rlp.setMargins(VUtil.getX4QC(totalWidth,j,max)+hMargin,vMargin,hMargin,vMargin);
                view.setLayoutParams(rlp);
            }
            if (idx > 0){ // show submenu
                for (int i=0; i<qcss[0].getChildCount(); i++){
                    if (prefMenuOneLine.getValue()) {
                        // hide menus, if just one line
                        qcss[0].getChildAt(i).setVisibility(View.INVISIBLE);
                    } else { // two Line
                        getMapViewUtility().setScaleBarVisibility(false);
                        if ((i + 1) != idx) {
                            // enlight (not pressed) menus
                            qcss[0].getChildAt(i).setBackground(qcLightBackground);
                            qcss[0].getChildAt(i).setEnabled(false);
                        }
                    }
                }
            }
        }
    }

    private class MenuDeflater{
        private final int idx;
        private final ViewGroup qcs;
        public MenuDeflater(int idx){
            this.idx = idx;
            qcs = qcss[idx];
        }
        public void start(){
            // animate menu item deflation
            AutoTransition at = new AutoTransition();
            at.setDuration(getMenuAnimationTimeout());
            at.addListener(new TransitionListenerAdapter() {
                @Override
                public void onTransitionEnd(Transition transition) {
                    mgLog.v("transition end");
                    qcs.setVisibility(View.INVISIBLE);
                    getMapViewUtility().setScaleBarVisibility(true);

                    for (int i=0; i<qcss[0].getChildCount(); i++){
                        // reset intensity and visibility of menus
                        qcss[0].getChildAt(i).setVisibility(View.VISIBLE);
                        qcss[0].getChildAt(i).setBackground(qcBackground);
                    }
                }
                @Override
                public void onTransitionCancel(Transition transition) {
                    mgLog.v("transition cancel");
                    this.onTransitionEnd(transition);
                }
            });
            TransitionManager.beginDelayedTransition(qcs,at);
            int max = qcs.getChildCount();
            for (int j=0; j<max; j++){
                View view = qcs.getChildAt(j);
                RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(VUtil.getX4QC(totalWidth,1,max)-2*hMargin, VUtil.QC_HEIGHT-2*vMargin);
                rlp.setMargins(VUtil.getX4QC(totalWidth,idx-1,max)+hMargin,(prefMenuOneLine.getValue()?0:VUtil.QC_HEIGHT) + hMargin,hMargin,vMargin);
                view.setLayoutParams(rlp);
            }
        }
    }

}
