/*
 * Copyright 2017 - 2022 mg4gh
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
package mg.mgmap.activity.statistic;

import android.app.Activity;
import android.os.Handler;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.TextViewCompat;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.R;
import mg.mgmap.application.MGMapApplication;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.generic.util.ExtendedClickListener;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.view.ExtendedTextView;

@SuppressLint("ViewConstructor")
public class TrackStatisticView extends TableLayout {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private static final ArrayList<TrackStatisticView> boundViews = new ArrayList<>();
    private final MGMapApplication application;
    public TrackLog trackLog = null;
    private final Observer modifiedObserver;
    private final Observer selectedObserver;

    private final ExtendedTextView etvSelected;
    public final ExtendedTextView etvName;
    private final ExtendedTextView etvSegment;
    private final ExtendedTextView etvDate;
    private final ExtendedTextView etvTime;
    private final ExtendedTextView etvDuration;
    private final ExtendedTextView etvLength;
    private final ExtendedTextView etvPoints;
    private final ExtendedTextView etvGain;
    private final ExtendedTextView etvLoss;
    private final ExtendedTextView etvMaxEle;
    private final ExtendedTextView etvMinEle;

    private int dp(float dp){
        return ControlView.dp(dp);
    }

    static private final Handler timer = new Handler();

    public TrackStatisticView(Context context){
        super(context);
        application = MGMapApplication.getByContext(context);

        this.setId(View.generateViewId());
        this.setPadding(0, dp(2),0,0);
        this.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));

        TableRow tableRow0 = new TableRow(context);
        tableRow0.setLayoutParams(new TableLayout.LayoutParams(-1, -2));

        this.addView(tableRow0);
        etvSelected = createETV(tableRow0,10).setFormat(Formatter.FormatType.FORMAT_STRING).setData(R.drawable.select_off,R.drawable.select_on);
        etvName = createETV(tableRow0,81).setFormat(Formatter.FormatType.FORMAT_STRING);
        etvName.setMaxLines(5);

        TableRow tableRow1 = new TableRow(context);
        tableRow1.setLayoutParams(new TableLayout.LayoutParams(-1, -2));
        this.addView(tableRow1);
        etvSegment = createETV(tableRow1,10).setFormat(Formatter.FormatType.FORMAT_STRING);
        etvDate = createETV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_DATE);
        etvTime = createETV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_TIME);
        etvDuration = createETV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_DURATION).setData(R.drawable.duration);
        etvLength = createETV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_DISTANCE).setData(R.drawable.length);

        TableRow tableRow2 = new TableRow(context);
        tableRow2.setLayoutParams(new TableLayout.LayoutParams(-1, -2));

        this.addView(tableRow2);
        etvPoints = createETV(tableRow2,10).setFormat(Formatter.FormatType.FORMAT_INT);
        TextViewCompat.setAutoSizeTextTypeWithDefaults(etvPoints,   TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM);
        etvGain = createETV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setData(R.drawable.gain);
        etvLoss = createETV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setData(R.drawable.loss);
        etvMaxEle = createETV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setData(R.drawable.maxele);
        etvMinEle = createETV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setData(R.drawable.minele);


        setOnClickListener(new StatisticClickListener());
        modifiedObserver = (e) -> {
            TrackLogStatistic statistic = trackLog.getTrackStatistic();
            etvName.setValue( trackLog.getName() + (trackLog.isModified()?"*":""));
            etvName.invalidate();
            etvDuration.setValue(statistic.getDuration());
            etvLength.setValue(statistic.getTotalLength());
            etvPoints.setValue(statistic.getNumPoints());
            etvGain.setValue(statistic.getGain());
            etvLoss.setValue(statistic.getLoss());
            etvMaxEle.setValue(statistic.getMaxEle());
            etvMinEle.setValue(statistic.getMinEle());
            mgLog.d(trackLog.getName());
        };
        selectedObserver = (e) -> TrackStatisticView.this.setViewtreeColor(TrackStatisticView.this,  getColorIdForTrackLog(trackLog));
    }

    public void bind(TrackLog trackLog) {
        this.trackLog = trackLog;
        mgLog.d("bind_data "+trackLog.getName());
        TrackLogStatistic statistic = trackLog.getTrackStatistic();

        etvSelected.setName("SEL_"+trackLog.getName());
        etvName.setName("NAM_"+trackLog.getName());
        etvSegment.setName("SEG_"+trackLog.getName());
        etvDate.setName("DAT_"+trackLog.getName());
        etvTime.setName("TIM_"+trackLog.getName());
        etvDuration.setName("DUR_"+trackLog.getName());
        etvLength.setName("LEN_"+trackLog.getName());
        etvPoints.setName("PTS_"+trackLog.getName());
        etvGain.setName("GAI_"+trackLog.getName());
        etvLoss.setName("LOS_"+trackLog.getName());
        etvMaxEle.setName("MAX_"+trackLog.getName());
        etvMinEle.setName("MIN_"+trackLog.getName());

        etvSelected.setData(trackLog.getPrefSelected());
        String sIdx = "I="+statistic.getSegmentIdx();
        if (statistic.getSegmentIdx() == -1) sIdx = "All";
        if (statistic.getSegmentIdx() == -2) sIdx = "R";
        etvSegment.setValue( sIdx );
        etvDate.setValue( statistic.getTStart() );
        etvTime.setValue( statistic.getTStart() );

        trackLog.addObserver(modifiedObserver);
        modifiedObserver.propertyChange(null);

        setViewtreeColor(TrackStatisticView.this, getColorIdForTrackLog(trackLog));
        setOnClickListener(new StatisticClickListener());
        trackLog.getPrefSelected().addObserver(selectedObserver);

        boundViews.add(this);
        hack();
    }

    public void unbind(){
        mgLog.i(trackLog.getName());
        clearReferences();
        boundViews.remove(this);
    }

    private void clearReferences(){
        mgLog.i("unbind_data "+trackLog.getName());
        etvSelected.cleanup();
        trackLog.deleteObserver(modifiedObserver);
        trackLog.getPrefSelected().deleteObserver(selectedObserver);
        trackLog = null;
    }

    public class StatisticClickListener extends ExtendedClickListener {
        @Override
        public void onSingleClick(View v) {
            if (trackLog != null){
                trackLog.getPrefSelected().toggle();
            }
        }
    }


    public ExtendedTextView createETV(ViewGroup viewGroup, float weight) {
        ExtendedTextView etv = new ExtendedTextView(getContext()).setDrawableSize(dp(16)); // Need activity context for Theme.AppCompat (Otherwise we get error messages)
        viewGroup.addView(etv);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, RelativeLayout.LayoutParams.MATCH_PARENT);
        int margin = dp(0.8f);
        params.setMargins(margin,margin,margin,margin);
        params.weight = weight;
        etv.setLayoutParams(params);

        int padding = dp(2.0f);
        etv.setPadding(padding, padding, padding, padding);
        int drawablePadding = dp(3.0f);
        etv.setCompoundDrawablePadding(drawablePadding);
        Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.quick2, getContext().getTheme());
        if (drawable != null) drawable.setBounds(0, 0, etv.getDrawableSize(), etv.getDrawableSize());
        etv.setCompoundDrawables(null,null,null,null);
        etv.setText("");
        etv.setTextColor(getContext().getColor(R.color.CC_WHITE));
//        etv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        etv.setMaxLines(1);
        return etv;
    }

    private void setViewtreeColor(View view, int colorId){
        if (view instanceof TextView){
            view.setBackgroundColor(getResources().getColor( colorId, getContext().getTheme()) );
        }

        if (view instanceof ViewGroup viewGroup){
            for (int idx=0; idx<viewGroup.getChildCount(); idx++){
                setViewtreeColor(viewGroup.getChildAt(idx), colorId);
            }
        }

    }


    private int getColorIdForTrackLog(TrackLog trackLog){
        if (trackLog == application.recordingTrackLogObservable.getTrackLog()){
            return trackLog.isSelected()?R.color.CC_RED100_A150 :R.color.CC_RED100_A100;
        }
        if (trackLog == application.routeTrackLogObservable.getTrackLog()){
            return trackLog.isSelected()?R.color.CC_PURPLE_A150 :R.color.CC_PURPLE_A100;
        }
        if (trackLog == application.availableTrackLogsObservable.selectedTrackLogRef.getTrackLog()){
            return trackLog.isSelected()?R.color.CC_BLUE150_A150 :R.color.CC_BLUE100_A100;
        }
        if (application.availableTrackLogsObservable.availableTrackLogs.contains( trackLog )){
            return trackLog.isSelected()?R.color.CC_GREEN150_A150 :R.color.CC_GREEN100_A100;
        }
        if (application.metaTrackLogs.containsValue( trackLog )){
            return trackLog.isSelected()?R.color.CC_GRAY100_A150 :R.color.CC_GRAY100_A100;
        }
        return R.color.CC_BLACK; // should not occur
    }


    @Override
    public void invalidate() {
        super.invalidate();
        modifiedObserver.propertyChange(null);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (trackLog != null){
            hack();
        }
    }

    // For some unknown reason the name field shows initially only one line (and only for those in the first screen).
    // This workaround triggers a refresh for long track names
    private void hack(){
        if ((trackLog != null) && (etvName != null)){
            if (trackLog.getName().length() > 26){
                mgLog.d("hack triggered for "+trackLog.getNameKey());
                timer.postDelayed(() -> ((Activity)getContext()).runOnUiThread(() -> {
                    if (trackLog != null) {
                        mgLog.d("hack executed for "+trackLog.getNameKey());
                        String suffix = etvName.getText().toString().endsWith(" ")?"":" ";
                        etvName.setValue(trackLog.getName() + (trackLog.isModified() ? "*" : "") + suffix);
                    }
                }), 50);
            }
        }
    }

    /* cleanup Views that are bound  */
    public static void cleanup(){
        for (TrackStatisticView trackStatisticView : boundViews){
            trackStatisticView.clearReferences();
        }
        boundViews.clear();
    }
}
