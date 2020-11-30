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

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.util.ArrayUtil;
import mg.mapviewer.util.ExtendedClickListener;
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
    private int[] textIds = null;
    private int[] drawableIds = null;

    /**
     * Expect boolean Preferences.
     * @param prefs
     * @param textIds
     * @param drawableIds
     */
    public void setPrefData(MGPref<Boolean>[] prefs, int[] textIds, int[] drawableIds) {
        this.prefs = prefs;
        this.textIds = textIds;
        this.drawableIds = drawableIds;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
        createOCLs();
        onChange("init");
    }

    public void appendPrefData(MGPref<Boolean>[] prefs, int[] textIds, int[] drawableIds) {
        this.prefs = ArrayUtil.concat(this.prefs, prefs);
        this.textIds = ArrayUtil.concat(this.textIds, textIds);
        this.drawableIds = ArrayUtil.concat(this.drawableIds, drawableIds);
        createOCLs();
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
        Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+key+" "+getId()+s);
        int idx = 0;
        int cnt = 1;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        for (MGPref<Boolean> pref : prefs){
            if (pref.getValue()){
                idx += cnt;
            }
            cnt *= 2;
        }
        setText((idx<textIds.length )?getResources().getString(textIds[idx]):"");

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
