package mg.mapviewer.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

import mg.mapviewer.ControlView;
import mg.mapviewer.R;
import mg.mapviewer.model.TrackLog;
import mg.mapviewer.model.TrackLogStatistic;
import mg.mapviewer.util.CC;
import mg.mapviewer.util.ExtendedClickListener;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.MGPref;

public class TrackStatisticEntry extends TableLayout {

    private MGPref<Boolean> prefSelected = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);

    private TrackLog trackLog;
    private final MGPref<Boolean> prefShowNameKey = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);


    private Observer modifiedObserver;


    private int dp2px(float dp){
        return ControlView.dp(dp);
    }

    public TrackStatisticEntry(Context context, TrackLog trackLog, ViewGroup parent, int colorId, int colorIdSelected){
        super(context);
        this.trackLog = trackLog;

        TrackLogStatistic statistic = trackLog.getTrackStatistic();

        parent.addView(this);
        this.setId(View.generateViewId());
        this.setPadding(0, dp2px(2),0,0);

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
        prefSelected.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                if (isPrefSelected()){
                    setViewtreeColor(TrackStatisticEntry.this, colorIdSelected);
                } else {
                    setViewtreeColor(TrackStatisticEntry.this, colorId);
                }
            }
        });
        modifiedObserver = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                nameETV.setValue( ((prefShowNameKey.getValue())?trackLog.getNameKey():trackLog.getName()) + (trackLog.isModified()?"*":""));
                durationETV.setValue(statistic.duration);
                lengthETV.setValue(statistic.getTotalLength());
                numPointsETV.setValue(statistic.getNumPoints());
                gainETV.setValue(statistic.getGain());
                lossETV.setValue(statistic.getLoss());
                maxeleETV.setValue(statistic.getMaxEle());
                mineleETV.setValue(statistic.getMinEle());
            }
        };
        trackLog.getPrefModified().addObserver(modifiedObserver);
        prefShowNameKey.addObserver(modifiedObserver);

        modifiedObserver.update(null,null);

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
        ExtendedTextView etv = new ExtendedTextView(getContext()).setDrawableSize(dp2px(16)); // Need activity context for Theme.AppCompat (Otherwise we get error messages)
        viewGroup.addView(etv);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, RelativeLayout.LayoutParams.MATCH_PARENT);
        int margin = dp2px(0.8f);
        params.setMargins(margin,margin,margin,margin);
        params.weight = weight;
        etv.setLayoutParams(params);

        int padding = dp2px(2.0f);
        etv.setPadding(padding, padding, padding, padding);
        int drawablePadding = dp2px(3.0f);
        etv.setCompoundDrawablePadding(drawablePadding);
        Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.quick2, getContext().getTheme());

        drawable.setBounds(0, 0, etv.getDrawableSize(), etv.getDrawableSize());
        etv.setCompoundDrawables(drawable,null,null,null);
        etv.setText("");
        etv.setTextColor(CC.getColor(R.color.WHITE));
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
    public MGPref<Boolean> getPrefSelected() {
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
    public void setModified(boolean modified){
        trackLog.setModified(modified);
    }


    public void onCleanup(){
        trackLog.getPrefModified().deleteObserver(modifiedObserver);
        prefShowNameKey.deleteObserver(modifiedObserver);
    }

}
