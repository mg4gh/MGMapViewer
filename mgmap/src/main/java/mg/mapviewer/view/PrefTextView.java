package mg.mapviewer.view;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.util.ArrayUtil;
import mg.mapviewer.util.ExtendedClickListener;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.pref.MGPref;

public class PrefTextView extends AppCompatTextView  {

    public PrefTextView(Context context) {
        super(context, null);
    }

    public PrefTextView(Context context,  AttributeSet attrs) {
        super(context, attrs);
    }

    private MGPref<Boolean>[] prefs = null;
    private int[] drawableIds = null;
    private Formatter.FormatType formatType = Formatter.FormatType.FORMAT_STRING;
    private int drawableSize = 0;
    private Observer prefObserver = null;

    /**
     * Expect boolean Preferences.
     * @param prefs
     * @param drawableIds
     */
    public PrefTextView setPrefData(MGPref<Boolean>[] prefs, int[] drawableIds) {
        this.prefs = (prefs==null)?new MGPref[]{}:prefs;
        this.drawableIds = (drawableIds==null)?new int[]{}:drawableIds;

        if (this.prefs.length > 0){
//            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
//            sharedPreferences.registerOnSharedPreferenceChangeListener(this);
            createOCLs();
            prefObserver = createObserver();
            for (MGPref<?> pref : prefs){
                 pref.addObserver(prefObserver);
            }
        }

        onChange("init");
        return this;
    }

    private Observer createObserver(){
        return new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                String key = null;
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

    public void appendPrefData(MGPref<Boolean>[] prefs, int[] drawableIds) {
        this.prefs = ArrayUtil.concat(this.prefs, prefs);
        this.drawableIds = ArrayUtil.concat(this.drawableIds, drawableIds);
        createOCLs();
    }

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
                    prefs[0].toggle();
                }

                @Override
                public void onDoubleClick(View view) {
                    if (prefs.length >= 3){
                        prefs[2].toggle();
                    }
                }
            });
        }
        if (prefs.length >= 2){
            setOnLongClickListener(new OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    prefs[1].toggle();
                    return true;
                }
            });
        }
    }

//    @Override
//    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//        for (MGPref<?> pref : prefs){
//            if (pref.getKey().equals(key)){
//                onChange(key);
//            }
//        }
//    }

    private void onChange(String key){
        if (drawableIds.length == 0) {
            setCompoundDrawables(null,null,null,null);
        } else {
            int idx = 0;
            int cnt = 1;
            if (prefs != null){
                for (MGPref<Boolean> pref : prefs){
                    if (pref.getValue()){
                        idx += cnt;
                    }
                    cnt *= 2;
                }
            }

            if (idx<drawableIds.length){
                Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), drawableIds[idx], getContext().getTheme());
                if (drawable != null){
                    drawable.setBounds(0,0,drawableSize,drawableSize);
                    setCompoundDrawables(drawable,null,null,null);
                }
            }
        }
    }

}
