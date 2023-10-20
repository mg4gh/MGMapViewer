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
package mg.mgmap.activity.mgmap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;

import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.model.DisplayModel;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.activity.mgmap.util.CC;
import mg.mgmap.activity.mgmap.util.EnlargeControl;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.activity.mgmap.view.LabeledSlider;

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

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

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

    TextView tv_enlarge = null;
    EnlargeControl enlargeControl = null;

    private int statusBarHeight;
    private int navigationBarHeight;
    Pref<String> prefVerticalFullscreenOffset;
    public ArrayList<View> variableVerticalOffsetViews = new ArrayList<>();

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

    public static int dp(float f){
        return (int) (f * DisplayModel.getDeviceScaleFactor());
    }



    public void init(final MGMapApplication application, final MGMapActivity activity){
        try {
            this.application = application;
            this.activity = activity;
            ControlComposer controlComposer = new ControlComposer();

            mapView = activity.getMapsforgeMapView();
            // this is necessary to get the scalbar above the quick controls and the status line
            mapView.getMapScaleBar().setMarginVertical(dp(65));

            prepareEnlargeControl();

            // initialize the dashboardKeys and dashboardMap object and then hide dashboard entries
            dashboard = findViewById(R.id.dashboard);
            controlComposer.composeDashboard(activity, this);
            initSystemBarHeight(activity);
            variableVerticalOffsetViews.add(this);
            prefVerticalFullscreenOffset = activity.getPrefCache().get(R.string.preferences_display_fullscreen_offset_key, ""+statusBarHeight);

            controlComposer.composeAlphaSlider(activity,this);
            controlComposer.composeAlphaSlider2(activity,this);
            registerSliderObserver(); // do this after the init call, which set the visibility prefs
            reworkLabeledSliderVisibility();

            controlComposer.composeStatusLine(activity, this);
            finalizeStatusLine();

            controlComposer.composeQuickControls(activity, this);

            controlComposer.composeHelpControls(activity, this);
        } catch (Exception e){
            mgLog.e(e);
        }
    }

    public void setVerticalOffset(){
        initSystemBarHeight(activity);
        boolean fullscreen =  activity.getPrefCache().get(R.string.FSControl_qcFullscreenOn, true).getValue();
        int top = statusBarHeight;
        if ( fullscreen ){ // is fullscreen
            try{
                top = Integer.parseInt(prefVerticalFullscreenOffset.getValue());
            } catch (NumberFormatException e) {
                mgLog.e(e);
            }
        }
        int bottom = ControlView.dp(2)+(fullscreen?0:navigationBarHeight);
        for (View view : variableVerticalOffsetViews){
            RelativeLayout.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, top,0,bottom);
            view.setLayoutParams(params);
        }
        mapView.getMapScaleBar().setMarginVertical(bottom + dp(65));
        mapView.getMapScaleBar().redrawScaleBar();
        activity.getPrefCache().get(R.string.FSPosition_pref_RefreshMapView, false).toggle();
    }


    @SuppressLint({"InternalInsetResource", "DiscouragedApi"})
    public void initSystemBarHeight(Activity activity) {
        Resources myResources = activity.getResources();
        int idStatusBarHeight = myResources.getIdentifier( "status_bar_height", "dimen", "android");
        statusBarHeight = (idStatusBarHeight > 0)?activity.getResources().getDimensionPixelSize(idStatusBarHeight):ControlView.dp(24);
        int idNavBarHeight = myResources.getIdentifier( "navigation_bar_height", "dimen", "android");
        navigationBarHeight = (idNavBarHeight > 0)?activity.getResources().getDimensionPixelSize(idNavBarHeight):ControlView.dp(24);
        mgLog.i("statusBarHeight="+statusBarHeight+" navigationBarHeight"+navigationBarHeight);
    }

// *************************************************************************************************
// ********* EnlargeControl related stuff                                                      **********
// *************************************************************************************************

    void prepareEnlargeControl(){
        tv_enlarge = findViewById(R.id.enlarge);
        tv_enlarge.setVisibility(INVISIBLE);
        enlargeControl = new EnlargeControl(tv_enlarge);

        View parent = (View) tv_enlarge.getParent();
        RelativeLayout.LayoutParams parentLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        parentLayoutParams.setMargins(dp(60), dp(350), dp(60), dp(0));
        parent.setLayoutParams(parentLayoutParams);

        tv_enlarge.setLines(1);
        tv_enlarge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 40);
        int padding = dp(5);
        tv_enlarge.setPadding(padding,padding,padding,padding);
        tv_enlarge.setBackgroundResource(R.drawable.shape);
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

    public ExtendedTextView createDashboardETV(ViewGroup vgDashboard, float weight) {
        ExtendedTextView etv = new ExtendedTextView(context).setDrawableSize(dp(16));
        vgDashboard.addView(etv);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, LayoutParams.MATCH_PARENT);
        int margin = dp(0.8f);
        params.setMargins(margin,margin,margin,margin);
        params.weight = weight;
        etv.setLayoutParams(params);

        int padding = dp(2.0f);
        etv.setPadding(padding, padding, padding, padding);
        int drawablePadding = dp(3.0f);
        etv.setCompoundDrawablePadding(drawablePadding);
        Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.quick2, getContext().getTheme());
        if (drawable != null){
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        }
        etv.setCompoundDrawables(drawable,null,null,null);
        etv.setText("");
        etv.setLines(1);
        etv.setOnClickListener(enlargeControl);
        return etv;
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
            assert statistic != null;
            String sIdx = "I="+statistic.getSegmentIdx();
            if (statistic.getSegmentIdx() == -1) sIdx = "All";
            if (statistic.getSegmentIdx() == -2) sIdx = "R";
            ((ExtendedTextView) dashboardEntry.getChildAt(0)).setValue(sIdx);
            ((ExtendedTextView) dashboardEntry.getChildAt(1)).setValue(statistic.getTotalLength());
            ((ExtendedTextView) dashboardEntry.getChildAt(2)).setValue(statistic.getGain());
            ((ExtendedTextView) dashboardEntry.getChildAt(3)).setValue(statistic.getLoss());
            ((ExtendedTextView) dashboardEntry.getChildAt(4)).setValue(statistic.getDuration());
        }
    }

    // *************************************************************************************************
    // ********* Alpha Slider related stuff                                                   **********
    // *************************************************************************************************


    HashMap<ViewGroup, ArrayList<LabeledSlider>> labeledSliderMap = new HashMap<>();
    Observer sliderVisibilityChangeObserver = (e) -> reworkLabeledSliderVisibility();

    LabeledSlider createLabeledSlider(ViewGroup parent){
        LabeledSlider labeledSlider = new LabeledSlider(context);
        parent.addView(labeledSlider);

        ArrayList<LabeledSlider> childList = labeledSliderMap.computeIfAbsent(parent, k -> new ArrayList<>());
        childList.add(labeledSlider);
        return labeledSlider;
    }

    private void registerSliderObserver() {
        for (ViewGroup parent : labeledSliderMap.keySet()) {
            for (LabeledSlider slider : Objects.requireNonNull(labeledSliderMap.get(parent))) {
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

    public ExtendedTextView createStatusLineETV(ViewGroup vgParent, float weight){
        ExtendedTextView etv = new ExtendedTextView(context).setDrawableSize(dp(16));
        vgParent.addView(etv);
        tvList.add(etv);
        TableRow.LayoutParams llParms = new TableRow.LayoutParams(0, LayoutParams.MATCH_PARENT);
        int margin = dp(0.8f);
        llParms.setMargins(margin,margin,margin,margin);
        llParms.weight = weight;
        etv.setLayoutParams(llParms);

        int padding = dp(2.0f);
        etv.setPadding(padding, padding, padding, padding);
        int drawablePadding = dp(3.0f);
        etv.setCompoundDrawablePadding(drawablePadding);
        Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.quick2, getContext().getTheme());
        if (drawable != null){
            drawable.setBounds(0, 0, etv.getDrawableSize(), etv.getDrawableSize());
        }
        etv.setCompoundDrawables(drawable,null,null,null);
        etv.setText("");
        etv.setLines(1);
//        etv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        etv.setTextColor(CC.getColor(R.color.BLACK));
        etv.setBackgroundColor(CC.getColor(R.color.WHITE_A150));
        return etv;
    }

    void reworkStatusLine(){
        int cntVisible = 0;
        for (TextView tv : tvList){
            boolean shouldBeVisible = (tv.getText() != null) && (!("".contentEquals(tv.getText()) ) && (cntVisible < 5));
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

    public void setStatusLineValue(ExtendedTextView etv, Object value){
        if (etv != null) {
            if (etv.setValue(value)){
                mgLog.i(etv.getLogText());
                reworkStatusLine();
            }
        }
    }

    void finalizeStatusLine(){
        tr_states = activity.findViewById(R.id.tr_states);
        for (int idx=0; idx<tr_states.getChildCount();idx++){
            tr_states.getChildAt(idx).setOnClickListener(enlargeControl);
        }
    }

    // *************************************************************************************************
    // ********* Quick controls related stuff                                                 **********
    // *************************************************************************************************

    public static ExtendedTextView createQuickControlETV(ViewGroup parent) {
        Context context = parent.getContext();
        ExtendedTextView etv = new ExtendedTextView(context).setDrawableSize(dp(36));
        parent.addView(etv);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, LayoutParams.MATCH_PARENT);
        int margin = dp(1.5f);
        params.setMargins(margin,margin,margin,margin);
        params.weight = 20;
        etv.setLayoutParams(params);

        etv.setPadding(0, dp(4),0, dp(4));
        etv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape, context.getTheme()));
        etv.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((left != oldLeft) || (top != oldTop) || (right != oldRight) || (bottom != oldBottom)){
                int paddingHorizontal = Math.max((right-left - etv.getDrawableSize()) / 2, 0);
                etv.setPadding(paddingHorizontal,etv.getPaddingTop(),paddingHorizontal,etv.getPaddingBottom());
            }
        });
        return etv;
    }

    public static ExtendedTextView createControlETV(ViewGroup parent) {
        Context context = parent.getContext();
        ExtendedTextView etv = new ExtendedTextView(context);
        parent.addView(etv, 0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        int margin = dp(2f);
        params.setMargins(margin,margin,margin,margin);
        etv.setLayoutParams(params);
        etv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        etv.setTextColor(CC.getColor(R.color.WHITE));

        etv.setPadding(dp(4), dp(4),dp(4), dp(4));
        etv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape, context.getTheme()));
        return etv;
    }


    public static TableRow createRow(Context context){
        TableRow tableRow = new TableRow(context);
        tableRow.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return tableRow;
    }


    // *************************************************************************************************
    // ********* Help related stuff                                                           **********
    // *************************************************************************************************

    public LinearLayout createHelpPanel(ViewGroup parent, int gravity, int rotation){
        LinearLayout ll = new LinearLayout(parent.getContext());
        TableLayout.LayoutParams lp_ll3 = new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ll.setLayoutParams(lp_ll3);
        ll.setOrientation(LinearLayout.VERTICAL);
        parent.addView(ll);
        ll.setGravity(gravity);
        ll.setRotation(rotation);
        return ll;
    }

    public TextView createHelpText1(ViewGroup parent){
        Point displaySize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
        int size = (displaySize.x / 7)-dp(1f);

        ExtendedTextView tv = new ExtendedTextView(parent.getContext());
        LinearLayout.LayoutParams lp_tv = new LinearLayout.LayoutParams(size, size);
        lp_tv.setMargins(dp(1),dp(1),dp(1),dp(2));
        tv.setLayoutParams(lp_tv);
        tv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape, context.getTheme()));
        parent.addView(tv);
        Drawable drawable = ResourcesCompat.getDrawable(context.getResources(), R.drawable.exit, context.getTheme());
        if (drawable != null){
            drawable.setBounds(0,0,dp(36),dp(36));
            tv.setCompoundDrawables(drawable,null,null,null);
        }
        tv.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((left != oldLeft) || (top != oldTop) || (right != oldRight) || (bottom != oldBottom)){
                int paddingHorizontal = Math.max((right-left - dp(36)) / 2, 0);
                int paddingVertical = Math.max((bottom-top - dp(36)) / 2, 0);
                tv.setPadding(paddingHorizontal,paddingVertical,paddingHorizontal,paddingVertical);
            }
        });
        return tv;
    }

    public TextView createHelpText2(ViewGroup parent){
        Point displaySize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);

        TextView tv = new TextView(parent.getContext());
        int helpLength = (int)(displaySize.y * 0.7);
        parent.getLayoutParams().width = helpLength;
        parent.getLayoutParams().height = helpLength;
        LinearLayout.LayoutParams lp_tv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT , (displaySize.x / 7)-dp(1f));
        lp_tv.setMargins(dp(1),dp(0.75f),dp(1),dp(0.75f));
        tv.setLayoutParams(lp_tv);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        tv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape, context.getTheme()));
        tv.setTextColor(CC.getColor(R.color.WHITE));
        tv.setPadding(dp(16),0,dp(8),0);
        parent.addView(tv);
        return tv;
    }

    public TextView createHelpText3(ViewGroup parent){
        TextView tv = new TextView(parent.getContext());
        TableRow.LayoutParams lp_tv3 = new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp_tv3.height=dp(48);
        tv.setLayoutParams(lp_tv3);
        tv.setText(" ");
        parent.addView(tv);
        return tv;
    }

}