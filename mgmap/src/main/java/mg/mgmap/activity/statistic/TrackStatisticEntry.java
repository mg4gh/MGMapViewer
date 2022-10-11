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
package mg.mgmap.activity.statistic;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import java.util.Observer;

import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.R;
import mg.mgmap.generic.model.TrackLog;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.generic.util.ExtendedClickListener;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.view.ExtendedTextView;

@SuppressLint("ViewConstructor")
public class TrackStatisticEntry extends TableLayout {

    private final Pref<Boolean> prefSelected = new Pref<>(false);

    private final Context context;
    private final TrackLog trackLog;
    private final int colorId;
    private final int colorIdSelected;

    private boolean initialized = false;
    private final Pref<Boolean> prefShowNameKey = new Pref<>(false);

    private Observer modifiedObserver;


    private int dp(float dp){
        return ControlView.dp(dp);
    }

    public TrackStatisticEntry(Context context, TrackLog trackLog, int colorId, int colorIdSelected){
        super(context);
        this.context = context;
        this.trackLog = trackLog;
        this.colorId = colorId;
        this.colorIdSelected = colorIdSelected;
    }

    TrackStatisticEntry initialize(){
        if (!initialized){
            TrackLogStatistic statistic = trackLog.getTrackStatistic();

            this.setId(View.generateViewId());
            this.setPadding(0, dp(2),0,0);

            TableRow tableRow0 = new TableRow(context);
            this.addView(tableRow0);
            createETV(tableRow0,10).setFormat(Formatter.FormatType.FORMAT_STRING).setData(prefSelected,R.drawable.select_off,R.drawable.select_on);
            ExtendedTextView nameETV = createETV(tableRow0,81).setFormat(Formatter.FormatType.FORMAT_STRING);
            nameETV.setMaxLines(5);

            TableRow tableRow1 = new TableRow(context);
            this.addView(tableRow1);
            String sIdx = "I="+statistic.getSegmentIdx();
            if (statistic.getSegmentIdx() == -1) sIdx = "All";
            if (statistic.getSegmentIdx() == -2) sIdx = "R";
            createETV(tableRow1,10).setFormat(Formatter.FormatType.FORMAT_STRING).setValue( sIdx );
            createETV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_DATE).setValue( statistic.getTStart() );
            createETV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_TIME).setValue( statistic.getTStart() );
            ExtendedTextView durationETV = createETV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_DURATION).setData(R.drawable.duration);
            ExtendedTextView lengthETV = createETV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_DISTANCE).setData(R.drawable.length);

            TableRow tableRow2 = new TableRow(context);
            this.addView(tableRow2);
            ExtendedTextView numPointsETV = createETV(tableRow2,10).setFormat(Formatter.FormatType.FORMAT_INT);
            ExtendedTextView gainETV = createETV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setData(R.drawable.gain);
            ExtendedTextView lossETV = createETV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setData(R.drawable.loss);
            ExtendedTextView maxeleETV = createETV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setData(R.drawable.maxele);
            ExtendedTextView mineleETV = createETV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setData(R.drawable.minele);


            setViewtreeColor(this, colorId);
            setOnClickListener(new StatisticClickListener());
            prefSelected.addObserver((o, arg) -> setViewtreeColor(TrackStatisticEntry.this, isPrefSelected()?colorIdSelected:colorId));
            modifiedObserver = (o, arg) -> {
                nameETV.setValue( ((prefShowNameKey.getValue())?trackLog.getNameKey():trackLog.getName()) + (trackLog.isModified()?"*":""));
                durationETV.setValue(statistic.getDuration());
                lengthETV.setValue(statistic.getTotalLength());
                numPointsETV.setValue(statistic.getNumPoints());
                gainETV.setValue(statistic.getGain());
                lossETV.setValue(statistic.getLoss());
                maxeleETV.setValue(statistic.getMaxEle());
                mineleETV.setValue(statistic.getMinEle());
            };
            trackLog.addObserver(modifiedObserver);
            prefShowNameKey.addObserver(modifiedObserver);

            modifiedObserver.update(null,null);
            initialized = true;
        }
        return this;
    }


    public class StatisticClickListener extends ExtendedClickListener {
        @Override
        public void onSingleClick(View v) {
            prefSelected.toggle();
        }

        @Override
        public void onDoubleClick(View v) {
            prefShowNameKey.toggle();
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
        etv.setCompoundDrawables(drawable,null,null,null);
        etv.setText("");
        etv.setTextColor(getContext().getColor(R.color.WHITE));
        etv.setLines(1);
        return etv;
    }

    private void setViewtreeColor(View view, int colorId){
        if (view instanceof TextView){
            view.setBackgroundColor(getResources().getColor( colorId, getContext().getTheme()) );
        }

        if (view instanceof ViewGroup){
            ViewGroup viewGroup = (ViewGroup)view;
            for (int idx=0; idx<viewGroup.getChildCount(); idx++){
                setViewtreeColor(viewGroup.getChildAt(idx), colorId);
            }
        }

    }

    public boolean isPrefSelected() {
        return prefSelected.getValue();
    }
    public Pref<Boolean> getPrefSelected() {
        return prefSelected;
    }

    public void setPrefSelected(boolean selected) {
        this.prefSelected.setValue(selected);
    }

    public TrackLog getTrackLog() {
        return trackLog;
    }

    public boolean isModified(){
        return trackLog.isModified();
    }

    public void onCleanup(){
        trackLog.deleteObserver(modifiedObserver);
        prefShowNameKey.deleteObserver(modifiedObserver);
    }

    @Override
    public void invalidate() {
        super.invalidate();
        modifiedObserver.update(null,null);
    }
}
