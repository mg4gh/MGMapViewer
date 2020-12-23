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

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.res.ResourcesCompat;

import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.model.DisplayModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import mg.mapviewer.model.TrackLogStatistic;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.HintControl;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.Control;
import mg.mapviewer.view.LabeledSlider;
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

    /** There are five dashboard entries, each with a specific semantic. This ordered list contains these five entries. */
    ArrayList<ViewGroup> dashboardEntries = new ArrayList<>();
    /** Parent of the dashboard entries. */
    ViewGroup dashboard;
    /** Reference to the MapView object - some controls change properties of the mapView */
    MapView mapView = null;


    /** parent object for status line */
    TableRow tr_states = null;
    /** List will be filled with all members of the status line */
    ArrayList<TextView> tvList = new ArrayList<>();

    TextView tv_hint = null;
    HintControl hintControl = null;

    private Map<View, ArrayList<View>> submenuMap = new HashMap<>();
    private Map<View, Control> menuControlMap = new HashMap<>();

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

    public static int convertDp(float dp){
        return (int) (dp * DisplayModel.getDeviceScaleFactor());
    }



    public void init(final MGMapApplication application, final MGMapActivity activity){
        try {
            this.application = application;
            this.activity = activity;
            ControlComposer controlComposer = new ControlComposer();

            mapView = activity.getMapsforgeMapView();
            // this is necessary to get the scalbar above the quick controls and the status line
            mapView.getMapScaleBar().setMarginVertical((int)(convertDp(65)));

            prepareHintControl();

            // initialize the dashboardKeys and dashboardMap object and then hide dashboard entries
            dashboard = findViewById(R.id.dashboard);

            controlComposer.composeDashboard(application, activity, this);

            controlComposer.composeMenu(application, activity, this);
            setMenuVisibility(false);

            controlComposer.composeAlphaSlider(application,activity,this);
            controlComposer.composeAlphaSlider2(application,activity,this);
            registerSliderObserver(); // do this after the init call, which set the visibility prefs
            reworkLabeledSliderVisibility();

            controlComposer.composeStatusLine(application, activity, this);
            finalizeStatusLine();

            controlComposer.composeQuickControls(application, activity, this);

        } catch (Exception e){
            Log.e(MGMapApplication.LABEL, NameUtil.context()+"", e);
        }
    }

// *************************************************************************************************
// ********* HintControl related stuff                                                      **********
// *************************************************************************************************

    void prepareHintControl(){
        tv_hint = findViewById(R.id.hint);
        tv_hint.setVisibility(INVISIBLE);
        hintControl = new HintControl(tv_hint);

        View parent = (View)tv_hint.getParent();
        RelativeLayout.LayoutParams parentLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        parentLayoutParams.setMargins(convertDp(90),convertDp(350),convertDp(90),convertDp(0));
        parent.setLayoutParams(parentLayoutParams);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tv_hint.setLines(1);
        tv_hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
        int padding = convertDp(5);
        tv_hint.setPadding(padding,padding,padding,padding);
        tv_hint.setBackgroundResource(R.drawable.shape);
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
        PrefTextView ptv = new PrefTextView(context).setDrawableSize(convertDp(16));
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

    private boolean setDashboardEntryVisibility(ViewGroup dashboardEntry, boolean shouldBeVisible, boolean isVisible){
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
        if (setDashboardEntryVisibility(dashboardEntry, condition && (statistic != null) , (dashboardEntry.getParent() != null))){
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
// ********* Menu Button creation  ---  related stuff                                     **********
// *************************************************************************************************

    Button createMenuButton(ConstraintLayout parent, View alignViewTop, View alignViewSide, boolean alignEnd, int submenuEntries) {
        boolean isSubmenuButton = (parent != alignViewSide);
        Button button = new AppCompatButton(context) {
            @Override
            public void setEnabled(boolean enabled) {
                super.setEnabled(enabled);
                this.setTextColor(enabled ? CC.getColor(R.color.WHITE) : CC.getColor(R.color.WHITE_A100));
            }
        };
        parent.addView(button);
        button.setVisibility(VISIBLE);
        button.setId(View.generateViewId());
        button.setBackgroundResource(R.drawable.shape);
        button.setPadding(0, 0, 0, 0);
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = convertDp(2.5f);
        layoutParams.setMargins(margin, margin, margin, margin);
        button.setLayoutParams(layoutParams);

        ConstraintSet set = new ConstraintSet();
        set.clone(parent);
        if (alignViewTop == null) {
            if (isSubmenuButton){ // then it's a main menu button
                set.connect(button.getId(), ConstraintSet.TOP, alignViewSide.getId(), ConstraintSet.TOP, convertDp(0));
            } else {
                set.connect(button.getId(), ConstraintSet.TOP, parent.getId(), ConstraintSet.TOP, convertDp(150));
            }
        } else {
            set.connect(button.getId(), ConstraintSet.TOP, alignViewTop.getId(), ConstraintSet.BOTTOM, convertDp(2.5f));
        }
        int startSide = (alignEnd)?ConstraintSet.RIGHT:ConstraintSet.LEFT;
        int endSide =  (alignEnd == isSubmenuButton)?ConstraintSet.LEFT:ConstraintSet.RIGHT;
        set.connect(button.getId(), startSide, alignViewSide.getId(), endSide, convertDp(2.5f));
        set.applyTo(parent);

        if (submenuEntries > 0){
            submenuMap.put(button, new ArrayList<>()); // int List for submenu buttons
            View alignSubMenuTop = null;
            for (int i=0; i<submenuEntries; i++){
                alignSubMenuTop = createMenuButton(parent, alignSubMenuTop, button, alignEnd, 0);
            }
        } else {
            if (!isSubmenuButton){
                submenuMap.put(button, null); // need this entry to detect all main menu entries
            }
        }
        if (isSubmenuButton){
            submenuMap.get(alignViewSide).add(button);
        }
        return button;
    }

// *************************************************************************************************
// ********* Menu Button and Control registration stuff                                   **********
// *************************************************************************************************

    View registerMenuControl(View view, Control control){
        if (control != null){
            control.setControlView(this);
            view.setOnClickListener(control);
            menuControlMap.put(view, control);
        }
        return view;
    }

    void registerSubmenuControls(ArrayList<View> submenu, Control[] controls){
        if (submenu.size() < controls.length){
            Log.e(MGMapApplication.LABEL, NameUtil.context() + " too much controls("+controls.length+") for menu("+submenu.size()+")");
        }
        for (View submenuView : submenu){
            registerMenuControl(submenuView, controls[submenu.indexOf(submenuView)]);
        }
    }

    View registerMenuControls(View button, final String name, Control[] controls){
        registerSubmenuControls(submenuMap.get(button), controls);
        Control control = new Control(false){
            @Override
            public void onClick(View v) {
                super.onClick(v);
                disableMainMenuButtons();
                if (submenuMap.get(button).get(0).getVisibility()==VISIBLE){
                    setMenuVisibility(false);
                } else {
                    for (View subButton : submenuMap.get(button)){
                        showMenuButton(subButton);
                    }
                    button.setEnabled(true);
                }
            }

            @Override
            public void onPrepare(View v) {
                setText(v, name);
            }
        };
        return registerMenuControl(button, control);
    }

    void disableMainMenuButtons(){
        for (View chview : submenuMap.keySet()){
            if (chview instanceof Button) {
                chview.setEnabled(false); // disable main menu entries by default
            }
        }
    }

    void showMenuButton(View view){
        view.setEnabled(true);
        menuControlMap.get(view).onPrepare(view);
        view.setVisibility( VISIBLE );
    }

    void setMenuVisibility(boolean visibility){
        View menu = findViewById(R.id.menuBase);
        menu.setVisibility(visibility?VISIBLE:INVISIBLE);
        if (visibility) {
            for (View button : submenuMap.keySet()){ // the keyset contains the main menu buttons
                showMenuButton(button);
            }
            startTTHideButtons(7000);
        } else {
            for (View view : submenuMap.keySet()){
                view.setVisibility(INVISIBLE);
                ArrayList<View> submenu = submenuMap.get(view);
                if (submenu != null){
                    for (View subview : submenu){
                        subview.setVisibility(INVISIBLE);
                    }
                }
            }
        }
    }

    void toggleMenuVisibility(){
        setMenuVisibility(findViewById(R.id.menuBase).getVisibility() != VISIBLE);
    }

    private Handler timer = new Handler();
    final Runnable ttHideButtons = new Runnable() { // define timerTask to hide menu buttons
        @Override
        public void run() {
            setMenuVisibility(false);
            timer.removeCallbacks(ttHideButtons);
        }
    };
    public void startTTHideButtons(long millis){
        timer.removeCallbacks(ttHideButtons);
        timer.postDelayed(ttHideButtons, millis);
    }


    // *************************************************************************************************
    // ********* Alpha Slider related stuff                                                   **********
    // *************************************************************************************************


    HashMap<ViewGroup, ArrayList<LabeledSlider>> labeledSliderMap = new HashMap<>();
    Observer sliderVisibilityChangeObserver = new Observer() {
        @Override
        public void update(Observable o, Object arg) {
            reworkLabeledSliderVisibility();
        }
    };

    LabeledSlider createLabeledSlider(ViewGroup parent){
        LabeledSlider labeledSlider = new LabeledSlider(context);
        parent.addView(labeledSlider);

        ArrayList<LabeledSlider> childList = labeledSliderMap.get(parent);
        if (childList == null){
            childList = new ArrayList<>();
            labeledSliderMap.put(parent, childList);
        }
        childList.add(labeledSlider);
        return labeledSlider;
    }

    private void registerSliderObserver() {
        for (ViewGroup parent : labeledSliderMap.keySet()) {
            for (LabeledSlider slider : labeledSliderMap.get(parent)) {
                slider.getPrefSliderVisibility().addObserver(sliderVisibilityChangeObserver);
            }
        }
    }
    

    public void reworkLabeledSliderVisibility(){
        for (ViewGroup parent : labeledSliderMap.keySet()){
            if (parent.getVisibility() == VISIBLE){
                ArrayList<LabeledSlider> children = labeledSliderMap.get(parent);
                int idxInParent = 0;
                if (children != null){
                    for (LabeledSlider slider : children){
                        if (slider.getPrefSliderVisibility().getValue()){  // should be visible
                            if (slider.getParent() != parent){
                                parent.addView(slider, idxInParent);
                            }
                            idxInParent++;
                        } else {                                            // should be invisible
                            if (slider.getParent() == parent){
                                parent.removeView(slider);
                            }
                        }
                    }
                }
            }
        }

    }

    // *************************************************************************************************
    // ********* Status line related stuff                                                    **********
    // *************************************************************************************************

    public PrefTextView createStatusLinePTV(ViewGroup vgParent, float weight){
        PrefTextView ptv = new PrefTextView(context).setDrawableSize(convertDp(16));
        vgParent.addView(ptv);
        tvList.add(ptv);
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
        if (drawable != null){
            drawable.setBounds(0, 0, ptv.getDrawableSize(), ptv.getDrawableSize());
        }
        ptv.setCompoundDrawables(drawable,null,null,null);
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
            if (ptv.setValue(value)){
                reworkStatusLine();
            }
        }
    }

    void finalizeStatusLine(){
        tr_states = activity.findViewById(R.id.tr_states);
        for (int idx=0; idx<tr_states.getChildCount();idx++){
            tr_states.getChildAt(idx).setOnClickListener(hintControl);
        }
    }

    // *************************************************************************************************
    // ********* Quick controls related stuff                                                 **********
    // *************************************************************************************************

    public static PrefTextView createQuickControlPTV(ViewGroup vgQuickControls, float weight) {
        Context context = vgQuickControls.getContext();
        PrefTextView ptv = new PrefTextView(context).setDrawableSize(convertDp(32));
        vgQuickControls.addView(ptv);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, LayoutParams.MATCH_PARENT);
        int margin = convertDp(1.5f);
        params.setMargins(margin,margin,margin,margin);
        params.weight = weight;
        ptv.setLayoutParams(params);

        ptv.setPadding(0,convertDp(4),0,convertDp(4));
        ptv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape, context.getTheme()));
        ptv.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if ((left != oldLeft) || (top != oldTop) || (right != oldRight) || (bottom != oldBottom)){
                    int paddingHorizontal = Math.max((right-left - ptv.getDrawableSize()) / 2, 0);
                    int paddingVertical = Math.max((bottom-top - ptv.getDrawableSize()) / 2, 0);
                    ptv.setPadding(paddingHorizontal,paddingVertical,paddingHorizontal,paddingVertical);
                }
            }
        });
        return ptv;
    }
}