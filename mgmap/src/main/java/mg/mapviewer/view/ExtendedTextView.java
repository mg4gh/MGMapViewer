package mg.mapviewer.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

import java.util.Observable;
import java.util.Observer;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.util.NameUtil;

public class ExtendedTextView extends AppCompatTextView {

    private MGPref<Boolean> prState1=null, prState2=null;
    private MGPref<Boolean> prAction1=null, prAction2=null;
    private MGPref<Boolean> prEnabled=null;

    private int drId1=0,drId2=0,drId3=0,drId4=0;
    private int drIdDis=0;

    private Formatter.FormatType formatType = Formatter.FormatType.FORMAT_STRING;
    private int drawableSize = 0;


    private final Observer prefObserver = createObserver();

    public ExtendedTextView(Context context) {
        this(context, null);
    }

    public ExtendedTextView(Context context,  AttributeSet attrs) {
        super(context, attrs);
    }

    public ExtendedTextView setData(int drId){
        drId1 = drId;
        onChange("setData0");
        return this;
    }
    public ExtendedTextView setData(MGPref<Boolean> prState1, int drId1, int drId2){
        this.prState1 = prState1;
        prState1.addObserver(prefObserver);
        this.drId1 = drId1;
        this.drId2 = drId2;
        onChange("setData1");
        return this;
    }
    public ExtendedTextView setData(MGPref<Boolean> prState1, MGPref<Boolean> prState2, int drId1, int drId2, int drId3, int drId4){
        this.prState1 = prState1;
        prState1.addObserver(prefObserver);
        this.prState2 = prState2;
        prState2.addObserver(prefObserver);
        this.drId1 = drId1;
        this.drId2 = drId2;
        this.drId3 = drId3;
        this.drId4 = drId4;
        onChange("setData2");
        return this;
    }
    public ExtendedTextView setPrAction(MGPref<Boolean> prAction){
        this.prAction1 = prAction;
        this.setOnClickListener(prAction);
        return this;
    }
    public ExtendedTextView setPrAction(MGPref<Boolean> prAction1,MGPref<Boolean> prAction2){
        this.prAction1 = prAction1;
        this.setOnClickListener(prAction1);
        this.prAction2 = prAction2;
        this.setOnLongClickListener(prAction2);
        return this;
    }
    public ExtendedTextView setDisabledData(MGPref<Boolean> prEnabled, int drIdDis){
        this.prEnabled = prEnabled;
        prEnabled.addObserver(prefObserver);
        this.drIdDis = drIdDis;
        setEnabled();
        onChange("setDisabledData");
        return this;
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

    private Observer createObserver(){
        return new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                String info;
                if (o instanceof MGPref<?>) {
                    MGPref<?> mgPref = (MGPref<?>) o;
                    info = "key=\""+mgPref.getKey()+"\" value=\""+mgPref.getValue()+"\"";
                } else {
                    info = o.toString();
                }
                if (o == prEnabled){
                    setEnabled();
                }
                onChange(info);
            }
        };
    }

    private void setEnabled(){
        setEnabled((prEnabled!=null)?prEnabled.getValue():true);
    }

    public ExtendedTextView setFormat(Formatter.FormatType formatType){
        this.formatType = formatType;
        return this;
    }
    public boolean setValue(Object value){
        String newText = Formatter.format(formatType, value);
        if (!newText.equals(getText())){
            setText( newText );
            onChange("onSetValue");
            return true;
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
        Log.v(MGMapApplication.LABEL, NameUtil.context()+" "+reason);
        int drId = 0;
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
            setCompoundDrawables(null,null,null,null);
        } else {
            Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), drId, getContext().getTheme());
            if (drawable != null){
                drawable.setBounds(0,0,drawableSize,drawableSize);
                setCompoundDrawables(drawable,null,null,null);
            }
        }
    }


    public void onDestroy(){
        if (prState1 != null) prState1.deleteObserver(prefObserver);
        if (prState2 != null) prState2.deleteObserver(prefObserver);
        if (prEnabled != null) prEnabled.deleteObserver(prefObserver);
    }
}
