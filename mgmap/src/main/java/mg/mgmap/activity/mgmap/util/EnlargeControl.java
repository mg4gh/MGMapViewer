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
package mg.mgmap.activity.mgmap.util;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;

import org.mapsforge.map.model.DisplayModel;

import java.util.Objects;

import mg.mgmap.generic.util.ExtendedClickListener;

public class EnlargeControl extends ExtendedClickListener {

    private final TextView tvEnlarge;

    private final Handler timer = new Handler();
    private final Runnable ttHide = new Runnable() {
        @Override
        public void run() {
            tvEnlarge.setVisibility(View.INVISIBLE);
        }
    };

    public EnlargeControl(TextView tvEnlarge){
        this.tvEnlarge = tvEnlarge;
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);

        timer.removeCallbacks(ttHide); // cancel old timer if exists
        timer.postDelayed(ttHide, 1500);

        if (v instanceof TextView tv) {
            String text = " "+tv.getText().toString();
            tvEnlarge.setText(text);
            if (tv.getBackground() instanceof ColorDrawable cd) {
                int colorCode = cd.getColor();
                tvEnlarge.setBackgroundColor(colorCode);
            }
            Drawable[] ds = tv.getCompoundDrawables();
            if (ds[0] != null){
                Drawable clone = Objects.requireNonNull(ds[0].getConstantState()).newDrawable();
                float scale = DisplayModel.getDeviceScaleFactor();
                clone.setBounds(0,0, (int)(30*scale),(int)(30*scale));
                tvEnlarge.setCompoundDrawables(clone,null,null,null);
            }
            int fgColor = tv.getTextColors().getDefaultColor();
            tvEnlarge.setTextColor(fgColor);
            tvEnlarge.setVisibility(View.VISIBLE);
        }
    }
}
