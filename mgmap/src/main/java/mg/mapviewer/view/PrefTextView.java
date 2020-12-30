package mg.mapviewer.view;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;

import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.util.ArrayUtil;
import mg.mapviewer.util.ExtendedClickListener;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.MGPref;
import mg.mapviewer.util.NameUtil;

public class PrefTextView extends AppCompatTextView  {

    private MGPref<Boolean>[] prefs = new MGPref[]{};
    private int[] drawableIds = new int[]{};
    private Formatter.FormatType formatType = Formatter.FormatType.FORMAT_STRING;
    private int drawableSize = 0;
    private Observer prefObserver = createObserver();
    private MGPref<Boolean> prefEnabled;
    private int drawableIdDisabled;

    public PrefTextView(Context context) {
        this(context, null);
    }

    public PrefTextView(Context context,  AttributeSet attrs) {
        super(context, attrs);
        setDisabledData(new MGPref<Boolean>(UUID.randomUUID().toString(), true, false), 0);
    }

    /**
     * Expect boolean Preferences.
     * @param prefs
     * @param drawableIds
     */
    public PrefTextView setPrefData(MGPref<Boolean>[] prefs, int[] drawableIds) {
        prefs = (prefs==null)?new MGPref[]{}:prefs;
        drawableIds = (drawableIds==null)?new int[]{}:drawableIds;
        this.prefs = ArrayUtil.concat(this.prefs, prefs);
        this.drawableIds = ArrayUtil.concat(this.drawableIds, drawableIds);
        for (MGPref<?> pref : prefs){ // iterate only over the new prefs
            pref.addObserver(prefObserver);
        }
        createOCLs();

        onChange("init");
        return this;
    }

    public PrefTextView setDisabledData(MGPref<Boolean> prefEnabled, int drawableIdDisabled){
        this.prefEnabled = prefEnabled;
        prefEnabled.addObserver(prefObserver);
        this.drawableIdDisabled = drawableIdDisabled;
        onChange("initDis");
        return this;
    }

    private Observer createObserver(){
        return new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                String key = null;
//                Log.d(MGMapApplication.LABEL, NameUtil.context()+" "+prefs.length+" "+o);
                if (o instanceof MGPref<?>) {
                    MGPref<?> mgPref = (MGPref<?>) o;
                    key = mgPref.getKey();
                } else {
                    key = o.toString();
                }
                onChange(key);
            }
        };
    }

//    public void appendPrefData(MGPref<Boolean>[] prefs, int[] drawableIds) {
//        this.prefs = ArrayUtil.concat(this.prefs, prefs);
//        this.drawableIds = ArrayUtil.concat(this.drawableIds, drawableIds);
//        for (MGPref<?> pref : prefs){ // add the prefObserver to the additional prefs
//            pref.addObserver(prefObserver);
//        }
//        createOCLs();
//    }

    public PrefTextView setFormat(Formatter.FormatType formatType){
        this.formatType = formatType;
        return this;
    }

    public int getDrawableSize(){
        return drawableSize;
    }
    public PrefTextView setDrawableSize(int drawableSize){
        this.drawableSize = drawableSize;
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

    private void createOCLs(){
        if (prefs.length >= 1){
            setOnClickListener(new ExtendedClickListener() {
                @Override
                public void onSingleClick(View v) {
                    doubleClickTimeout = (prefs.length<=2)?0:doubleClickTimeout;
                    handleOnClick(v,0);
                }

                @Override
                public void onDoubleClick(View v) {
                    if (prefs.length >= 3){
                        handleOnClick(v,2);
                    }
                }
            });
        }
        if (prefs.length >= 2){
            setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    handleOnClick(v,1);
                    return true;
                }
            });
        }
    }

    protected void handleOnClick(View v, int i){
        if (prefEnabled.getValue() && (i>=0)){
            prefs[i].toggle();
        }
    }

    protected void onChange(String key){

        if (drawableIds.length == 0) {
            setCompoundDrawables(null,null,null,null);
        } else {
            int idx = calcState();
            setDrawable(idx);
        }
    }

    protected int calcState(){
        int idx = 0;
        int cnt = 1;
        if (prefs != null){
            for (MGPref<Boolean> pref : prefs){
                if (pref.getValue()){
                    if (idx + cnt < drawableIds.length){
                        idx += cnt;
                    }
                }
                cnt *= 2;
            }
        }
        return idx;
    }

    protected void setDrawable(int idx){
        int drId = (drawableIds.length>0)?drawableIds[0]:0;
        if ((prefEnabled.getValue()) || (drawableIdDisabled == 0)){
            if (idx < drawableIds.length){
                drId = drawableIds[idx];
            }
        } else {
            drId = drawableIdDisabled;
        }
        Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), drId, getContext().getTheme());
        if (drawable != null){
            drawable.setBounds(0,0,drawableSize,drawableSize);
            setCompoundDrawables(drawable,null,null,null);
        }
    }

    public void setPrefEnabled(boolean enabled){
        prefEnabled.setValue(enabled);
    }

}
