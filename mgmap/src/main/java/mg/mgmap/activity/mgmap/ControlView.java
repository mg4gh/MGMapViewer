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

import android.content.Context;

import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observer;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.activity.mgmap.util.CC;
import mg.mgmap.activity.mgmap.util.EnlargeControl;
import mg.mgmap.generic.util.basic.NameUtil;
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

//    private final Map<View, ArrayList<View>> submenuMap = new HashMap<>();
//    private final Map<View, Control> menuControlMap = new HashMap<>();

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

            controlComposer.composeDashboard(application, activity, this);

            controlComposer.composeAlphaSlider(application,activity,this);
            controlComposer.composeAlphaSlider2(application,activity,this);
            registerSliderObserver(); // do this after the init call, which set the visibility prefs
            reworkLabeledSliderVisibility();

            controlComposer.composeStatusLine(application, activity, this);
            finalizeStatusLine();

            controlComposer.composeQuickControls(application, activity, this);

            controlComposer.composeHelpControls(application, activity, this);
        } catch (Exception e){
            Log.e(MGMapApplication.LABEL, NameUtil.context()+"", e);
        }
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
        parentLayoutParams.setMargins(dp(90), dp(350), dp(90), dp(0));
        parent.setLayoutParams(parentLayoutParams);

        tv_enlarge.setLines(1);
        tv_enlarge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
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

        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
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
            String sIdx = "I="+statistic.getSegmentIdx();
            if (statistic.getSegmentIdx() == -1) sIdx = "All";
            if (statistic.getSegmentIdx() == -2) sIdx = "R";
//            String sIdx = ("I"+statistic.getSegmentIdx()).replaceFirst("I=-1","All").replaceFirst("I=-2","R");
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
    Observer sliderVisibilityChangeObserver = (o, arg) -> reworkLabeledSliderVisibility();

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
        etv.setTextColor(CC.getColor(R.color.BLACK));
        etv.setBackgroundColor(CC.getColor(R.color.WHITE_A150));
        return etv;
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

    public void setStatusLineValue(ExtendedTextView etv, Object value){
        if (etv != null) {
            if (etv.setValue(value)){
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

    public static ExtendedTextView createQuickControlETV(ViewGroup vgQuickControls) {
        Context context = vgQuickControls.getContext();
        ExtendedTextView etv = new ExtendedTextView(context).setDrawableSize(dp(36));
        vgQuickControls.addView(etv);

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
                int paddingVertical = Math.max((bottom-top - etv.getDrawableSize()) / 2, 0);
                etv.setPadding(paddingHorizontal,paddingVertical,paddingHorizontal,paddingVertical);
            }
        });
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
        int size = (displaySize.x / 7)-dp(1.8f);

        TextView tv = new TextView(parent.getContext());
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
        LinearLayout.LayoutParams lp_tv = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT , (displaySize.x / 7)-dp(1.8f));
        lp_tv.setMargins(dp(1),dp(1),dp(1),dp(1));
        tv.setLayoutParams(lp_tv);
        tv.setGravity(Gravity.CENTER_VERTICAL);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
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