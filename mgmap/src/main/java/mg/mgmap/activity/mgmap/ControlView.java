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
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.WindowInsetsCompat;

import org.mapsforge.map.model.DisplayModel;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.application.MGMapApplication;
import mg.mgmap.R;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.generic.util.CC;
import mg.mgmap.activity.mgmap.util.EnlargeControl;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.ExtendedTextView;
import mg.mgmap.activity.mgmap.view.LabeledSlider;
import mg.mgmap.generic.view.VUtil;

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

    final Context context;
    MGMapApplication application = null;
    MGMapActivity activity = null;

    /** There are five dashboard entries, each with a specific semantic. This ordered list contains these five entries. */
    final ArrayList<ViewGroup> dashboardEntries = new ArrayList<>();
    /** Parent of the dashboard entries. */
    ViewGroup dashboard;
    ViewGroup trackDetails;
    ViewGroup routingProfiles;


    /** parent object for status line */
    ViewGroup tr_states = null;
    /** List will be filled with all members of the status line */
    final ArrayList<TextView> tvList = new ArrayList<>();

    TextView tv_enlarge = null;
    EnlargeControl enlargeControl = null;

    private int statusBarHeight = 0;
    private int navigationBarHeight = 0;
    Pref<String> prefVerticalFullscreenOffset;
    Pref<String> prefVerticalNoneFullscreenOffset;
    public final ArrayList<View> variableVerticalOffsetViews = new ArrayList<>();

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
    public int getStatusBarHeight(){
        return statusBarHeight;
    }
    public int getNavigationBarHeight(){
        return navigationBarHeight;
    }

    public static int dp(float f){
        return (int) (f * DisplayModel.getDeviceScaleFactor());
    }


    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        // This method is used starting from Android 11 (API Level R) to control vertical offsets
        // (previous releases call setVerticalOffset() directly from the prefFullscreenObserver ... see FSControl)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            navigationBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            mgLog.d("statusBarHeight="+statusBarHeight+" navigationBarHeight="+navigationBarHeight);

            if (activity.findViewById(R.id.statusBar) instanceof RelativeLayout statusBarView) {
                ViewGroup.LayoutParams params = statusBarView.getLayoutParams();
                params.height = statusBarHeight;
                statusBarView.setLayoutParams(params);
            }
            setVerticalOffset();
        }
        return super.onApplyWindowInsets(insets);
    }

    public void init(final MGMapApplication application, final MGMapActivity activity){
        try {
            this.application = application;
            this.activity = activity;
            ControlComposer controlComposer = new ControlComposer();
            prepareEnlargeControl();

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                // View R.id.statusBar is a background rectangle to the status bar. It compensates that status bar background color cannot cannot set anymore directly.
                if (activity.findViewById(R.id.statusBar) instanceof RelativeLayout statusBarView) {
                    statusBarView.setVisibility(VISIBLE);
                }
            }

            trackDetails = findViewById(R.id.trackDetails);
            routingProfiles = findViewById(R.id.routingProfiles);
            // initialize the dashboardKeys and dashboardMap object and then hide dashboard entries
            dashboard = findViewById(R.id.dashboard);
            controlComposer.composeDashboard(activity, this);
            initSystemBarHeight(activity);
            variableVerticalOffsetViews.add(this);
            prefVerticalFullscreenOffset = activity.getPrefCache().get(R.string.preferences_display_fullscreen_offset_key, ""+statusBarHeight);
            prefVerticalNoneFullscreenOffset = activity.getPrefCache().get(R.string.preferences_display_none_fullscreen_offset_key, ""+0);

            controlComposer.composeRoutingProfileButtons(activity, this);

            controlComposer.composeAlphaSlider(activity,this);
            getActivity().getPrefCache().get(MGMapLayerFactory.PREF_LAYER_CONFIG, "").addObserver(evt -> controlComposer.configureAlphaSlider(activity,this));
            controlComposer.configureAlphaSlider(activity,this);
            reworkLabeledSliderVisibility();
            controlComposer.composeAlphaSlider2(activity,this);
            reworkLabeledSliderVisibility2();

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
        int top = 0, bottom = dp(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            top = statusBarHeight;
            if (fullscreen){
                try{
                    top = Integer.parseInt(prefVerticalFullscreenOffset.getValue());
                } catch (NumberFormatException e) {
                    mgLog.e(e.getMessage());
                    mgLog.w("Reset prefVerticalFullscreenOffset to default "+statusBarHeight);
                    prefVerticalFullscreenOffset.setValue(""+statusBarHeight);
                }
            } else{
                try{
                    top += Integer.parseInt(prefVerticalNoneFullscreenOffset.getValue());
                } catch (NumberFormatException e) {
                    mgLog.e(e.getMessage());
                    mgLog.w("Reset prefVerticalNoneFullscreenOffset to default "+0);
                    prefVerticalNoneFullscreenOffset.setValue(""+0);
                }
                bottom = navigationBarHeight;
            }
        }
        for (View view : variableVerticalOffsetViews){
            RelativeLayout.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, top,0,bottom);
            view.setLayoutParams(params);
        }
        getActivity().getMapViewUtility().setScaleBarVMargin(bottom + dp(VUtil.QC_HEIGHT_DP*1.5f));
        getActivity().getMapViewUtility().setScaleBarColor((0xFF808080));
        activity.getPrefCache().get(R.string.FSPosition_pref_RefreshMapView, false).toggle();
    }


    @SuppressLint({"InternalInsetResource", "DiscouragedApi"})
    public void initSystemBarHeight(Activity activity) {
        Resources myResources = activity.getResources();
        int idStatusBarHeight = myResources.getIdentifier( "status_bar_height", "dimen", "android");
        statusBarHeight = (idStatusBarHeight > 0)?activity.getResources().getDimensionPixelSize(idStatusBarHeight):ControlView.dp(24);
        int idNavBarHeight = myResources.getIdentifier( "navigation_bar_height", "dimen", "android");
        navigationBarHeight = (idNavBarHeight > 0)?activity.getResources().getDimensionPixelSize(idNavBarHeight):ControlView.dp(24);
        mgLog.d("statusBarHeight="+statusBarHeight+" navigationBarHeight="+navigationBarHeight);
    }

// *************************************************************************************************
// ********* EnlargeControl related stuff                                                 **********
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
        // This method is called, when search takes place - then do not only hide dashboard, but also track details.
        trackDetails.setVisibility(visibitity?VISIBLE:INVISIBLE);
        dashboard.setVisibility(visibitity?VISIBLE:INVISIBLE);
        routingProfiles.setVisibility(visibitity?VISIBLE:INVISIBLE);
    }

    public static class DashboardEntry extends TableRow{
        TrackLogStatistic statistic = null;
        TrackLogStatistic tdStatistic = null;
        private DashboardEntry(Context context){
            super(context);
        }
    }

    public ViewGroup createDashboardEntry(){
        DashboardEntry tr = new DashboardEntry(context);
        TableLayout.LayoutParams llParms = new TableLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        tr.setLayoutParams(llParms);
        return tr;
    }

    public ExtendedTextView createDashboardETV(ViewGroup vgDashboard, float weight) {
        ExtendedTextView etv = new ExtendedTextView(context){
            public void layout(int l, int t, int r, int b) {
                if ((b-t)==(r-l)) return; // 04.02.2024: There seems to be a bug in Android - layout with same height as width - although something different was requested
                super.layout(l, t, r, b);
            }
        };
        etv.setDrawableSize(dp(16));
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
        return etv;
    }

    public void setViewGroupColors(ViewGroup viewGroup, int textColorId, int bgColorId){
        for (int idx=0; idx<viewGroup.getChildCount(); idx++){
            if (viewGroup.getChildAt(idx) instanceof TextView tv) {
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

    public void setDashboardValue(String id, TrackLogStatistic tdStatistic){
        DashboardEntry dashboardEntry = getDashboardEntry(id);
        if (dashboardEntry != null){
            dashboardEntry.tdStatistic=tdStatistic;
            showDashboardValue(dashboardEntry);
        }
    }
    public void setDashboardValue(boolean condition, ViewGroup dashboardEntry, TrackLogStatistic statistic){
        if (setDashboardEntryVisibility(dashboardEntry, condition && (statistic != null) , (dashboardEntry.getParent() != null))){
            assert statistic != null;
            ((DashboardEntry) dashboardEntry).statistic = statistic;
            showDashboardValue((DashboardEntry) dashboardEntry);
        }
    }

    private void showDashboardValue(DashboardEntry dashboardEntry){
        TrackLogStatistic statistic = ((dashboardEntry.tdStatistic != null) && (dashboardEntry.tdStatistic.getNumPoints()>0))?dashboardEntry.tdStatistic:dashboardEntry.statistic;
        String sIdx = (statistic.getSegmentIdx()>=0)?"I="+statistic.getSegmentIdx():TrackLogStatistic.SEGMENT_IDS.get(statistic.getSegmentIdx());
        ((ExtendedTextView) dashboardEntry.getChildAt(0)).setValue(sIdx);
        ((ExtendedTextView) dashboardEntry.getChildAt(1)).setValue(statistic.getTotalLength());
        ((ExtendedTextView) dashboardEntry.getChildAt(2)).setValue(statistic.getGain());
        ((ExtendedTextView) dashboardEntry.getChildAt(3)).setValue(statistic.getLoss());
        ((ExtendedTextView) dashboardEntry.getChildAt(4)).setValue(statistic.getDuration());
    }

    /* Using this way to implement EnlargeControl for DashboardEntryViews allows to get scrollEvents and thus to implement drag and drop for Dashboard */
    public boolean checkDashboardEntryView(float x, float y){
        int[] loc = new int[2];
        for (int i=0; i<dashboard.getChildCount(); i++){
            ViewGroup dashboardEntry = (ViewGroup)dashboard.getChildAt(i);
            dashboardEntry.getLocationOnScreen(loc);
            if ((loc[0] < x) && (x < loc[0]+dashboardEntry.getWidth()) && (loc[1] <= y) && (y < loc[1]+dashboardEntry.getHeight())){
                for (int j=0; j<dashboardEntry.getChildCount(); j++) {
                    View dashboardEntryView = dashboardEntry.getChildAt(j);
                    dashboardEntryView.getLocationOnScreen(loc);
                    if ((loc[0] < x) && (x < loc[0] + dashboardEntryView.getWidth()) && (loc[1] <= y) && (y < loc[1] + dashboardEntryView.getHeight())) {
                        new EnlargeControl(tv_enlarge).onClick(dashboardEntryView);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public String checkDashboardEntry(float x, float y){
        int[] loc = new int[2];
        for (int i=0; i<dashboard.getChildCount(); i++){
            ViewGroup dashboardEntry = (ViewGroup)dashboard.getChildAt(i);
            dashboardEntry.getLocationOnScreen(loc);
            if ((loc[0] < x) && (x < loc[0]+dashboardEntry.getWidth()) && (loc[1] <= y) && (y < loc[1]+dashboardEntry.getHeight())){
                if (Double.parseDouble( ((ExtendedTextView)dashboardEntry.getChildAt(1)).getText().toString().replace("km","") ) > 0.05){ // this requires that the trackLog has at least two points
                    return ((ExtendedTextView)(dashboardEntry.getChildAt(0))).getLogName();
                }
            }
        }
        return null;
    }

    private DashboardEntry getDashboardEntry(String id){
        for (int i=0; i<dashboard.getChildCount(); i++) {
            DashboardEntry dashboardEntry = (DashboardEntry) dashboard.getChildAt(i);
            View view = dashboardEntry.getChildAt(0);
            if (view instanceof ExtendedTextView evt) {
                if (evt.getLogName().equals(id)){
                    return dashboardEntry;
                }
            }
        }
        return null;
    }

    // *************************************************************************************************
    // ********* Routing Profile Button related stuff                                         **********
    // *************************************************************************************************


    public ExtendedTextView createRoutingProfileETV(ViewGroup parent) {
        Context context = parent.getContext();
        ExtendedTextView etv = new ExtendedTextView(context){
            @Override
            protected void onDrawableChanged(Drawable oldDrawable,Drawable newDrawable) {
                if (oldDrawable instanceof Animatable2) {
                    ((Animatable) oldDrawable).stop();
                }
                if (newDrawable instanceof Animatable2 an2) {
                    an2.start();
                    an2.registerAnimationCallback(new Animatable2.AnimationCallback() {
                        @Override
                        public void onAnimationEnd(Drawable drawable) {
                            an2.start();
                        }
                    });
                }
            }
        };
        etv.setDrawableSize(dp(36));

        int hMargin  = dp(5f);
        int vMargin = dp(5);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(hMargin,vMargin,hMargin,vMargin);
        params.weight = 20;
        etv.setLayoutParams(params);

        etv.setPadding(8, dp(4),8, dp(4));
        etv.setBackground(ResourcesCompat.getDrawable(context.getResources(), R.drawable.shape, context.getTheme()));
        etv.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if ((left != oldLeft) || (top != oldTop) || (right != oldRight) || (bottom != oldBottom)){
                int paddingHorizontal = Math.max((right-left - etv.getDrawableSize()) / 2, 0);
                etv.setPadding(paddingHorizontal,etv.getPaddingTop(),paddingHorizontal,etv.getPaddingBottom());
            }
        });
        return etv;
    }

    // *************************************************************************************************
    // ********* Alpha Slider related stuff                                                   **********
    // *************************************************************************************************


    ArrayList<LabeledSlider> labeledSliders = new ArrayList<>(); // for map sliders
    ArrayList<LabeledSlider> labeledSliders2 = new ArrayList<>(); // for track sliders

    final Observer sliderVisibilityChangeObserver2 = (e) -> reworkLabeledSliderVisibility2();

    LabeledSlider createLabeledSlider(ViewGroup parent){
        LabeledSlider labeledSlider = new LabeledSlider(context);
        parent.addView(labeledSlider);
        labeledSliders.add(labeledSlider);
        return labeledSlider;
    }
    LabeledSlider createLabeledSlider2(ViewGroup parent){
        LabeledSlider labeledSlider = new LabeledSlider(context);
        parent.addView(labeledSlider);
        labeledSliders2.add(labeledSlider);
        return labeledSlider;
    }

    void registerSliderVisibilityObserver2() {
        for (LabeledSlider slider : labeledSliders2) {
            slider.getPrefSliderVisibility().addObserver(sliderVisibilityChangeObserver2);
        }
    }
    

    public void reworkLabeledSliderVisibility(){
        ViewGroup parent = findViewById(R.id.bars);
        int idxInParent = 0;
        for (LabeledSlider slider : labeledSliders){
            if (slider.getPrefSlider() != null){  // should be visible
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

    public void reworkLabeledSliderVisibility2(){
        ViewGroup parent = findViewById(R.id.bars2);
        int idxInParent = 0;
        for (LabeledSlider slider : labeledSliders2){
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


    // *************************************************************************************************
    // ********* Status line related stuff                                                    **********
    // *************************************************************************************************

    public ExtendedTextView createStatusLineETV(ViewGroup vgParent, float weight){
        ExtendedTextView etv = new ExtendedTextView(context).setDrawableSize(dp(16));
        vgParent.addView(etv);
        tvList.add(etv);
        LinearLayout.LayoutParams llParms = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT);
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
        etv.setTextColor(CC.getColor(R.color.CC_BLACK));
        etv.setBackgroundColor(CC.getColor(R.color.CC_WHITE_A150));
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

    public void setStatusLineVisibility(boolean visible){
        tr_states.setVisibility(visible?VISIBLE:INVISIBLE);
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

    // moved to VUtil, since it is also used in other activities


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
        tv.setTextColor(CC.getColor(R.color.CC_WHITE));
        tv.setPadding(dp(16),0,dp(8),0);
        parent.addView(tv);
        return tv;
    }

    public TextView createHelpText3(ViewGroup parent){
        TextView tv = new TextView(parent.getContext());
        Pref<Boolean> prefMenuOneLine = activity.getPrefCache().get(R.string.FSControl_pref_menu_one_line_key, false);
        prefMenuOneLine.addObserver(evt ->
                tv.setLayoutParams(new TableRow.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (prefMenuOneLine.getValue()?1:2) * VUtil.QC_HEIGHT)) );
        prefMenuOneLine.changed();
        tv.setText(" ");
        parent.addView(tv);
        return tv;
    }

}