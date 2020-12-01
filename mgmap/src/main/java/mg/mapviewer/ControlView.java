/*
 * Copyright 2020 mg4gh
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
package mg.mapviewer;

import android.content.Context;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.TileLayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import mg.mapviewer.model.TrackLogStatistic;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.HintControl;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.Control;
import mg.mapviewer.view.PrefTextView;

/**
 * The control view is the parent container view object. So it is the parent for
 * <ul>
 *     <li>the dashboard</li>
 *     <li>all menu und submenu buttons</li>
 *     <li>the transparency control sliders</li>
 *     <li>the progress bar</li>
 *     <li>the status line</li>
 *     <li>the quick controls</li>
 * </ul>
 * @see <a href="../../../images/MGMapViewer_ViewModel.PNG">MGMapActivity_ViewModel</a>
 *
 */
public class ControlView extends RelativeLayout {

    Context context;
    MGMapApplication application = null;
    MGMapActivity activity = null;

//    /** There are five dashboard entries, each with a specific semantic. This ordered list contains the viewIds of these five entries. */
//    ArrayList<Integer> dashboardKeys = new ArrayList<>();
//    /** Mapping of the viewIDs to the Dashboard entries. The Dashboard entry objects are kept here even if they are not visible. */
//    Map<Integer,ViewGroup> dashboardMap = new HashMap<>();

    ArrayList<ViewGroup> dashboardEntries = new ArrayList<>();
    /** Parent of the dashboard entries. */
    ViewGroup dashboard;
    /** Reference to the MapView object - some controls change properties of the mapView */
    MapView mapView = null;

    /** progress bar - only used in case of route opimization */
    ProgressBar progressBar = null;
    /** indicates visibility of menu */
    private boolean menuVisibility = false;



    /** parent object for status line */
    TableRow tr_states = null;
    /** List will be filled with all members of the status line */
    ArrayList<TextView> tvList = new ArrayList<>();

    TextView tv_hint = null;
    HintControl hintControl = null;

    /** A Control object is an extension of a ViewOnClickListener. With this map it's easy to determine from the OnClickListener the corresponding View object. Furthermore this map provides a list of all menu and submenu views. */
    Map<Control, View> menuControlMap = new HashMap<>();

    public ControlView(Context context) {
        super(context);
        this.context = context;
    }

    public ControlView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.context = context;
    }

    public MGMapActivity getActivity(){
        return activity;
    }
    public MGMapApplication getApplication(){
        return application;
    }

    public String rstring(int id){
        return getResources().getString(id);
    }

    private int convertDp(float dp){
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }



    public void init(final MGMapApplication application, final MGMapActivity activity){
        try {
            this.application = application;
            this.activity = activity;
            ControlComposer controlComposer = new ControlComposer();

            tv_hint = findViewById(R.id.hint);
            tv_hint.setVisibility(INVISIBLE);
            hintControl = new HintControl(tv_hint);

            mapView = activity.getMapsforgeMapView();
            // this is necessary to get the scalbar above the quick controls and the status line
            mapView.getMapScaleBar().setMarginVertical((int)(60 * mapView.getModel().displayModel.getScaleFactor()));

            // initialize the dashboardKeys and dashboardMap object and then hide dashboard entries
            dashboard = findViewById(R.id.dashboard);

            controlComposer.composeDashboard(application, activity, this);

            controlComposer.composeMenu(application, activity, this);
            hideMenu();

            prepareAlphaSliders();

            prepareStatusLine();
            controlComposer.composeStatusLine(application, activity, this);
            finalizeStatusLine();

            controlComposer.composeQuickControls(application, activity, this);

        } catch (Exception e){
            Log.e(MGMapApplication.LABEL, NameUtil.context()+"", e);
        }
    }

// *************************************************************************************************
// ********* Menu Button related stuff                                                    **********
// *************************************************************************************************

    void registerMenuControl(View view, Control control){
        control.setControlView(this);
        menuControlMap.put(control, view);
        view.setOnClickListener(control);
    }

    void registerSubmenuControls(ViewGroup menu, Control[] controls){
        if (menu.getChildCount() < controls.length){
            Log.e(MGMapApplication.LABEL, NameUtil.context() + " too much controls("+controls.length+") for menu("+menu.getChildCount()+")");
        }
        for (int idx=menu.getChildCount()-1; idx >= 0; idx--){
            if (idx < controls.length){
                registerMenuControl(menu.getChildAt(idx), controls[idx]);
            } else {
                menu.removeViewAt(idx);
            }
        }
    }

    void registerMenuControls(View view, final String name, final ViewGroup menu, Control[] controls){
        registerSubmenuControls(menu, controls);
        Control control = new Control(false){
            @Override
            public void onClick(View v) {
                super.onClick(v);
                disableMainMenuButtons();
                showMenu(menu);
            }

            @Override
            public void onPrepare(View v) {
                setText(v, name);
            }
        };
        registerMenuControl(view, control);
    }

    void disableMainMenuButtons(){
        for (int idx=0; idx<getChildCount(); idx++) {
            View chview = getChildAt(idx);
            if (chview instanceof Button) {
                chview.setEnabled(false); // disable main menu entries by default
            }
        }
    }

    public void showMenu(ViewGroup parent){
        for (Control control : menuControlMap.keySet()){
            View view = menuControlMap.get(control);
            if (view.getParent() == parent){
                view.setEnabled( true ); // default is enabled, might be reset in onPrepare
                control.onPrepare(view);
                view.setVisibility( VISIBLE );
            }
        }
    }

    public void hideMenu(){
        menuVisibility = false;
        for (Control control : menuControlMap.keySet()){
            View view = menuControlMap.get(control);

            if (view.getVisibility() == VISIBLE){
                view.setVisibility( INVISIBLE );
            }
        }
    }


    public void toggleMenuVisibility(){
        menuVisibility = !menuVisibility;
        if (menuVisibility) {
            showMenu(this);
            startTTHideButtons(7000);
        } else {
            hideMenu();
        }
    }


    private Handler timer = new Handler();
    final Runnable ttHideButtons = new Runnable() { // define timerTask to hide menu buttons
        @Override
        public void run() {
            hideMenu();
            timer.removeCallbacks(ttHideButtons);
        }
    };
    public void cancelTTHideButtons(){
        timer.removeCallbacks(ttHideButtons);
    }
    public void startTTHideButtons(long millis){
        timer.removeCallbacks(ttHideButtons);
        timer.postDelayed(ttHideButtons, millis);
    }

// *************************************************************************************************
// ********* Dashboard related stuff                                                      **********
// *************************************************************************************************

    public void setDashboardVisibility(boolean visibitity){
        dashboard.setVisibility(visibitity?VISIBLE:INVISIBLE);
    }

    public ViewGroup createDashboardEntry(){
        TableRow tr = new TableRow(context);
        TableLayout.LayoutParams llParms = new TableLayout.LayoutParams(0, LayoutParams.MATCH_PARENT);
        tr.setLayoutParams(llParms);
        return tr;
    }

    public PrefTextView createDashboardPTV(ViewGroup vgDashboard, float weight) {
        PrefTextView ptv = new PrefTextView(context);
        vgDashboard.addView(ptv);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, LayoutParams.MATCH_PARENT);
        int margin = convertDp(0.8f);
        params.setMargins(margin,margin,margin,margin);
        params.weight = weight;
        ptv.setLayoutParams(params);

        int padding = convertDp(2.0f);
        ptv.setPadding(padding, padding, padding, padding);
        int drawablePadding = convertDp(3.0f);
        ptv.setCompoundDrawablePadding(drawablePadding);
        Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.quick2, getContext().getTheme());

        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        ptv.setCompoundDrawables(drawable,null,null,null);
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+drawable.getIntrinsicWidth() +" "+ drawable.getIntrinsicHeight()+" "+drawable.getBounds());
        ptv.setText("");
        ptv.setLines(1);
        ptv.setOnClickListener(hintControl);
        return ptv;
    }

    public void setViewGroupColors(ViewGroup viewGroup, int textColorId, int bgColorId){
        for (int idx=0; idx<viewGroup.getChildCount(); idx++){
            if (viewGroup.getChildAt(idx) instanceof TextView) {
                TextView tv = (TextView) viewGroup.getChildAt(idx);
                tv.setTextColor(CC.getColor(textColorId));
                tv.setBackgroundColor(CC.getColor(bgColorId));
            }
        }
    }

    private boolean setDashboradEntryVisibility(ViewGroup dashboardEntry, boolean shouldBeVisible, boolean isVisible){
        int idx = 0;
        if (shouldBeVisible && !isVisible){
            for (ViewGroup entry : dashboardEntries){
                if (entry == dashboardEntry){
                    break;
                } else {
                    if (entry.getParent() != null){
                        idx++;
                    }
                }
            }
            dashboard.addView(dashboardEntry, idx);
        }
        if (!shouldBeVisible && isVisible) {
            dashboard.removeView(dashboardEntry);
        }
        return shouldBeVisible; // means finally it is visible
    }

    public void setDashboardValue(boolean condition, ViewGroup dashboardEntry, TrackLogStatistic statistic){
        if (setDashboradEntryVisibility(dashboardEntry, condition && (statistic != null) , (dashboardEntry.getParent() != null))){
            String sIdx = "I="+statistic.getSegmentIdx();
            if (statistic.getSegmentIdx() == -1) sIdx = "All";
            if (statistic.getSegmentIdx() == -2) sIdx = "R";
//            String sIdx = ("I"+statistic.getSegmentIdx()).replaceFirst("I=-1","All").replaceFirst("I=-2","R");
            ((PrefTextView) dashboardEntry.getChildAt(0)).setValue(sIdx);
            ((PrefTextView) dashboardEntry.getChildAt(1)).setValue(statistic.getTotalLength());
            ((PrefTextView) dashboardEntry.getChildAt(2)).setValue(statistic.getGain());
            ((PrefTextView) dashboardEntry.getChildAt(3)).setValue(statistic.getLoss());
            ((PrefTextView) dashboardEntry.getChildAt(4)).setValue(statistic.duration);
        }
    }

    // *************************************************************************************************
    // ********* Alpha Slider related stuff                                                   **********
    // *************************************************************************************************

    Map<String, SeekBar> mapSliders = new HashMap<>();
    Map<String, TextView> mapSliderNames = new HashMap<>();
    private void prepareAlphaSliders(){
        mapSliders.put(context.getResources().getString( R.string.Layers_pref_chooseMap1_key), (SeekBar) findViewById(R.id.sb1));
        mapSliders.put(context.getResources().getString( R.string.Layers_pref_chooseMap2_key), (SeekBar) findViewById(R.id.sb2));
        mapSliders.put(context.getResources().getString( R.string.Layers_pref_chooseMap3_key), (SeekBar) findViewById(R.id.sb3));
        mapSliders.put(context.getResources().getString( R.string.Layers_pref_chooseMap4_key), (SeekBar) findViewById(R.id.sb4));
        mapSliders.put(context.getResources().getString( R.string.Layers_pref_chooseMap5_key), (SeekBar) findViewById(R.id.sb5));

        mapSliderNames.put(context.getResources().getString( R.string.Layers_pref_chooseMap1_key), (TextView) findViewById(R.id.sbt1));
        mapSliderNames.put(context.getResources().getString( R.string.Layers_pref_chooseMap2_key), (TextView) findViewById(R.id.sbt2));
        mapSliderNames.put(context.getResources().getString( R.string.Layers_pref_chooseMap3_key), (TextView) findViewById(R.id.sbt3));
        mapSliderNames.put(context.getResources().getString( R.string.Layers_pref_chooseMap4_key), (TextView) findViewById(R.id.sbt4));
        mapSliderNames.put(context.getResources().getString( R.string.Layers_pref_chooseMap5_key), (TextView) findViewById(R.id.sbt5));

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        LinearLayout parentLayer = findViewById(R.id.bars);
        for (String prefKey : application.getMapLayerKeys()) {
            final String key = sharedPreferences.getString(prefKey, "");
            final String alphakey = "ControlView_alpha_"+key;
            SeekBar seekBar = mapSliders.get(prefKey);
            TextView seekBarName = mapSliderNames.get(prefKey);
            if (MGMapLayerFactory.hasAlpha(key)) {
                seekBarName.setText(key);
                float alpha = MGMapLayerFactory.getMapLayerAlpha(key);
                alpha = sharedPreferences.getFloat(alphakey, alpha);
                Log.d(MGMapApplication.LABEL, NameUtil.context()+" "+alphakey+" "+alpha);
                MGMapLayerFactory.setMapLayerAlpha(key, alpha);
                seekBar.setProgress((int) (alpha*100));

                seekBar.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) { }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) { }

                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        Layer layer = MGMapLayerFactory.getMapLayer(key);
                        if (layer instanceof TileLayer) {
                            float alpha = progress/100.0f;
                            if (!fromUser){
                                alpha = sharedPreferences.getFloat(alphakey, alpha);
                                seekBar.setProgress((int) (alpha*100));
                            }
                            MGMapLayerFactory.setMapLayerAlpha(key, alpha);
                            Log.d(MGMapApplication.LABEL, NameUtil.context()+" "+alphakey+" "+alpha+ " "+seekBar);
                            sharedPreferences.edit().putFloat(alphakey, alpha).apply();
                            mapView.getLayerManager().redrawLayers();
                        }
                        Log.d(MGMapApplication.LABEL, NameUtil.context()+" progress="+progress);
                    }
                });

            } else {
                parentLayer.removeView(seekBar);
                parentLayer.removeView(seekBarName);
            }
        }
    }

    // *************************************************************************************************
    // ********* Status line related stuff                                                    **********
    // *************************************************************************************************

    public PrefTextView setStatusLineLayout(PrefTextView ptv, float weight){
        TableRow.LayoutParams llParms = new TableRow.LayoutParams(0, LayoutParams.MATCH_PARENT);
        int margin = convertDp(0.8f);
        llParms.setMargins(margin,margin,margin,margin);
        llParms.weight = weight;
        ptv.setLayoutParams(llParms);

        int padding = convertDp(2.0f);
        ptv.setPadding(padding, padding, padding, padding);
        int drawablePadding = convertDp(3.0f);
        ptv.setCompoundDrawablePadding(drawablePadding);
        Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.quick2, getContext().getTheme());

        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        ptv.setCompoundDrawables(drawable,null,null,null);
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+drawable.getIntrinsicWidth() +" "+ drawable.getIntrinsicHeight()+" "+drawable.getBounds());
        ptv.setText("");
        ptv.setLines(1);
        ptv.setTextColor(CC.getColor(R.color.BLACK));
        ptv.setBackgroundColor(CC.getColor(R.color.WHITE_A150));
        return ptv;
    }

    void reworkStatusLine(){
        int cntVisible = 0;
        for (TextView tv : tvList){
            boolean shouldBeVisible = (tv.getText() != null) && (!("".equals(tv.getText()) ) && (cntVisible < 5));
            boolean isVisible = (tv.getParent() != null);
            if (shouldBeVisible && !isVisible){
                tr_states.addView(tv, cntVisible);
            }
            if (!shouldBeVisible && isVisible){
                tr_states.removeView(tv);
            }
            if (shouldBeVisible) cntVisible++;
        }
    }

    public void setStatusLineValue(PrefTextView ptv, Object value){
        if (ptv != null) {
            ptv.setValue(value);
            reworkStatusLine();
        }
    }

    void prepareStatusLine(){
        tr_states = activity.findViewById(R.id.tr_states);
        for (int idx=0; idx<tr_states.getChildCount();idx++){
            tvList.add((TextView) tr_states.getChildAt(idx));
        }
    }

    void finalizeStatusLine(){
        tr_states = activity.findViewById(R.id.tr_states);
        for (int idx=0; idx<tr_states.getChildCount();idx++){
            tr_states.getChildAt(idx).setOnClickListener(hintControl);
        }
    }


}