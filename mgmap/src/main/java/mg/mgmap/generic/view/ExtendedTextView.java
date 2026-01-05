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
package mg.mgmap.generic.view;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Looper;
import android.text.TextPaint;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

import java.lang.invoke.MethodHandles;

import mg.mgmap.generic.util.Observer;
import mg.mgmap.generic.util.basic.Formatter;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.basic.MGLog;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ExtendedTextView extends AppCompatTextView {

    private static final MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private Pref<Boolean> prState1=null, prState2=null;
    private Pref<Boolean> prAction1=null, prAction2=null;
    private Pref<Boolean> prEnabled=null;

    private int drId1=0,drId2=0,drId3=0,drId4=0;
    private int drIdDis=0;
    private int currentDrId = 0;

    private Formatter.FormatType formatType = Formatter.FormatType.FORMAT_STRING;
    private int drawableSize = 0;
    private String help = "";
    private String help1 = null, help2 = null, help3 = null, help4 = null;
    private String logName = "";

    int[] loc = new int[2];
    private Object value = null;
    private int availableWidth = 0;
    private String availableText = null;
    Observer pclPrState1 = null;
    Observer pclPrState2 = null;
    Observer pclPrEnabled = null;


    public ExtendedTextView(Context context) {
        this(context, null);
    }

    public ExtendedTextView(Context context,  AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        try {
            int newAvailableWidth = getWidth() - getPaddingLeft() - getPaddingRight() - ((getCompoundDrawables()[0] != null) ? (getCompoundDrawablePadding() + getDrawableSize()) : 0);
            if (newAvailableWidth != availableWidth){
                availableWidth = newAvailableWidth;
                mgLog.v(getLogText()+" - "+" available=" + availableWidth);
                availableText = null; // force recalc text
                setValue(value);
            }
        } catch (Exception e) {
            mgLog.e(e);
        }
        mgLog.v(()-> getLogText() + " - " + " w=" + w + " h=" + h + " oldw=" + oldw + " oldh=" + oldh);
    }

    public ExtendedTextView setName(String logName){
        this.logName = logName;
        return this;
    }

    public ExtendedTextView setNameAndId(int id) {
        super.setId(id);
        logName = getResources().getResourceName(id).replaceFirst(".*/","");
        return this;
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        String rName;
        try {
            rName = getResources().getResourceName(id);
        } catch (Resources.NotFoundException e){
            rName = "res_"+id;
        }
        logName = rName.replaceFirst(".*/","");
    }

    public void setIdOnly(int id){
        super.setId(id);
        logName = "res_"+id;
    }

    public ExtendedTextView setData(int drId){
        drId1 = drId;
        onChange(null);
        return this;
    }
    public ExtendedTextView setData(int drId1, int drId2){
        this.drId1 = drId1;
        this.drId2 = drId2;
        onChange(null);
        return this;
    }
    public ExtendedTextView setData(Pref<Boolean> prState1){
        return setData(prState1, drId1, drId2);
    }

    public ExtendedTextView setData(Pref<Boolean> prState1, int drId1, int drId2){
        return setData(prState1, prState2, drId1, drId2, drId3, drId4);
    }

    public ExtendedTextView setData(Pref<Boolean> prState1, Pref<Boolean> prState2, int drId1, int drId2, int drId3, int drId4){
        if (prState1 != null){
            this.prState1 = prState1;
            pclPrState1 = (e)->onChange(prState1.getKey());
            prState1.addObserver(pclPrState1);
        }
        if (prState2 != null){
            this.prState2 = prState2;
            pclPrState2 = (e)->onChange(prState2.getKey());
            prState2.addObserver(pclPrState2);
        }
        this.drId1 = drId1;
        this.drId2 = drId2;
        this.drId3 = drId3;
        this.drId4 = drId4;
        onChange(null);
        return this;
    }
    public ExtendedTextView setPrAction(Pref<Boolean> prAction){
        return setPrAction(prAction, null);
    }
    public ExtendedTextView setPrAction(Pref<Boolean> prAction1, Pref<Boolean> prAction2){
        this.prAction1 = prAction1;
        if (prAction1 != null){
            this.setOnClickListener(v -> {
                mgLog.d("onClick "+logName);
                prAction1.toggle();
            });
        }
        this.prAction2 = prAction2;
        if (prAction2 != null){
            this.setOnLongClickListener(v -> {
                mgLog.d("onClick "+logName);
                prAction2.toggle();
                return true;
            });
        }
        return this;
    }
    public ExtendedTextView setDisabledData(Pref<Boolean> prEnabled, int drIdDis){
        this.prEnabled = prEnabled;
        pclPrEnabled = (e)->{ setEnabled(); onChange(prEnabled.getKey()); };
        prEnabled.addObserver(pclPrEnabled);
        this.drIdDis = drIdDis;
        setEnabled();
        onChange("setDisabledData");
        return this;
    }
    public ExtendedTextView setHelp(String help){
        this.help = help;
        return this;
    }
    public ExtendedTextView setHelp(String help1,String help2){
        this.help1 = help1;
        this.help2 = help2;
        return this;
    }
    public ExtendedTextView setHelp(String help1,String help2,String help3,String help4){
        this.help1 = help1;
        this.help2 = help2;
        this.help3 = help4;
        this.help4 = help3;
        return this;
    }
    public String getHelp(){
        String line2 = "";
        if ((prEnabled!=null) && (!prEnabled.getValue())){
            line2 = System.lineSeparator()+"disabled";
        } else {
            String res = null;
            if ((prState1 != null) && (prState2 == null)){
                res = prState1.getValue()?help2:help1;
            }
            if ((prState1 != null) && (prState2 != null)){
                if (prState2.getValue()){
                    res = prState1.getValue()?help4:help3;
                } else {
                    res = prState1.getValue()?help2:help1;
                }
            }
            if ((res!=null) && (!res.isEmpty())){
                line2 = System.lineSeparator()+res;
            }
        }
        return help + line2;
    }
    public ExtendedTextView addActionObserver(Observer action1Observer){
        if ((prAction1!=null) && (action1Observer!=null)) prAction1.addObserver(action1Observer);
        return this;
    }
    public ExtendedTextView addActionObserver(Observer action1Observer,Observer action2Observer){
        if ((prAction1!=null) && (action1Observer!=null)) prAction1.addObserver(action1Observer);
        if ((prAction2!=null) && (action2Observer!=null)) prAction2.addObserver(action2Observer);
        return this;
    }

    private void setEnabled(){
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            setEnabled((prEnabled!=null)?prEnabled.getValue():true);
        } else {
            mgLog.d("need UIThread: "+logName+ " context="+getContext().getClass().getSimpleName());
            Activity activity = (Activity) this.getContext();
            activity.runOnUiThread(this::setEnabled);
        }
    }

    public ExtendedTextView setFormat(Formatter.FormatType formatType){
        this.formatType = formatType;
        return this;
    }
    public boolean setValue(Object value){
        this.value = value;
        TextPaint paint = getPaint();
        if ((value!=null) && (availableWidth>0) && (paint!=null)){
            String newText = Formatter.format(formatType, value, getPaint() /*availablePaint*/, availableWidth*getMaxLines());
            if (!newText.equals(availableText)){
                mgLog.d(logName+":"+newText /*+ " availableWidth="+availableWidth +" textSize="+getTextSize()*/);
                if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                    setText( newText );
                } else {
                    mgLog.d("TODO: check need UIThread: "+logName+ " context="+getContext().getClass().getSimpleName());
                    Activity activity = (Activity) this.getContext();
                    activity.runOnUiThread(() -> setText( newText ));
                }
                availableText = newText;
                onChange("onSetValue: "+newText);
                return true;
            }
        }
        return false;
    }

    public int getDrawableSize(){
        return drawableSize;
    }
    public ExtendedTextView setDrawableSize(int drawableSize){
        this.drawableSize = drawableSize;
        return this;
    }

    private void onChange(String reason){
        if (getParent() == null) return;
        if (reason != null) {
            mgLog.v(" n="+logName+" "+((prState1==null)?"":prState1.toString())+" "+((prState2==null)?"":prState2.toString())+" reason="+reason);
        }
        int drId;
        if ((prEnabled != null) && (!prEnabled.getValue())){
            drId = drIdDis;
        } else { // not disabled
            if ((prState1 != null) && (prState2 != null)){
                if (prState2.getValue()){
                    drId = prState1.getValue()?drId4:drId3;
                } else {
                    drId = prState1.getValue()?drId2:drId1;
                }
            } else if (prState1 != null){
                drId = prState1.getValue()?drId2:drId1;
            } else {
                drId = drId1;
            }
        }
        if (drId == 0){
            onDrawableChanged(getCompoundDrawables()[0], null);
            setCompoundDrawables(null,null,null,null);
        } else {
            if (drId != currentDrId){
                currentDrId = drId;
                Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), drId, getContext().getTheme());
                if (drawable != null){
                    drawable.setBounds(0,0,drawableSize,drawableSize);
                    if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                        onDrawableChanged(getCompoundDrawables()[0], drawable);
                        setCompoundDrawables(drawable, null, null, null);
                    } else {
                        mgLog.d("need UIThread: "+logName+ " context="+getContext().getClass().getSimpleName());
                        onDrawableChanged(getCompoundDrawables()[0], drawable);
                        Activity activity = (Activity) this.getContext();
                        activity.runOnUiThread(() -> setCompoundDrawables(drawable, null, null, null));
                    }
                }
//            } else {
//                mgLog.d("unchanged");
            }
        }
    }

    protected void onDrawableChanged(Drawable oldDrawable, Drawable newDrawable){}

    public void cleanup(){
        if ((pclPrState1 != null)  && (prState1 != null))  prState1.deleteObserver(pclPrState1);
        if ((pclPrState2 != null)  && (prState2 != null))  prState2.deleteObserver(pclPrState2);
        if ((pclPrEnabled != null) && (prEnabled != null)) prEnabled.deleteObserver(pclPrEnabled);
    }

    public String getLogText(){
        return logName + ":" + getText();
    }

    public String getLogName(){
        return logName;
    }

}
