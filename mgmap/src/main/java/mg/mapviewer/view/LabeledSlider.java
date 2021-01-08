package mg.mapviewer.view;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import java.util.Observable;
import java.util.Observer;

import mg.mapviewer.ControlView;
import mg.mapviewer.MGMapApplication;
import mg.mapviewer.R;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.MGPref;

public class LabeledSlider extends LinearLayout {

    Context context;
    MGPref<Float> prefSlider = null;
    MGPref<Boolean> prefSliderVisibility = null;
    private TextView label;
    private SeekBar seekBar;

    public LabeledSlider(Context context) {
        this(context, null);
    }

    public LabeledSlider(Context context,  AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        this.setOrientation(VERTICAL);
        this.setLayoutParams(llParams);

        label = this.createSeekBarLabel(this);
        seekBar = this.createSeekBar(this);
    }

    public LabeledSlider initPrefData(MGPref<Boolean> prefSliderVisibility, MGPref<Float> prefSlider, Integer color, String text){
        label.setText(text);
        this.prefSliderVisibility = prefSliderVisibility;
        this.prefSlider = prefSlider;
        this.prefSlider.addObserver(new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                seekBar.setProgress( (int)(LabeledSlider.this.prefSlider.getValue() * 100));
            }
        });
        prefSlider.onChange();
        seekBar.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefSlider.setValue( progress/100.0f );
                Log.d(MGMapApplication.LABEL, NameUtil.context()+" progress="+progress);
            }
        });

        if (color != null){
            GradientDrawable sd = (GradientDrawable) ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.shape3, getContext().getTheme());
            sd.setTint(color);
            sd.setBounds(0,0,convertDp(25),convertDp(10));
            label.setCompoundDrawables(sd,null,null,null);
            int drawablePadding = convertDp(3.0f);
            label.setCompoundDrawablePadding(drawablePadding);
        }
        return this;
    }

    private TextView createSeekBarLabel(ViewGroup vgParent){
        TextView tv = new TextView(context);
        tv.setPadding(convertDp(10),0,convertDp(10),convertDp(0));
        vgParent.addView(tv);

        int padding = convertDp(2.0f);
        tv.setPadding(padding, padding, padding, 0);
        return tv;
    }

    private SeekBar createSeekBar(ViewGroup vgParent){
        LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        SeekBar sb = new SeekBar(context);
        sb.setLayoutParams(llParams);
        vgParent.addView(sb);
        //sb.setScaleX(1.5f); does scale the thumb to a nice circle, but also scale the seekBar to the full width of the screen
        sb.setScaleY(1.5f);
        sb.setPadding(convertDp(20),0,convertDp(20),convertDp(10));
        return sb;
    }

    private int convertDp(float dp){
        return ControlView.dp(dp);
    }

    public MGPref<Boolean> getPrefSliderVisibility(){
        return prefSliderVisibility;
    }

    @Override
    public String toString() {
        return "LabeledSlider{" +
                "label=" + label.getText() +
                " visibility="+prefSliderVisibility.getValue()+
                " alpha="+prefSlider.getValue()+
                '}';
    }
}
