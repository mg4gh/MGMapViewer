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
import mg.mapviewer.util.NameUtil;
import mg.mapviewer.util.pref.MGPref;

public class PrefTextView extends AppCompatTextView implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final SimpleDateFormat sdf2 = new SimpleDateFormat(" HH:mm", Locale.GERMANY);
    public enum FormatType {FORMAT_TIME, FORMAT_DISTANCE, FORMAT_DURATION, FORMAT_DAY, FORMAT_INT, FORMAT_HEIGHT};

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
    private FormatType formatType = null;

    /**
     * Expect boolean Preferences.
     * @param prefs
     * @param drawableIds
     */
    public void setPrefData(MGPref<Boolean>[] prefs, int[] drawableIds) {
        this.prefs = prefs;
        this.drawableIds = drawableIds;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        createOCLs();
        onChange("init");
    }

    public void appendPrefData(MGPref<Boolean>[] prefs, int[] drawableIds) {
        this.prefs = ArrayUtil.concat(this.prefs, prefs);
        this.drawableIds = ArrayUtil.concat(this.drawableIds, drawableIds);
        createOCLs();
    }

    public void setFormat(FormatType formatType){
        this.formatType = formatType;
    }

    public void setValue(Object value){
        String text = "";
        if (formatType == FormatType.FORMAT_TIME){
            long millis = (Long)value;
            if (millis > 0){
                text = sdf2.format(millis);
            }
        } else if (formatType == FormatType.FORMAT_INT){
            int iValue = (Integer)value;
            text = Integer.toString(iValue);
        } else if (formatType == FormatType.FORMAT_DISTANCE){
            double distance = (Double)value;
            text = (distance==0)?"":String.format(Locale.ENGLISH, " %.2f km", distance / 1000.0);
        } else if (formatType == FormatType.FORMAT_HEIGHT){
            float height  = (Float)value;
            text = (height == PointModel.NO_ELE)?"":String.format(Locale.ENGLISH," %.1f m",height);
        }

        setText(text);
        onChange("onSetValue");
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
        String s =  "";
        for (MGPref<?> pref : prefs){
            s += " "+pref.toString();
        }
//        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+key+" "+getId()+s);
        int idx = 0;
        int cnt = 1;
        for (MGPref<Boolean> pref : prefs){
            if (pref.getValue()){
                idx += cnt;
            }
            cnt *= 2;
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