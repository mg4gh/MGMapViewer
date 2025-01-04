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
package mg.mgmap.activity.mgmap.view;

import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.lang.invoke.MethodHandles;

import mg.mgmap.activity.mgmap.ControlView;
import mg.mgmap.R;
import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.Pref;

public class LabeledSlider extends LinearLayout {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    final Context context;
    Pref<Float> prefSlider = null;
    Pref<Boolean> prefSliderVisibility = null;
    private final TextView label;
    private final SeekBar seekBar;
    private Observer prefSliderObserver;

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

    @SuppressWarnings("UnusedReturnValue")
    public LabeledSlider initPrefData(Pref<Boolean> prefSliderVisibility, Pref<Float> prefSlider, Integer color, String text){
        mgLog.d("prefSlider="+prefSlider+" text="+text);
        this.prefSliderVisibility = prefSliderVisibility;
        this.prefSlider = prefSlider;
        if (prefSlider != null){
            label.setText(text);
            if (prefSliderObserver != null){
                this.prefSlider.deleteObserver(prefSliderObserver);// remove, if exists
            }
            prefSliderObserver = evt -> seekBar.setProgress( (int)(LabeledSlider.this.prefSlider.getValue() * 100));
            this.prefSlider.addObserver(prefSliderObserver); // add anyway
            prefSlider.onChange();
            seekBar.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    prefSlider.setValue( progress/100.0f );
                    mgLog.d(" progress="+progress);
                }
            });

            if (color != null){
                GradientDrawable sd = (GradientDrawable) ResourcesCompat.getDrawable(getContext().getResources(), R.drawable.shape3, getContext().getTheme());
                if (sd != null){
                    sd.setTint(color);
                    sd.setBounds(0,0,convertDp(25),convertDp(10));
                    label.setCompoundDrawables(sd,null,null,null);
                    int drawablePadding = convertDp(3.0f);
                    label.setCompoundDrawablePadding(drawablePadding);
                }
            }
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

    public Pref<Boolean> getPrefSliderVisibility(){
        return prefSliderVisibility;
    }

    public Pref<Float> getPrefSlider() {
        return prefSlider;
    }

    @NonNull
    @Override
    public String toString() {
        return "LabeledSlider{" +
                "label=" + ((label==null)?"":label.getText()) +
                " visibility="+((prefSliderVisibility==null)?"":prefSliderVisibility.getValue())+
                " alpha="+((prefSlider==null)?"":prefSlider.getValue())+
                '}';
    }

    public Point getThumbPos(int progress){
        int[] loc = new int[2];
        seekBar.getLocationOnScreen(loc);
        return new Point(loc[0] + convertDp(20) + ( (seekBar.getWidth()-convertDp(40))*progress)/100, loc[1] +convertDp(10));
    }
}
