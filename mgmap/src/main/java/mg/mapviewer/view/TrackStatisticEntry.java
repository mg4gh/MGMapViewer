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
import mg.mapviewer.util.pref.MGPref;

public class TrackStatisticEntry extends TableLayout {

    private MGPref<Boolean> prefSelected = new MGPref<Boolean>(UUID.randomUUID().toString(), false, false);

    private TrackLog trackLog;
    private PrefTextView namePTV;

    private Observer modifiedObserver;


    private int dp2px(float dp){
        return ControlView.convertDp(dp);
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
        createPTV(tableRow0,10).setFormat(Formatter.FormatType.FORMAT_STRING).setPrefData(new MGPref[]{prefSelected},new int[]{R.drawable.select_off,R.drawable.select_on});
        namePTV = createPTV(tableRow0,81).setFormat(Formatter.FormatType.FORMAT_STRING).setPrefData(null,null);
        refreshNamePTV();
        namePTV.setMaxLines(5);

        TableRow tableRow1 = new TableRow(context);
        this.addView(tableRow1);
        createPTV(tableRow1,10).setFormat(Formatter.FormatType.FORMAT_STRING).setPrefData(null,null).setValue( (statistic.getSegmentIdx()<0)?"All":""+statistic.getSegmentIdx() );
        createPTV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_DATE).setPrefData(null,null).setValue( statistic.getTStart() );
        createPTV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_TIME).setPrefData(null,null).setValue( statistic.getTStart() );
        createPTV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_DURATION).setPrefData(null, new int[]{R.drawable.duration}).setValue( statistic.duration );
        createPTV(tableRow1,20).setFormat(Formatter.FormatType.FORMAT_DISTANCE).setPrefData(null, new int[]{R.drawable.length}).setValue( statistic.getTotalLength() );

        TableRow tableRow2 = new TableRow(context);
        this.addView(tableRow2);
        createPTV(tableRow2,10).setFormat(Formatter.FormatType.FORMAT_INT).setPrefData(null,null).setValue( statistic.getNumPoints() );
        createPTV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setPrefData(null, new int[]{R.drawable.gain}).setValue( statistic.getGain() );
        createPTV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setPrefData(null, new int[]{R.drawable.loss}).setValue( statistic.getLoss() );
        createPTV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setPrefData(null, new int[]{R.drawable.maxele}).setValue( statistic.getMaxEle() );
        createPTV(tableRow2,20).setFormat(Formatter.FormatType.FORMAT_HEIGHT).setPrefData(null, new int[]{R.drawable.minele}).setValue( statistic.getMinEle() );


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
                refreshNamePTV();
            }
        };
        trackLog.getPrefModified().addObserver(modifiedObserver);
    }

    private void refreshNamePTV(){
        namePTV.setValue(trackLog.getName()+(trackLog.isModified()?"*":""));
    }


    public class StatisticClickListener extends ExtendedClickListener {
        private boolean showNameKey = false;

        @Override
        public void onSingleClick(View v) {
            setPrefSelected(!isPrefSelected());
        }

        @Override
        public void onDoubleClick(View v) {
            showNameKey = !showNameKey;
            if (showNameKey){
                namePTV.setText(trackLog.getNameKey());
            } else {
                namePTV.setText(trackLog.getName());
            }
            super.onDoubleClick(v);
        }

    }




    public PrefTextView createPTV(ViewGroup viewGroup, float weight) {
        PrefTextView ptv = new PrefTextView(getContext()).setDrawableSize(dp2px(16)); // Need activity context for Theme.AppCompat (Otherwise we get error messages)
        viewGroup.addView(ptv);

        TableRow.LayoutParams params = new TableRow.LayoutParams(0, RelativeLayout.LayoutParams.MATCH_PARENT);
        int margin = dp2px(0.8f);
        params.setMargins(margin,margin,margin,margin);
        params.weight = weight;
        ptv.setLayoutParams(params);

        int padding = dp2px(2.0f);
        ptv.setPadding(padding, padding, padding, padding);
        int drawablePadding = dp2px(3.0f);
        ptv.setCompoundDrawablePadding(drawablePadding);
        Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.quick2, getContext().getTheme());

        drawable.setBounds(0, 0, ptv.getDrawableSize(), ptv.getDrawableSize());
        ptv.setCompoundDrawables(drawable,null,null,null);
        ptv.setText("");
        ptv.setTextColor(CC.getColor(R.color.WHITE));
        ptv.setLines(1);
        return ptv;
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
    }

}
