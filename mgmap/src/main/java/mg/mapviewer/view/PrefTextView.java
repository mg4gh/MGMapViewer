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

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.model.PointModel;
import mg.mapviewer.util.ArrayUtil;
import mg.mapviewer.util.ExtendedClickListener;
import mg.mapviewer.util.Formatter;
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.pref.MGPref;

public class PrefTextView extends AppCompatTextView implements SharedPreferences.OnSharedPreferenceChangeListener {


    private static Handler timer = new Handler();

    Runnable ttControlRefresh = new Runnable() {
        @Override
        public void run() {
            onChange("refresh");
        }
    };

    protected void resetTTControlRefresh(){
        timer.removeCallbacks(ttControlRefresh);
        timer.postDelayed(ttControlRefresh, 100);
    }

    public PrefTextView(Context context) {
        super(context, null);
    }

    public PrefTextView(Context context,  AttributeSet attrs) {
        super(context, attrs);
    }

    private MGPref<Boolean>[] prefs = null;
    private int[] drawableIds = null;
    private Formatter.FormatType formatType = Formatter.FormatType.FORMAT_STRING;

    /**
     * Expect boolean Preferences.
     * @param prefs
     * @param drawableIds
     */
    public PrefTextView setPrefData(MGPref<Boolean>[] prefs, int[] drawableIds) {
        this.prefs = (prefs==null)?new MGPref[]{}:prefs;
        this.drawableIds = (drawableIds==null)?new int[]{}:drawableIds;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        createOCLs();
        onChange("init");
        return this;
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        for (MGPref<?> pref : prefs){
            if (pref.getKey().equals(key)){
                onChange(key);
            }
        }
    }

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

            Drawable dOld = getCompoundDrawables()[0];
            if (dOld != null) {
                if (idx<drawableIds.length){
                    Rect rect = getCompoundDrawables()[0].getBounds();
                    Drawable drawable = ResourcesCompat.getDrawable(getContext().getResources(), drawableIds[idx], getContext().getTheme());
                    if ((drawable != null) && (rect != null)){
                        drawable.setBounds(rect.left,rect.top,rect.right,rect.bottom);
                        setCompoundDrawables(drawable,null,null,null);
                    }
                }
            } else {
                resetTTControlRefresh();
            }
        }
    }

}
