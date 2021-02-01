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
package mg.mgmap.util;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.TimerTask;

import mg.mgmap.MGMapApplication;

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
