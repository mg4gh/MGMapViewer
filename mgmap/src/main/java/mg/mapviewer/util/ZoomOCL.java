package mg.mapviewer.util;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.TimerTask;

import mg.mapviewer.MGMapApplication;

public class ZoomOCL extends ExtendedClickListener {
//public class ZoomOCL implements View.OnClickListener {

    public ZoomOCL(float scale){
        this.scale = scale;
    }

    private float scale;
    private static Handler timer = new Handler();
    TextView tv;
    private int toMillis = 1500;

    final TimerTask ttResetTextSize = new TimerTask() {
        @Override
        public void run() {
//            float scale = mapView.getModel().displayModel.getScaleFactor();
            float size = tv.getTextSize();
            Log.i(MGMapApplication.LABEL, NameUtil.context() + "size="+size);

            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX,  size-10*scale);
            scaleBoundsForDrawable(tv,(int)(-10*scale));
            ((LinearLayout.LayoutParams)tv.getLayoutParams()).weight -= 5*scale;
        }
    };
    @Override
    public void onSingleClick(View v) {
        if (v instanceof TextView){
            this.tv = (TextView)v;
//            float scale = mapView.getModel().displayModel.getScaleFactor();
            float size = tv.getTextSize();
            Log.i(MGMapApplication.LABEL, NameUtil.context() + "size="+size);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX,  size+10*scale );
            scaleBoundsForDrawable(tv,(int)(10*scale));
            ((LinearLayout.LayoutParams)tv.getLayoutParams()).weight += 5*scale;
            timer.postDelayed(ttResetTextSize,toMillis);
        }
    }

    private void scaleBoundsForDrawable(TextView tv, int diff){
        Drawable drawable = tv.getCompoundDrawables()[0]; // index 0 is the left
        if (drawable != null){
            Rect bounds = drawable.getBounds();
            bounds.right += diff;
            bounds.bottom += diff;
            drawable.setBounds(bounds);
            tv.setCompoundDrawables(drawable,null,null,null);
        }
    }

    public void setToMillis(int millis){
        toMillis = millis;
    }

}
